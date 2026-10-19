package com.fullmall.article.app;

import com.fullmall.platform.i18n.LocaleFallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 文章系统读取（公告 / 帮助文档 / 单页）。
 *
 * <p>多语言规则复用 {@link LocaleFallback}：请求语言 → **英文默认**，没有占位概念；
 * 只返回 PUBLISHED 的文章与译文，未发布内容不会泄露到前台。
 */
@Service
public class ArticleService {

    private final JdbcTemplate jdbcTemplate;

    public ArticleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record ArticleView(UUID id, String slug, String type, String locale, String title,
                              String summary, String content, String localeSource) {
    }

    public record ArticleSummary(UUID id, String slug, String type, String locale, String title,
                                 String summary, String localeSource) {
    }

    public List<ArticleSummary> listPublished(String type, String requestedLocale, String siteDefault,
                                              int limit) {
        if (limit <= 0 || limit > 200) {
            throw new IllegalArgumentException("查询条数必须在 1..200，实际=" + limit);
        }
        String locale = LocaleFallback.preferred(requestedLocale, siteDefault);
        return jdbcTemplate.query("""
                SELECT a.id, a.slug, a.type,
                       COALESCE(t.locale, d.locale) AS resolved_locale,
                       COALESCE(t.title, d.title) AS resolved_title,
                       COALESCE(t.summary, d.summary) AS resolved_summary,
                       CASE WHEN t.locale IS NULL THEN 'DEFAULT' ELSE 'LOCALIZED' END AS locale_source
                  FROM article a
                  LEFT JOIN article_translation t
                         ON t.article_id = a.id AND t.locale = ? AND t.status = 'PUBLISHED'
                  JOIN article_translation d
                    ON d.article_id = a.id AND d.locale = ? AND d.status = 'PUBLISHED'
                 WHERE a.status = 'PUBLISHED' AND a.type = ?
                 ORDER BY a.published_at DESC NULLS LAST
                 LIMIT ?
                """, (rs, rowNum) -> new ArticleSummary(
                rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("type"),
                rs.getString("resolved_locale"), rs.getString("resolved_title"),
                rs.getString("resolved_summary"), rs.getString("locale_source")),
                locale, LocaleFallback.FALLBACK_LOCALE, type, limit);
    }

    public ArticleView findBySlug(String slug, String requestedLocale, String siteDefault) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("slug 不能为空");
        }
        String locale = LocaleFallback.preferred(requestedLocale, siteDefault);
        return jdbcTemplate.query("""
                SELECT a.id, a.slug, a.type,
                       COALESCE(t.locale, d.locale) AS resolved_locale,
                       COALESCE(t.title, d.title) AS resolved_title,
                       COALESCE(t.summary, d.summary) AS resolved_summary,
                       COALESCE(t.content, d.content) AS resolved_content,
                       CASE WHEN t.locale IS NULL THEN 'DEFAULT' ELSE 'LOCALIZED' END AS locale_source
                  FROM article a
                  LEFT JOIN article_translation t
                         ON t.article_id = a.id AND t.locale = ? AND t.status = 'PUBLISHED'
                  JOIN article_translation d
                    ON d.article_id = a.id AND d.locale = ? AND d.status = 'PUBLISHED'
                 WHERE a.status = 'PUBLISHED' AND a.slug = ?
                """, (rs, rowNum) -> new ArticleView(
                rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("type"),
                rs.getString("resolved_locale"), rs.getString("resolved_title"),
                rs.getString("resolved_summary"), rs.getString("resolved_content"),
                rs.getString("locale_source")), locale, LocaleFallback.FALLBACK_LOCALE, slug)
                .stream().findFirst()
                .orElseThrow(() -> com.fullmall.platform.error.ApiException.notFound(
                        "ARTICLE_NOT_FOUND", "文章不存在或未发布: " + slug));
    }
}