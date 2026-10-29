package com.fullmall.catalog.app;

import com.fullmall.catalog.domain.Product;
import com.fullmall.catalog.infra.ProductTranslationRepository;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import com.fullmall.setting.app.SystemSettingService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * 译文审校与发布（I18N-003/004）。
 *
 * <p>门禁：
 * <ul>
 *   <li>只有 MACHINE/DRAFT 可以送审 → REVIEWED；</li>
 *   <li>只有 REVIEWED 可以 PUBLISHED（{@link TranslationStatus#requirePublishable}）；</li>
 *   <li>源文变更 → 已发布译文转 OUTDATED，前台只读 PUBLISHED，因此会回落到默认语言。</li>
 * </ul>
 *
 * <p>译文已经并入商品管理（设计审查 2026-10-28）：目标语言来自「系统设置 → 已启用语言」，
 * 不再由前端硬编码 en/ja，也不再把"没有译文"当成 404 让前端猜。
 */
@Service
public class ProductTranslationWorkflowService {

    private final ProductTranslationRepository repository;
    private final AuditEventService auditEvents;
    private final SystemSettingService settings;
    private final CatalogService catalog;
    private final TranslationService translator;
    private final RichTextTranslationService richText;
    private final JdbcTemplate jdbc;

    public ProductTranslationWorkflowService(ProductTranslationRepository repository,
                                            AuditEventService auditEvents,
                                            SystemSettingService settings,
                                            CatalogService catalog,
                                            TranslationService translator,
                                            RichTextTranslationService richText,
                                            JdbcTemplate jdbc) {
        this.repository = repository;
        this.auditEvents = auditEvents;
        this.settings = settings;
        this.catalog = catalog;
        this.translator = translator;
        this.richText = richText;
        this.jdbc = jdbc;
    }

    /** 录入译文草稿（运营手填），需再审核发布。 */
    @Transactional
    public ProductTranslationRepository.Row saveDraft(UUID productId, String locale,
                                                      String title, String description, String actor) {
        return saveDraft(productId, locale, title, description, null, actor);
    }

    /** 录入译文草稿（含本地化商品图），需再审核发布。 */
    @Transactional
    public ProductTranslationRepository.Row saveDraft(UUID productId, String locale,
                                                      String title, String description,
                                                      String imageUrl, String actor) {
        requireActor(actor);
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("TRANSLATION_TITLE_REQUIRED", "译文标题不能为空");
        }
        requireTargetLocale(locale);
        Product source = catalog.getProduct(productId, settings.defaultLocale());
        String hash = translator.sourceHash(source.title(), source.description());
        repository.upsert(productId, locale, title, description, normalizeImageUrl(imageUrl), null, hash);
        auditEvents.record(new AuditEventService.AuditEvent(
                "TRANSLATION_DRAFT_SAVED", actor, "product_translation",
                productId + ":" + locale, "title=" + title, null));
        return repository.find(productId, locale).orElseThrow();
    }

    /**
     * AI 翻译：用默认语言的标题/描述生成目标语言草稿（先查翻译记忆，命中不重复计费）。
     *
     * <p>只写入 DRAFT，仍然需要人工审核 → 发布；机器译文直接上线是运营事故的常见来源。
     */
    @Transactional
    public ProductTranslationRepository.Row autoTranslate(UUID productId, String locale, String actor) {
        requireActor(actor);
        requireTargetLocale(locale);
        Product source = catalog.getProduct(productId, settings.defaultLocale());
        if (source.title() == null || source.title().isBlank()) {
            throw ApiException.badRequest("TRANSLATION_SOURCE_TITLE_REQUIRED",
                    "源语言标题为空，无法翻译：" + productId);
        }
        int glossaryVersion = translator.currentGlossaryVersion();
        TranslationService.Translation title = translator.translate(source.title(), locale, glossaryVersion);
        String description = null;
        if (source.description() != null && !source.description().isBlank()) {
            description = translator.translate(source.description(), locale, glossaryVersion).text();
        }
        String hash = translator.sourceHash(source.title(), source.description());
        repository.upsert(productId, locale, title.text(), description, null, null, hash);
        auditEvents.record(new AuditEventService.AuditEvent(
                "TRANSLATION_AUTO_TRANSLATED", actor, "product_translation",
                productId + ":" + locale, "provider=" + title.source(), null));
        return repository.find(productId, locale).orElseThrow();
    }

    /**
     * 只翻译"富文本详情"，不动已存在的标题/描述（避免把已审核的标题打回草稿）。
     *
     * <p>没有草稿时先建一条：标题用源标题的机翻，描述/图片留空，详情写入译文 HTML。
     */
    @Transactional
    public RichTextTranslationService.Result autoTranslateDetail(UUID productId, String locale,
                                                                String actor) {
        requireActor(actor);
        requireTargetLocale(locale);
        Product source = catalog.getProduct(productId, settings.defaultLocale());
        String sourceDetail = detailHtmlOf(productId);
        if (sourceDetail == null || sourceDetail.isBlank()) {
            throw ApiException.badRequest("PRODUCT_DETAIL_EMPTY",
                    "该商品还没有富文本详情，先在商品编辑页补充详情再翻译");
        }
        int glossaryVersion = translator.currentGlossaryVersion();
        RichTextTranslationService.Result translated =
                richText.translateHtml(sourceDetail, locale, glossaryVersion);

        var existing = repository.find(productId, locale);
        String title = existing.map(ProductTranslationRepository.Row::title)
                .orElseGet(() -> translator.translate(source.title(), locale, glossaryVersion).text());
        String hash = translator.sourceHash(source.title(), source.description());
        if (existing.isEmpty()) {
            repository.upsert(productId, locale, title, null, null, translated.html(), hash);
        } else {
            repository.updateDetailHtml(productId, locale, translated.html());
        }
        auditEvents.record(new AuditEventService.AuditEvent(
                "TRANSLATION_DETAIL_TRANSLATED", actor, "product_translation",
                productId + ":" + locale,
                "segments=" + translated.segments() + ", cached=" + translated.cached(), null));
        return translated;
    }

    private String detailHtmlOf(UUID productId) {
        var rows = jdbc.query("SELECT detail_html FROM product WHERE id = ?",
                (rs, rowNum) -> rs.getString("detail_html"), productId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public ProductTranslationRepository.Row review(UUID productId, String locale, String actor) {
        requireActor(actor);
        var row = require(productId, locale);
        if (!row.status().canTransitionTo(TranslationStatus.REVIEWED)) {
            throw ApiException.conflict("TRANSLATION_NOT_REVIEWABLE",
                    "当前状态不可送审: " + row.status() + "（product=" + productId + ", locale=" + locale + "）");
        }
        repository.updateStatus(productId, locale, TranslationStatus.REVIEWED, actor, Instant.now());
        auditEvents.record(new AuditEventService.AuditEvent(
                "TRANSLATION_REVIEWED", actor, "product_translation", productId + ":" + locale,
                "from=" + row.status(), null));
        return repository.find(productId, locale).orElseThrow();
    }

    /**
     * 发布译文（只有 REVIEWED 可发布，见 {@link TranslationStatus#requirePublishable}）。
     *
     * <p>发布是前台可见的唯一开关：此前草稿只有后台能看，PUBLISHED 之后前台才会按该语言展示。
     * {@code sensitive} 标记法律/支付类文案，未审校时门禁更严；状态不允许时抛 409，
     * 不静默跳过，否则运营会以为已上线。操作人写入 reviewed_* 字段，便于追责。
     */
    @Transactional
    public ProductTranslationRepository.Row publish(UUID productId, String locale, boolean sensitive,
                                                    String actor) {
        requireActor(actor);
        var row = require(productId, locale);
        row.status().requirePublishable(sensitive);
        repository.updateStatus(productId, locale, TranslationStatus.PUBLISHED, actor, Instant.now());
        auditEvents.record(new AuditEventService.AuditEvent(
                "TRANSLATION_PUBLISHED", actor, "product_translation", productId + ":" + locale,
                "sensitive=" + sensitive, null));
        return repository.find(productId, locale).orElseThrow();
    }

    /**
     * 源文变更时调用：把已发布译文标记 OUTDATED，并返回是否发生了过期。
     */
    @Transactional
    public boolean markOutdatedOnSourceChange(UUID productId, String locale, String newSourceHash,
                                              String actor) {
        requireActor(actor);
        boolean outdated = repository.markOutdatedIfSourceChanged(productId, locale, newSourceHash);
        if (outdated) {
            auditEvents.record(new AuditEventService.AuditEvent(
                    "TRANSLATION_OUTDATED", actor, "product_translation", productId + ":" + locale,
                    "sourceChanged", null));
        }
        return outdated;
    }

    public Optional<ProductTranslationRepository.Row> published(UUID productId, String locale) {
        return repository.find(productId, locale)
                .filter(row -> row.status() == TranslationStatus.PUBLISHED);
    }

    // 目标语言必须已在「系统设置 → 已启用语言」里：未启用的语言即使手工写入，
    // 前台也没有入口能切过去，只会变成谁都看不到的僵尸数据。
    private void requireTargetLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            throw ApiException.badRequest("TRANSLATION_LOCALE_INVALID", "目标语言不能为空");
        }
        if (locale.equals(settings.defaultLocale())) {
            throw ApiException.badRequest("TRANSLATION_LOCALE_INVALID",
                    "目标语言必须是非 " + settings.defaultLocale() + " 的语言代码，如 en");
        }
        if (!settings.translationLocales().contains(locale)) {
            throw ApiException.badRequest("TRANSLATION_LOCALE_DISABLED",
                    "语言 " + locale + " 未在「系统设置 → 已启用语言」中启用，请先启用再录入译文");
        }
    }

    /**
     * 本地化图片地址：允许 http(s) 绝对地址或以 / 开头的站内路径。
     *
     * <p>不接受路径穿越（双点）与空白，避免脏数据进译文快照。
     */
    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        String value = imageUrl.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://") && !value.startsWith("/")) {
            throw ApiException.badRequest("TRANSLATION_IMAGE_URL_INVALID",
                    "译文图片必须是 http(s) 地址或以 / 开头的站内路径：" + value);
        }
        if (value.contains("..")) {
            throw ApiException.badRequest("TRANSLATION_IMAGE_URL_INVALID",
                    "译文图片路径不能包含双点：" + value);
        }
        return value;
    }

    private ProductTranslationRepository.Row require(UUID productId, String locale) {
        return repository.find(productId, locale)
                .orElseThrow(() -> ApiException.notFound("TRANSLATION_NOT_FOUND",
                        "译文不存在: product=" + productId + ", locale=" + locale));
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "审校/发布必须记录操作人");
        }
    }
}
