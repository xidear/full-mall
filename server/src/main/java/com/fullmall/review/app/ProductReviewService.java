package com.fullmall.review.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 商品评价。
 *
 * <p>约束（缺一不可）：
 * <ul>
 *   <li>订单必须属于该会员（`orders.member_id`）；</li>
 *   <li>订单状态必须是已支付之后（PAID / FULFILLED / REFUNDED 以外的完成态由调用方语义决定，
 *       这里只接受 PAID 与 FULFILLED）；</li>
 *   <li>SKU 必须在该订单里；</li>
 *   <li>同一订单同一 SKU 只能评价一次（DB 唯一约束 + 明确冲突错误）。</li>
 * </ul>
 */
@Service
public class ProductReviewService {

    private static final List<String> REVIEWABLE_STATUS = List.of("PAID", "FULFILLED");

    private final JdbcTemplate jdbcTemplate;

    public ProductReviewService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Review(UUID id, UUID skuId, int rating, String content, java.time.Instant createdAt) {
    }

    @Transactional
    public UUID create(UUID memberId, UUID orderId, UUID skuId, int rating, String content) {
        if (rating < 1 || rating > 5) {
            throw ApiException.badRequest("INVALID_RATING", "评分必须是 1..5");
        }
        if (content != null && content.length() > 1000) {
            throw ApiException.badRequest("CONTENT_TOO_LONG", "评价内容不能超过 1000 字");
        }

        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT status FROM orders WHERE id = ? AND member_id = ?",
                String.class, orderId, memberId);
        if (statuses.isEmpty()) {
            throw ApiException.notFound("ORDER_NOT_FOUND", "订单不存在或不属于当前会员: " + orderId);
        }
        if (!REVIEWABLE_STATUS.contains(statuses.get(0))) {
            throw ApiException.conflict("ORDER_NOT_REVIEWABLE",
                    "订单当前状态不可评价: " + statuses.get(0));
        }

        Integer lines = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM order_line WHERE order_id = ? AND sku_id = ?
                """, Integer.class, orderId, skuId);
        if (lines == null || lines == 0) {
            throw ApiException.badRequest("SKU_NOT_IN_ORDER", "该商品不在此订单中");
        }

        UUID id = UUID.randomUUID();
        try {
            jdbcTemplate.update("""
                    INSERT INTO product_review (id, sku_id, member_id, order_id, rating, content)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, skuId, memberId, orderId, rating,
                    content == null ? null : content.trim());
        } catch (org.springframework.dao.DuplicateKeyException error) {
            throw ApiException.conflict("REVIEW_ALREADY_EXISTS",
                    "该订单的这件商品已经评价过了");
        }
        return id;
    }

    public List<Review> listForSku(UUID skuId, int limit) {
        if (limit <= 0 || limit > 100) {
            throw ApiException.badRequest("INVALID_LIMIT", "查询条数必须在 1..100，实际=" + limit);
        }
        return jdbcTemplate.query("""
                SELECT id, sku_id, rating, content, created_at FROM product_review
                 WHERE sku_id = ? AND status = 'PUBLISHED'
                 ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> new Review(
                rs.getObject("id", UUID.class), rs.getObject("sku_id", UUID.class),
                rs.getInt("rating"), rs.getString("content"),
                rs.getTimestamp("created_at").toInstant()), skuId, limit);
    }

    /** 平均分（无评价返回 null，不返回 0 冒充满分）。 */
    public Double averageRating(UUID skuId) {
        return jdbcTemplate.queryForObject("""
                SELECT AVG(rating)::numeric(3,2) FROM product_review
                 WHERE sku_id = ? AND status = 'PUBLISHED'
                """, Double.class, skuId);
    }
}
