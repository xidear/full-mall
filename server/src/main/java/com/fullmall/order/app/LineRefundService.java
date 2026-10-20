package com.fullmall.order.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 按行退款：把退款落到具体商品行，为"部分退款按比例回退积分/佣金"提供分摊基础。
 *
 * <p>本轮只做**记录与校验**（退了哪行、几件、多少钱），不做资金回退——
 * 回退口径见 {@code docs/REFUND_POLICY.md}，需要产品确认后接入。
 *
 * <p>硬约束：
 * <ul>
 *   <li>行必须属于该订单；</li>
 *   <li>累计退款数量不得超过该行数量；</li>
 *   <li>累计退款金额不得超过该行金额（单价 × 数量）。</li>
 * </ul>
 */
@Service
public class LineRefundService {

    private final JdbcTemplate jdbcTemplate;

    public LineRefundService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record LineInfo(UUID id, UUID skuId, int quantity, long unitPrice, String currency) {
    }

    public record Refunded(UUID id, UUID orderLineId, int quantity, long amount) {
    }

    /** 该订单的可退行（含已退数量/金额，便于前端展示剩余可退）。 */
    public List<LineInfo> lines(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT id, sku_id, quantity, unit_price_amount, currency
                  FROM order_line WHERE order_id = ? ORDER BY id
                """, (rs, rowNum) -> new LineInfo(
                rs.getObject("id", UUID.class), rs.getObject("sku_id", UUID.class),
                rs.getInt("quantity"), rs.getLong("unit_price_amount"), rs.getString("currency")),
                orderId);
    }

    @Transactional
    public Refunded record(UUID orderId, UUID orderLineId, int quantity, long amount,
                           String currency, String reason) {
        if (quantity <= 0) {
            throw ApiException.badRequest("INVALID_REFUND_QUANTITY", "退款数量必须大于 0");
        }
        if (amount <= 0) {
            throw ApiException.badRequest("INVALID_REFUND_AMOUNT", "退款金额必须大于 0");
        }

        List<LineInfo> lines = jdbcTemplate.query("""
                SELECT id, sku_id, quantity, unit_price_amount, currency
                  FROM order_line WHERE id = ? AND order_id = ?
                """, (rs, rowNum) -> new LineInfo(
                rs.getObject("id", UUID.class), rs.getObject("sku_id", UUID.class),
                rs.getInt("quantity"), rs.getLong("unit_price_amount"), rs.getString("currency")),
                orderLineId, orderId);

        if (lines.isEmpty()) {
            throw ApiException.notFound("ORDER_LINE_NOT_FOUND", "该商品行不属于此订单: " + orderLineId);
        }
        LineInfo line = lines.get(0);
        if (!line.currency().equals(currency)) {
            throw ApiException.badRequest("CURRENCY_MISMATCH",
                    "退款币种与订单行不一致：" + currency + " vs " + line.currency());
        }

        int refundedQty = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0) FROM order_line_refund WHERE order_line_id = ?
                """, Integer.class, orderLineId);
        long refundedAmount = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_amount), 0) FROM order_line_refund WHERE order_line_id = ?
                """, Long.class, orderLineId);
        if (refundedQty + quantity > line.quantity()) {
            throw ApiException.conflict("REFUND_QUANTITY_EXCEEDS_LINE",
                    "累计退款数量 " + (refundedQty + quantity) + " 超过该行数量 " + line.quantity());
        }
        long lineTotal = Math.multiplyExact(line.unitPrice(), (long) line.quantity());
        if (refundedAmount + amount > lineTotal) {
            throw ApiException.conflict("REFUND_AMOUNT_EXCEEDS_LINE",
                    "累计退款金额 " + (refundedAmount + amount) + " 超过该行金额 " + lineTotal);
        }

        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO order_line_refund (id, order_id, order_line_id, quantity,
                                              amount_amount, currency, reason)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, orderId, orderLineId, quantity, amount, currency,
                reason == null ? null : reason.trim());
        return new Refunded(id, orderLineId, quantity, amount);
    }

    /**
     * 把一笔退款金额按行分摊到各商品（用于按比例回退积分/佣金）。
     *
     * <p>规则：按各行**剩余未退金额**占比分摊；**尾差分配给最后一个有剩余的行**，
     * 保证分摊之和**恰好等于**退款金额（不出现分币丢失或多出）。
     */
    public List<Refunded> allocate(UUID orderId, long totalRefundAmount, String currency) {
        if (totalRefundAmount <= 0) {
            throw ApiException.badRequest("INVALID_REFUND_AMOUNT", "退款金额必须大于 0");
        }
        List<LineInfo> lines = lines(orderId);
        if (lines.isEmpty()) {
            throw ApiException.notFound("ORDER_LINE_NOT_FOUND", "订单没有商品行: " + orderId);
        }

        List<Long> remaining = new ArrayList<>();
        long remainingTotal = 0;
        for (LineInfo line : lines) {
            long lineTotal = Math.multiplyExact(line.unitPrice(), (long) line.quantity());
            long refunded = jdbcTemplate.queryForObject("""
                    SELECT COALESCE(SUM(amount_amount), 0) FROM order_line_refund WHERE order_line_id = ?
                    """, Long.class, line.id());
            long left = lineTotal - refunded;
            remaining.add(Math.max(left, 0));
            remainingTotal += Math.max(left, 0);
        }
        if (remainingTotal <= 0) {
            throw ApiException.conflict("NO_REMAINING_AMOUNT", "订单已无可退金额");
        }
        if (totalRefundAmount > remainingTotal) {
            throw ApiException.conflict("REFUND_AMOUNT_EXCEEDS_REMAINING",
                    "退款金额 " + totalRefundAmount + " 超过剩余可退 " + remainingTotal);
        }

        List<Refunded> result = new ArrayList<>();
        long allocated = 0;
        int lastIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (remaining.get(i) <= 0) {
                continue;
            }
            lastIndex = i;
        }
        for (int i = 0; i < lines.size(); i++) {
            long left = remaining.get(i);
            if (left <= 0) {
                continue;
            }
            long share = (i == lastIndex)
                    ? totalRefundAmount - allocated
                    : Math.floorDiv(totalRefundAmount * left, remainingTotal);
            if (share <= 0) {
                continue;
            }
            allocated += share;
            result.add(new Refunded(null, lines.get(i).id(), 0, share));
        }
        if (allocated != totalRefundAmount) {
            throw new IllegalStateException(
                    "分摊后金额不一致：allocated=" + allocated + ", expected=" + totalRefundAmount);
        }
        return result;
    }
}
