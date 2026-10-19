package com.fullmall.catalog.infra;

import com.fullmall.catalog.app.TranslationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProductTranslationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductTranslationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Row(UUID productId, String locale, String title, String description,
                      TranslationStatus status, String sourceHash, String reviewedBy, Instant reviewedAt) {
    }

    public Optional<Row> find(UUID productId, String locale) {
        List<Row> rows = jdbcTemplate.query("""
                SELECT product_id, locale, title, description, status, source_hash, reviewed_by, reviewed_at
                  FROM product_translation WHERE product_id = ? AND locale = ?
                """, (rs, rowNum) -> new Row(
                rs.getObject("product_id", UUID.class),
                rs.getString("locale"),
                rs.getString("title"),
                rs.getString("description"),
                TranslationStatus.valueOf(rs.getString("status")),
                rs.getString("source_hash"),
                rs.getString("reviewed_by"),
                rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toInstant()),
                productId, locale);
        return rows.stream().findFirst();
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
}