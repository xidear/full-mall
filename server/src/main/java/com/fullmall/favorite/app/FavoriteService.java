package com.fullmall.favorite.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 商品收藏：幂等添加、幂等移除、按会员列最近收藏。 */
@Service
public class FavoriteService {

    private final JdbcTemplate jdbcTemplate;

    public FavoriteService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 收藏项：带上商品与价格，顾客端才能直接渲染（否则页面只有 skuId）。 */
    public record Favorite(UUID skuId, UUID productId, String title, long priceAmount,
                           String currency, Instant createdAt) {
    }

    /** @return true 表示新收藏，false 表示原本已收藏（幂等，不报错）。 */
    @Transactional
    public boolean add(UUID memberId, UUID skuId) {
        if (skuId == null) {
            throw ApiException.badRequest("SKU_REQUIRED", "必须指定商品");
        }
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sku WHERE id = ?", Integer.class, skuId);
        if (exists == null || exists == 0) {
            throw ApiException.notFound("SKU_NOT_FOUND", "商品不存在: " + skuId);
        }
        return jdbcTemplate.update("""
                INSERT INTO member_favorite (member_id, sku_id) VALUES (?, ?)
                ON CONFLICT (member_id, sku_id) DO NOTHING
                """, memberId, skuId) == 1;
    }

    /** @return true 表示确实删除了收藏。 */
    @Transactional
    public boolean remove(UUID memberId, UUID skuId) {
        return jdbcTemplate.update(
                "DELETE FROM member_favorite WHERE member_id = ? AND sku_id = ?",
                memberId, skuId) == 1;
    }

    public List<Favorite> list(UUID memberId, int limit) {
        if (limit <= 0 || limit > 100) {
            throw ApiException.badRequest("INVALID_LIMIT", "查询条数必须在 1..100，实际=" + limit);
        }
        return jdbcTemplate.query("""
                SELECT f.sku_id, f.created_at, s.product_id, s.sku_code,
                       s.price_amount, s.price_currency,
                       COALESCE(t.title, p.title) AS title
                  FROM member_favorite f
                  JOIN sku s ON s.id = f.sku_id
                  JOIN product p ON p.id = s.product_id
                  LEFT JOIN product_translation t
                         ON t.product_id = p.id AND t.locale = 'zh-CN' AND t.status = 'PUBLISHED'
                 WHERE f.member_id = ?
                 ORDER BY f.created_at DESC
                 LIMIT ?
                """, (rs, rowNum) -> new Favorite(
                rs.getObject("sku_id", UUID.class),
                rs.getObject("product_id", UUID.class),
                rs.getString("title"),
                rs.getLong("price_amount"),
                rs.getString("price_currency"),
                rs.getTimestamp("created_at").toInstant()),
                memberId, limit);
    }
}
