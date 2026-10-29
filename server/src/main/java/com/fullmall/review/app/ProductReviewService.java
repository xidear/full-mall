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
    private final com.fullmall.review.app.ReviewModerationPolicy moderationPolicy;

    public ProductReviewService(JdbcTemplate jdbcTemplate,
                                com.fullmall.review.app.ReviewModerationPolicy moderationPolicy) {
        this.moderationPolicy = moderationPolicy;
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Review(UUID id, UUID skuId, int rating, String content,
                         java.util.List<String> mediaUrls, java.time.Instant createdAt) {
    }

    /**
     * 发表评价。
     *
     * @param mediaUrls 顾客上传的图片/视频 URL（可为空）；只允许本站相对路径或 http(s) 绝对地址，
     *                  防止把 javascript:/data: 这类值塞进前台 &lt;img&gt;/&lt;video&gt; 造成 XSS
     */
    @Transactional
    public UUID create(UUID memberId, UUID orderId, UUID skuId, int rating, String content,
                       List<String> mediaUrls) {
        if (rating < 1 || rating > 5) {
            throw ApiException.badRequest("INVALID_RATING", "评分必须是 1..5");
        }
        if (content != null && content.length() > 1000) {
            throw ApiException.badRequest("CONTENT_TOO_LONG", "评价内容不能超过 1000 字");
        }
        List<String> media = normalizeMediaUrls(mediaUrls);

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
            var moderation = moderationPolicy.decide(content);
            if (moderation.outcome() == com.fullmall.review.app.ReviewModerationPolicy.Outcome.REJECT) {
                throw ApiException.badRequest("REVIEW_REJECTED",
                        "评价包含违规内容，请修改后重试（" + moderation.reason() + "）");
            }
            String status = moderation.outcome() == com.fullmall.review.app.ReviewModerationPolicy.Outcome.PUBLISH
                    ? "PUBLISHED" : "PENDING";
            jdbcTemplate.update("""
                    INSERT INTO product_review
                        (id, sku_id, member_id, order_id, rating, content, status, media_urls)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, skuId, memberId, orderId, rating,
                    content == null ? null : content.trim(), status,
                    media.toArray(new String[0]));
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
                SELECT id, sku_id, rating, content, media_urls, created_at FROM product_review
                 WHERE sku_id = ? AND status = 'PUBLISHED'
                 ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> new Review(
                rs.getObject("id", UUID.class), rs.getObject("sku_id", UUID.class),
                rs.getInt("rating"), rs.getString("content"),
                mediaUrlsOf(rs.getArray("media_urls")),
                rs.getTimestamp("created_at").toInstant()), skuId, limit);
    }

    /** 后台评价列表（含已隐藏），用于审核。 */
    /** Keyset 分页：按 (created_at, id) 倒序；与 offset 版本并存。 */
    public java.util.Map<String, Object> listForAdminKeyset(String status, String cursorCreatedAt,
                                                            UUID cursorId, int size) {
        var cursor = com.fullmall.platform.paging.KeysetPage.Cursor.parse(cursorCreatedAt, cursorId);
        int safeSize = com.fullmall.platform.paging.KeysetPage.normalizeSize(size);
        String filter = normalStatus(status);
        var items = jdbcTemplate.query("""
                SELECT r.id, r.sku_id, r.order_id, r.member_id, r.rating, r.content, r.status, r.created_at
                  FROM product_review r
                 WHERE (?::text IS NULL OR r.status = ?::text)
                   AND (?::timestamptz IS NULL
                        OR (r.created_at, r.id) < (?::timestamptz, ?::uuid))
                 ORDER BY r.created_at DESC, r.id DESC
                 LIMIT ?
                """, (rs, rowNum) -> new AdminReview(
                rs.getObject("id", UUID.class), rs.getObject("sku_id", UUID.class),
                rs.getObject("order_id", UUID.class), rs.getObject("member_id", UUID.class),
                rs.getInt("rating"), rs.getString("content"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()), filter, filter,
                cursor.createdAt() == null ? null : java.sql.Timestamp.from(cursor.createdAt()),
                cursor.createdAt() == null ? null : java.sql.Timestamp.from(cursor.createdAt()),
                cursor.id(), safeSize);
        return com.fullmall.platform.paging.KeysetPage.toResponse(
                items, safeSize, AdminReview::createdAt, AdminReview::id);
    }

    public java.util.List<AdminReview> listForAdmin(String status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * safeSize;
        String filter = normalStatus(status);
        return jdbcTemplate.query("""
                SELECT r.id, r.sku_id, r.order_id, r.member_id, r.rating, r.content, r.status, r.created_at
                  FROM product_review r
                 WHERE (?::text IS NULL OR r.status = ?::text)
                 ORDER BY r.created_at DESC
                 LIMIT ? OFFSET ?
                """, (rs, rowNum) -> new AdminReview(
                rs.getObject("id", UUID.class), rs.getObject("sku_id", UUID.class),
                rs.getObject("order_id", UUID.class), rs.getObject("member_id", UUID.class),
                rs.getInt("rating"), rs.getString("content"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()), filter, filter, safeSize, offset);
    }

    public long countForAdmin(String status) {
        String filter = normalStatus(status);
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM product_review WHERE (?::text IS NULL OR status = ?::text)
                """, Long.class, filter, filter);
        return count == null ? 0 : count;
    }

    /**
     * 隐藏/恢复评价。
     *
     * <p>只允许 PUBLISHED / HIDDEN 之间切换：删除评价会破坏顾客信任，隐藏可逆且可审计。
     */
    public void moderate(UUID reviewId, String status) {
        String target = normalStatus(status);
        if (target == null) {
            throw ApiException.badRequest("INVALID_REVIEW_STATUS", "评价状态只能是 PUBLISHED 或 HIDDEN");
        }
        int updated = jdbcTemplate.update(
                "UPDATE product_review SET status = ? WHERE id = ?", target, reviewId);
        if (updated != 1) {
            throw ApiException.notFound("REVIEW_NOT_FOUND", "评价不存在: " + reviewId);
        }
    }

    /** 评价媒体：条数受 ValidationRules.REVIEW_MEDIA_MAX 约束，单条长度防超长 URL 撑爆页面。 */
    private List<String> normalizeMediaUrls(List<String> mediaUrls) {
        if (mediaUrls == null || mediaUrls.isEmpty()) {
            return List.of();
        }
        if (mediaUrls.size() > com.fullmall.platform.validation.ValidationRules.REVIEW_MEDIA_MAX) {
            throw ApiException.badRequest("TOO_MANY_REVIEW_MEDIA", "评价最多附带 "
                    + com.fullmall.platform.validation.ValidationRules.REVIEW_MEDIA_MAX + " 个图片或视频，实际="
                    + mediaUrls.size());
        }
        List<String> normalized = new java.util.ArrayList<>(mediaUrls.size());
        for (String raw : mediaUrls) {
            if (raw == null || raw.isBlank()) {
                throw ApiException.badRequest("INVALID_REVIEW_MEDIA_URL", "评价媒体地址不能为空");
            }
            String url = raw.trim();
            if (url.length() > 512
                    || !(url.startsWith("/") || url.startsWith("https://") || url.startsWith("http://"))) {
                throw ApiException.badRequest("INVALID_REVIEW_MEDIA_URL",
                        "评价媒体必须是站内路径或以 http(s):// 开头的地址，实际=" + url);
            }
            normalized.add(url);
        }
        return List.copyOf(normalized);
    }

    private static List<String> mediaUrlsOf(java.sql.Array array) {
        if (array == null) {
            return List.of();
        }
        try {
            String[] values = (String[]) array.getArray();
            return values == null ? List.of() : java.util.List.of(values);
        } catch (java.sql.SQLException error) {
            throw new IllegalStateException("读取评价媒体失败", error);
        }
    }

    private String normalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.equals("PUBLISHED") && !normalized.equals("HIDDEN")) {
            throw ApiException.badRequest("INVALID_REVIEW_STATUS",
                    "评价状态只能是 PUBLISHED 或 HIDDEN，实际=" + status);
        }
        return normalized;
    }

    public record AdminReview(UUID id, UUID skuId, UUID orderId, UUID memberId,
                              int rating, String content, String status,
                              java.time.Instant createdAt) {
    }

    /** 平均分（无评价返回 null，不返回 0 冒充满分）。 */
    public Double averageRating(UUID skuId) {
        return jdbcTemplate.queryForObject("""
                SELECT AVG(rating)::numeric(3,2) FROM product_review
                 WHERE sku_id = ? AND status = 'PUBLISHED'
                """, Double.class, skuId);
    }
}
