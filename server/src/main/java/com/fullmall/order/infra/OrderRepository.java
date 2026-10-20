package com.fullmall.order.infra;

import com.fullmall.order.domain.FulfillmentMode;
import com.fullmall.order.domain.Order;
import com.fullmall.order.domain.OrderLine;
import com.fullmall.order.domain.OrderStatus;
import com.fullmall.platform.domain.FulfillmentType;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertOrder(Order order) {
        jdbcTemplate.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, contact_email,
                                    address_json, fulfillment_mode, store_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                """,
                order.id(),
                order.orderNo(),
                order.status().name(),
                order.currency(),
                order.subtotal().amount(),
                order.shippingFee().amount(),
                order.discount().amount(),
                order.total().amount(),
                order.contactName(),
                order.contactPhone(),
                order.contactEmail(),
                order.addressJson(),
                order.fulfillmentMode().name(),
                order.storeId(),
                java.sql.Timestamp.from(order.createdAt()));
    }

    public void insertOrderLine(UUID orderId, OrderLine line) {
        jdbcTemplate.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot,
                                        quantity, unit_price_amount, currency, fulfillment_type,
                                        promotion_id, line_type, snapshot)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                """,
                line.id(),
                orderId,
                line.skuId(),
                line.title(),
                line.skuCode(),
                line.quantity(),
                line.unitPrice().amount(),
                line.unitPrice().currency(),
                line.fulfillmentType().name(),
                line.promotionId(),
                line.lineType(),
                line.snapshot());
    }

    public record OrderSummary(UUID id, String orderNo, String status, long totalAmount,
                               String currency, java.time.Instant createdAt) {
    }

    public List<OrderSummary> findByMemberId(UUID memberId, int limit) {
        return jdbcTemplate.query("""
                SELECT id, order_no, status, total_amount, currency, created_at
                  FROM orders WHERE member_id = ?
                 ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> new OrderSummary(
                rs.getObject("id", UUID.class), rs.getString("order_no"), rs.getString("status"),
                rs.getLong("total_amount"), rs.getString("currency"),
                rs.getTimestamp("created_at").toInstant()), memberId, limit);
    }

    /** 记录下单来源与操作员。 */
    public void updatePlacement(UUID orderId, String placedBy, String operator) {
        int updated = jdbcTemplate.update("""
                UPDATE orders SET placed_by = ?, placed_by_operator = ? WHERE id = ?
                """, placedBy, operator, orderId);
        if (updated != 1) {
            throw new IllegalStateException("下单来源更新失败：" + orderId);
        }
    }

    /** 按手机号查会员（手机号唯一时）；用于下单过程中取券归属。 */
    public UUID findMemberIdByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        List<UUID> rows = jdbcTemplate.queryForList("""
                SELECT m.id FROM member m
                 WHERE m.phone = ?
                   AND (SELECT COUNT(*) FROM member m2 WHERE m2.phone = m.phone) = 1
                """, UUID.class, phone.trim());
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 下单后按手机号把订单绑定到会员（手机号唯一时）。 */
    public void attachMemberByPhone(UUID orderId, String phone) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE orders o SET member_id = m.id
                  FROM member m
                 WHERE o.id = ? AND o.member_id IS NULL AND m.phone = ?
                   AND (SELECT COUNT(*) FROM member m2 WHERE m2.phone = m.phone) = 1
                """, orderId, phone.trim());
    }

    /** @return 订单归属的会员 ID；游客订单返回 null。 */
    /** 写入游客订单令牌哈希（明文只在响应里返回一次）。 */
    public void storeAccessTokenHash(UUID orderId, String hash) {
        jdbcTemplate.update("UPDATE orders SET access_token_hash = ? WHERE id = ?", hash, orderId);
    }

    public UUID memberIdOf(UUID orderId) {
        List<UUID> rows = jdbcTemplate.queryForList(
                "SELECT member_id FROM orders WHERE id = ?", UUID.class, orderId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Order findOrder(UUID orderId) {
        List<Order> orders = jdbcTemplate.query("""
                SELECT id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                       discount_amount, total_amount, contact_name, contact_phone, contact_email,
                       address_json, fulfillment_mode, store_id, created_at
                FROM orders
                WHERE id = ?
                """, (rs, rowNum) -> mapOrderBase(rs), orderId);
        return orders.stream().findFirst().map(this::withLines).orElse(null);
    }

    public List<OrderLine> findOrderLines(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT id, sku_id, title_snapshot, sku_code_snapshot, quantity, unit_price_amount,
                       currency, fulfillment_type, promotion_id, line_type, snapshot::text AS snapshot
                FROM order_line
                WHERE order_id = ?
                ORDER BY title_snapshot
                """, (rs, rowNum) -> new OrderLine(
                rs.getObject("id", UUID.class),
                rs.getObject("sku_id", UUID.class),
                rs.getString("title_snapshot"),
                rs.getString("sku_code_snapshot"),
                rs.getInt("quantity"),
                new Money(rs.getLong("unit_price_amount"), rs.getString("currency")),
                FulfillmentType.valueOf(rs.getString("fulfillment_type")),
                rs.getObject("promotion_id", UUID.class),
                rs.getString("line_type"),
                rs.getString("snapshot")), orderId);
    }

    public boolean markPaidIfPending(UUID orderId) {
        return jdbcTemplate.update("""
                UPDATE orders SET status = 'PAID' WHERE id = ? AND status = 'PENDING_PAYMENT'
                """, orderId) == 1;
    }

    public boolean cancelIfPending(UUID orderId) {
        return jdbcTemplate.update("""
                UPDATE orders SET status = 'CANCELLED' WHERE id = ? AND status = 'PENDING_PAYMENT'
                """, orderId) == 1;
    }

    public List<UUID> findStalePendingOrderIds(java.time.Instant cutoff, int limit) {
        return jdbcTemplate.query("""
                SELECT id FROM orders
                WHERE status = 'PENDING_PAYMENT' AND created_at < ?
                ORDER BY created_at
                LIMIT ?
                """, (rs, rowNum) -> rs.getObject("id", UUID.class),
                java.sql.Timestamp.from(cutoff), limit);
    }

    public void updateStatus(UUID orderId, OrderStatus status) {
        // 完成时间只写一次：佣金"售后关闭"策略以它为起算点
        int updated = jdbcTemplate.update("""
                UPDATE orders
                   SET status = ?,
                       fulfilled_at = CASE WHEN ? = 'FULFILLED'
                                          THEN COALESCE(fulfilled_at, now())
                                          ELSE fulfilled_at END
                 WHERE id = ?
                """, status.name(), status.name(), orderId);
        if (updated != 1) {
            throw new IllegalStateException("Order not found while updating status: " + orderId);
        }
    }

    /**
     * 排序白名单：**只允许已知列与方向**，绝不拼接用户输入（防注入）。
     *
     * <p>未知字段回落到默认 `created_at DESC`，不报错——但会记日志由调用方观察。
     */
    private static String safeOrderBy(String sortKey, String direction) {
        String key = sortKey == null ? "" : sortKey.trim();
        String column = switch (key) {
            case "createdAt" -> "created_at";
            case "totalAmount" -> "total_amount";
            case "status" -> "status";
            default -> "created_at";
        };
        String dir = "asc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "ASC" : "DESC";
        return column + " " + dir;
    }

    public List<com.fullmall.order.api.OrderSummary> findSummaries(String status, int limit, int offset) {
        return findSummaries(status, limit, offset, "createdAt", "desc");
    }

    public List<com.fullmall.order.api.OrderSummary> findSummaries(String status, int limit, int offset,
                                                                   String sortKey, String direction) {
        String normalized = status == null ? "" : status.trim();
        return jdbcTemplate.query("""
                SELECT id, order_no, status, currency, total_amount, fulfillment_mode, contact_name, created_at
                FROM orders
                WHERE (? = '' OR status = ?)
                ORDER BY
                """ + safeOrderBy(sortKey, direction) + """
                 LIMIT ? OFFSET ?
                """, (rs, rowNum) -> new com.fullmall.order.api.OrderSummary(
                rs.getObject("id", UUID.class),
                rs.getString("order_no"),
                rs.getString("status"),
                new Money(rs.getLong("total_amount"), rs.getString("currency")),
                rs.getString("fulfillment_mode"),
                rs.getString("contact_name"),
                rs.getTimestamp("created_at").toInstant()), normalized, normalized, limit, offset);
    }

    public long countSummaries(String status) {
        String normalized = status == null ? "" : status.trim();
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM orders WHERE (? = '' OR status = ?)
                """, Long.class, normalized, normalized);
        if (count == null) {
            throw new IllegalStateException("Order count query returned null");
        }
        return count;
    }

    private Order mapOrderBase(ResultSet rs) throws SQLException {
        return new Order(
                rs.getObject("id", UUID.class),
                rs.getString("order_no"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getString("currency"),
                new Money(rs.getLong("subtotal_amount"), rs.getString("currency")),
                new Money(rs.getLong("shipping_fee_amount"), rs.getString("currency")),
                new Money(rs.getLong("discount_amount"), rs.getString("currency")),
                new Money(rs.getLong("total_amount"), rs.getString("currency")),
                rs.getString("contact_name"),
                rs.getString("contact_phone"),
                rs.getString("contact_email"),
                rs.getString("address_json"),
                FulfillmentMode.valueOf(rs.getString("fulfillment_mode")),
                rs.getObject("store_id", UUID.class),
                rs.getTimestamp("created_at").toInstant(),
                List.of());
    }

    private Order withLines(Order order) {
        return new Order(
                order.id(), order.orderNo(), order.status(), order.currency(),
                order.subtotal(), order.shippingFee(), order.discount(), order.total(),
                order.contactName(), order.contactPhone(), order.contactEmail(), order.addressJson(),
                order.fulfillmentMode(), order.storeId(), order.createdAt(), findOrderLines(order.id()));
    }
}