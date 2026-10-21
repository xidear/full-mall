package com.fullmall.supplier.infra;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class SupplierSettlementRepository {

    private final JdbcTemplate jdbcTemplate;

    public SupplierSettlementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record PeriodAggregate(String currency, int orderCount, long grossAmount) {
    }

    public record SettlementRow(UUID id, UUID supplierId, Instant periodStart, Instant periodEnd,
                                int orderCount, long grossAmount, long refundAmount,
                                long commissionAmount, long payableAmount, String currency,
                                String status, Instant createdAt) {
    }

    /**
     * 按币种聚合供应商代发订单的已支付金额。
     * 多币种会返回多行，由服务层显式拒绝（不做汇率换算的臆造）。
     */
    public List<PeriodAggregate> aggregate(UUID supplierId, Instant from, Instant to) {
        return jdbcTemplate.query("""
                SELECT o.currency,
                       COUNT(DISTINCT o.id) AS order_count,
                       COALESCE(SUM(p.amount_amount), 0) AS gross_amount
                  FROM orders o
                  JOIN fulfillment_order f ON f.order_id = o.id
                   AND f.source_id = ? AND f.type = 'SUPPLIER_DROPSHIP'
                  JOIN payment p ON p.order_id = o.id AND p.status = 'PAID'
                 WHERE o.created_at >= ? AND o.created_at < ?
                 GROUP BY o.currency
                 ORDER BY o.currency
                """, (rs, rowNum) -> new PeriodAggregate(
                rs.getString("currency"),
                rs.getInt("order_count"),
                rs.getLong("gross_amount")),
                supplierId, Timestamp.from(from), Timestamp.from(to));
    }

    public void insert(UUID id, UUID supplierId, Instant periodStart, Instant periodEnd,
                       int orderCount, long gross, long refund, long commission, long payable,
                       String currency, String status) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO supplier_settlement (id, supplier_id, period_start, period_end, order_count,
                                                 gross_amount, refund_amount, commission_amount,
                                                 payable_amount, currency, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, supplierId, Timestamp.from(periodStart), Timestamp.from(periodEnd),
                orderCount, gross, refund, commission, payable, currency, status);
        if (inserted != 1) {
            throw new IllegalStateException("结算单写入失败，影响行数=" + inserted);
        }
    }

    public List<SettlementRow> findBySupplier(UUID supplierId, int limit) {
        return jdbcTemplate.query("""
                SELECT id, supplier_id, period_start, period_end, order_count, gross_amount,
                       refund_amount, commission_amount, payable_amount, currency, status, created_at
                  FROM supplier_settlement
                 WHERE supplier_id = ?
                 ORDER BY period_end DESC LIMIT ?
                """, (rs, rowNum) -> new SettlementRow(
                rs.getObject("id", UUID.class),
                rs.getObject("supplier_id", UUID.class),
                rs.getTimestamp("period_start").toInstant(),
                rs.getTimestamp("period_end").toInstant(),
                rs.getInt("order_count"),
                rs.getLong("gross_amount"),
                rs.getLong("refund_amount"),
                rs.getLong("commission_amount"),
                rs.getLong("payable_amount"),
                rs.getString("currency"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()), supplierId, limit);
    }
}