package com.fullmall.catalog.app;

import com.fullmall.catalog.infra.ProductTranslationRepository;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
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
 */
@Service
public class ProductTranslationWorkflowService {

    private final ProductTranslationRepository repository;
    private final AuditEventService auditEvents;

    public ProductTranslationWorkflowService(ProductTranslationRepository repository,
                                             AuditEventService auditEvents) {
        this.repository = repository;
        this.auditEvents = auditEvents;
    }

    /** 录入译文草稿（运营手填/机翻结果），需再审核发布。 */
    @Transactional
    public ProductTranslationRepository.Row saveDraft(UUID productId, String locale,
                                                      String title, String description, String actor) {
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("TRANSLATION_TITLE_REQUIRED", "译文标题不能为空");
        }
        if (locale == null || locale.isBlank() || "zh-CN".equalsIgnoreCase(locale)) {
            throw ApiException.badRequest("TRANSLATION_LOCALE_INVALID",
                    "目标语言必须是非 zh-CN 的语言代码，如 en");
        }
        var source = repository.find(productId, "zh-CN");
        String hash = source.map(ProductTranslationRepository.Row::sourceHash).orElse(null);
        repository.upsert(productId, locale, title, description, hash);
        auditEvents.record(new AuditEventService.AuditEvent(
                "TRANSLATION_DRAFT_SAVED", actor, "product_translation",
                productId + ":" + locale, "title=" + title, null));
        return repository.find(productId, locale).orElseThrow();
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