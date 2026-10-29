package com.fullmall.catalog.infra;

import com.fullmall.catalog.app.TranslationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 译文表（{@code product_translation}）的读写（infra 层）。
 *
 * <p>状态字段的语义由 {@code TranslationStatus} 定义，这里只负责落库；「空集合 ≠ 不存在」在这里
 * 体现为：{@code find} 返回 {@code Optional.empty()} 表示该语言没有译文（正常状态），
 * {@code listByProduct} 返回空列表表示一条都没录入，二者都不抛异常——由上层决定是 404 还是提示。
 */
@Repository
public class ProductTranslationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductTranslationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Row(UUID productId, String locale, String title, String description,
                      String imageUrl, String detailHtml, TranslationStatus status, String sourceHash,
                      String reviewedBy, Instant reviewedAt) {
    }

    public Optional<Row> find(UUID productId, String locale) {
        List<Row> rows = jdbcTemplate.query("""
                SELECT product_id, locale, title, description, image_url, detail_html, status,
                       source_hash, reviewed_by, reviewed_at
                  FROM product_translation WHERE product_id = ? AND locale = ?
                """, (rs, rowNum) -> mapRow(rs), productId, locale);
        return rows.stream().findFirst();
    }

    /**
     * 商品的全部译文（后台详情页一次性取回）。
     *
     * <p>用"列表 + 缺失即空"替代"逐语言 404"：404 表示"资源不存在"，
     * 被前端当成正常状态吞掉后，切换语言时会留下上一个语言的内容（参见设计审查报告）。
     */
    public List<Row> listByProduct(UUID productId) {
        return jdbcTemplate.query("""
                SELECT product_id, locale, title, description, image_url, detail_html, status,
                       source_hash, reviewed_by, reviewed_at
                  FROM product_translation WHERE product_id = ? ORDER BY locale
                """, (rs, rowNum) -> mapRow(rs), productId);
    }

    private Row mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Row(
                rs.getObject("product_id", UUID.class),
                rs.getString("locale"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("image_url"),
                rs.getString("detail_html"),
                TranslationStatus.valueOf(rs.getString("status")),
                rs.getString("source_hash"),
                rs.getString("reviewed_by"),
                rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toInstant());
    }

    public void updateStatus(UUID productId, String locale, TranslationStatus status,
                             String reviewedBy, Instant reviewedAt) {
        int updated = jdbcTemplate.update("""
                UPDATE product_translation
                   SET status = ?, reviewed_by = COALESCE(?, reviewed_by),
                       reviewed_at = COALESCE(?, reviewed_at), updated_at = now()
                 WHERE product_id = ? AND locale = ?
                """, status.name(), reviewedBy, reviewedAt == null ? null : Timestamp.from(reviewedAt),
                productId, locale);
        if (updated != 1) {
            throw new IllegalStateException(
                    "译文状态更新失败: product=" + productId + ", locale=" + locale + "，影响行数=" + updated);
        }
    }

    /**
     * 源文（中文）内容哈希变化 → 已有译文标记 OUTDATED。
     *
     * <p>WHERE 只命中 {@code PUBLISHED}：草稿/待审本来就没上线，不该被改成 OUTDATED 而丢掉审校进度；
     * 条件是单条原子 UPDATE，返回 true 才代表确实有译文被下线（前台随之回落源文）。
     *
     * @return true 表示确实标记了过期
     */
    public boolean markOutdatedIfSourceChanged(UUID productId, String locale, String newSourceHash) {
        int updated = jdbcTemplate.update("""
                UPDATE product_translation
                   SET status = 'OUTDATED', source_hash = ?, updated_at = now()
                 WHERE product_id = ? AND locale = ?
                   AND status = 'PUBLISHED'
                   AND (source_hash IS NULL OR source_hash <> ?)
                """, newSourceHash, productId, locale, newSourceHash);
        return updated > 0;
    }

    // 覆盖时会把 status 打回 DRAFT 并清空 reviewed_by/reviewed_at：内容变了就得重新审校。
    // 前台只读 PUBLISHED，所以覆盖已发布译文会立即让该语言回落到源文——这是有意的，
    // 避免“改了译文却还挂着旧审校人的背书”。
    /** 录入/覆盖译文（运营手填或机器翻译结果），状态置 DRAFT 待审核。 */
    public void upsert(UUID productId, String locale, String title, String description,
                       String imageUrl, String detailHtml, String sourceHash) {
        jdbcTemplate.update("""
                INSERT INTO product_translation (product_id, locale, title, description, image_url,
                                                 detail_html, status, source_hash, reviewed_by,
                                                 reviewed_at)
                VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?, NULL, NULL)
                ON CONFLICT (product_id, locale) DO UPDATE
                   SET title = EXCLUDED.title,
                       description = EXCLUDED.description,
                       image_url = EXCLUDED.image_url,
                       detail_html = EXCLUDED.detail_html,
                       status = 'DRAFT',
                       source_hash = EXCLUDED.source_hash,
                       reviewed_by = NULL,
                       reviewed_at = NULL
                """, productId, locale, title, description, imageUrl, detailHtml, sourceHash);
    }

    /** 只更新富文本译文（不动标题/描述，避免"只翻详情"时覆盖已审核的标题）。 */
    public void updateDetailHtml(UUID productId, String locale, String detailHtml) {
        int updated = jdbcTemplate.update("""
                UPDATE product_translation SET detail_html = ?, updated_at = now()
                 WHERE product_id = ? AND locale = ?
                """, detailHtml, productId, locale);
        if (updated != 1) {
            throw new IllegalStateException(
                    "详情译文更新失败: product=" + productId + ", locale=" + locale + "，影响行数=" + updated);
        }
    }
}