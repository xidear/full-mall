package com.fullmall.distribution.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 部分退货期间的佣金处理（DEFER 口径）。
 *
 * <p>规则（业务确认过）：
 * <ul>
 *   <li>顾客申请部分退货 → 该订单佣金**延长冻结**，冻结额度按退款占比计算，记在 `hold_amount`；</li>
 *   <li>售后关闭且"退货完成" → 按 hold 金额**回冲**（`reversed_amount` 累加）；</li>
 *   <li>售后关闭但不是退货（驳回/换货完成/撤销）→ 解除 hold，佣金原额保留。</li>
 * </ul>
 *
 * <p>与供应商售后（`supplier_return`）**无关**：那只影响供应商结算，不影响顾客佣金。
 */
@Service
public class CommissionHoldService {

    private final JdbcTemplate jdbcTemplate;

    public CommissionHoldService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 售后开单：冻结该订单佣金的对应比例。 */
    @Transactional
    public void hold(UUID orderId, UUID afterSaleId, long refundAmount, long orderTotal) {
        if (orderTotal <= 0) {
            throw ApiException.badRequest("INVALID_ORDER_TOTAL", "订单金额必须大于 0，实际=" + orderTotal);
        }
        if (refundAmount <= 0) {
            return;   // 换货等无金额售后不冻结
        }
        List<Row> commissions = jdbcTemplate.query("""
                SELECT id, commission_amount FROM distribution_commission
                 WHERE order_id = ? AND status IN ('FROZEN', 'AVAILABLE')
                 FOR UPDATE
                """, (rs, rowNum) -> new Row(rs.getObject("id", UUID.class),
                rs.getLong("commission_amount")), orderId);

        for (Row row : commissions) {
            // 按退款占比计算冻结额；四舍五入到分，至少 1 分（有退款就应有冻结）
            long holdAmount = Math.max(1L, com.fullmall.platform.money.Money.mulDiv(
                    row.amount(), refundAmount, orderTotal));
            holdAmount = Math.min(holdAmount, row.amount());
            jdbcTemplate.update("""
                    UPDATE distribution_commission
                       SET hold_after_sale_id = ?, hold_amount = ?
                     WHERE id = ?
                    """, afterSaleId, holdAmount, row.id());
        }
    }

    /**
     * 售后关闭。
     *
     * @param refunded true 表示结果是"退货完成/已退款" → 按 hold 回冲；false 表示撤销/驳回/换货完成 → 不扣
     */
    @Transactional
    public int close(UUID afterSaleId, boolean refunded) {
        List<Row> held = jdbcTemplate.query("""
                SELECT id, commission_amount, hold_amount, reversed_amount, consumed_amount
                  FROM distribution_commission WHERE hold_after_sale_id = ? FOR UPDATE
                """, (rs, rowNum) -> new Row(rs.getObject("id", UUID.class),
                rs.getLong("commission_amount"), rs.getLong("hold_amount"),
                rs.getLong("reversed_amount"), rs.getLong("consumed_amount")), afterSaleId);

        int affected = 0;
        for (Row row : held) {
            if (refunded) {
                long newReversed = Math.min(row.reversed() + row.hold(),
                        row.amount() - row.consumed());
                boolean fullyReversed = newReversed + row.consumed() >= row.amount();
                jdbcTemplate.update("""
                        UPDATE distribution_commission
                           SET reversed_amount = ?, hold_after_sale_id = NULL, hold_amount = 0,
                               status = CASE WHEN ? THEN 'REVERSED' ELSE status END,
                               reversed_at = CASE WHEN ? THEN now() ELSE reversed_at END,
                               reverse_reason = CASE WHEN ? THEN 'after-sale refund' ELSE reverse_reason END
                         WHERE id = ?
                        """, newReversed, fullyReversed, fullyReversed, fullyReversed, row.id());
                jdbcTemplate.update("""
                        INSERT INTO commission_ledger (id, commission_id, from_status, to_status,
                                                       amount_amount, currency, reason)
                        SELECT ?, c.id, c.status, CASE WHEN ? THEN 'REVERSED' ELSE c.status END,
                               ?, c.currency, ?
                          FROM distribution_commission c WHERE c.id = ?
                        """, UUID.randomUUID(), fullyReversed, row.hold(),
                        "after-sale " + afterSaleId + " refund", row.id());
            } else {
                jdbcTemplate.update("""
                        UPDATE distribution_commission
                           SET hold_after_sale_id = NULL, hold_amount = 0
                         WHERE id = ?
                        """, row.id());
            }
            affected++;
        }
        return affected;
    }

    private record Row(UUID id, long amount, long hold, long reversed, long consumed) {
        Row(UUID id, long amount) {
            this(id, amount, 0, 0, 0);
        }
    }
}
