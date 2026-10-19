package com.fullmall.article.app;

import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.i18n.LocaleFallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 文章后台：创建、写译文、审校、发布。
 *
 * <p>发布门禁（I18N-004）：
 * <ol>
 *   <li>必须有 **英文默认译文**（{@link LocaleFallback#requirePublishable}），否则拒绝发布；</li>
 *   <li>只有 `REVIEWED` 的译文会被置为 `PUBLISHED`，机翻草稿不会流到前台；</li>
 *   <li>每次状态变化写审计。</li>
 * </ol>
 */
@Service
public class ArticleAdminService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditEventService auditEvents;

    public ArticleAdminService(JdbcTemplate jdbcTemplate, AuditEventService auditEvents) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditEvents = auditEvents;
    }

    public record ArticleRow(UUID id, String slug, String type, String status) {
    }

    @Transactional
    public UUID create(String slug, String type, String actor) {
        requireActor(actor);
        if (slug == null || !slug.matches("^[a-z0-9][a-z0-9-]{1,127}$")) {
            throw ApiException.badRequest("INVALID_SLUG",
                    "slug 只能是小写字母/数字/连字符，长度 2..128: " + slug);
        }
        if (type == null || !List.of("ANNOUNCEMENT", "HELP", "PAGE").contains(type)) {
            throw ApiException.badRequest("INVALID_ARTICLE_TYPE", "未知文章类型: " + type);
        }
        UUID id = UUID.randomUUID();
        int inserted = jdbcTemplate.update("""
                INSERT INTO article (id, slug, type, status) VALUES (?, ?, ?, 'DRAFT')
                """, id, slug, type);
        if (inserted != 1) {
            throw new IllegalStateException("文章创建失败: " + slug);
        }
        audit(id, "ARTICLE_CREATED", actor, "type=" + type);
        return id;
    }

    @Transactional
    public void upsertTranslation(UUID articleId, String locale, String title, String summary,
                                  String content, String actor) {
        requireActor(actor);
        requireArticle(articleId);
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("TITLE_REQUIRED", "标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw ApiException.badRequest("CONTENT_REQUIRED", "正文不能为空");
        }
        int updated = jdbcTemplate.update("""
                INSERT INTO article_translation (article_id, locale, title, summary, content, status)
                VALUES (?, ?, ?, ?, ?, 'DRAFT')
                ON CONFLICT (article_id, locale) DO UPDATE
                   SET title = EXCLUDED.title, summary = EXCLUDED.summary,
                       content = EXCLUDED.content, status = 'DRAFT', updated_at = now()
                """, articleId, locale, title.trim(), summary, content);
        if (updated < 1) {
            throw new IllegalStateException("译文写入失败: article=" + articleId + ", locale=" + locale);
        }
        audit(articleId, "ARTICLE_TRANSLATION_SAVED", actor, "locale=" + locale);
    }

    @Transactional
    public void review(UUID articleId, String locale, String actor) {
        requireActor(actor);
        int updated = jdbcTemplate.update("""
                UPDATE article_translation SET status = 'REVIEWED', updated_at = now()
                 WHERE article_id = ? AND locale = ? AND status IN ('DRAFT', 'MACHINE')
                """, articleId, locale);
        if (updated != 1) {
            throw ApiException.conflict("TRANSLATION_NOT_REVIEWABLE",
                    "译文不存在或当前状态不可送审: article=" + articleId + ", locale=" + locale);
        }
        audit(articleId, "ARTICLE_TRANSLATION_REVIEWED", actor, "locale=" + locale);
    }

    /**
     * 发布：必须有英文默认译文；只发布已审校的译文。
     */
    @Transactional
    public void publish(UUID articleId, String actor) {
        requireActor(actor);
        requireArticle(articleId);

        Integer englishReviewed = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM article_translation
                 WHERE article_id = ? AND locale = ? AND status = 'REVIEWED'
                """, Integer.class, articleId, LocaleFallback.FALLBACK_LOCALE);
        if (englishReviewed == null) {
            throw new IllegalStateException("英文默认译文查询返回 null: " + articleId);
        }
        LocaleFallback.requirePublishable(englishReviewed > 0, "article:" + articleId);

        jdbcTemplate.update("""
                UPDATE article_translation SET status = 'PUBLISHED', updated_at = now()
                 WHERE article_id = ? AND status = 'REVIEWED'
                """, articleId);
        int updated = jdbcTemplate.update("""
                UPDATE article SET status = 'PUBLISHED', published_at = now(), updated_at = now()
                 WHERE id = ?
                """, articleId);
        if (updated != 1) {
            throw new IllegalStateException("文章发布失败: " + articleId);
        }
        audit(articleId, "ARTICLE_PUBLISHED", actor, null);
    }

    private void requireArticle(UUID articleId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM article WHERE id = ?", Integer.class, articleId);
        if (count == null || count == 0) {
            throw ApiException.notFound("ARTICLE_NOT_FOUND", "文章不存在: " + articleId);
        }
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "该操作必须记录操作人");
        }
    }

    private void audit(UUID articleId, String type, String actor, String detail) {
        auditEvents.record(new AuditEventService.AuditEvent(
                type, actor, "article", articleId.toString(), detail, null));
    }
}