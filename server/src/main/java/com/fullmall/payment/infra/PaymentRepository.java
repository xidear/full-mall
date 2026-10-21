package com.fullmall.payment.infra;

import com.fullmall.payment.domain.Payment;
import com.fullmall.payment.domain.PaymentStatus;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class PaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Payment insert(Payment payment) {
        jdbcTemplate.update("""
                INSERT INTO payment (id, order_id, provider, status, amount_amount, amount_currency,
                                     provider_transaction_id, paid_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                payment.id(),
                payment.orderId(),
                payment.provider(),
                payment.status().name(),
                payment.amount().amount(),
                payment.amount().currency(),
                payment.providerTransactionId(),
                payment.paidAt() == null ? null : java.sql.Timestamp.from(payment.paidAt()),
                java.sql.Timestamp.from(payment.createdAt()));
        return payment;
    }

    /** 累计已退款额（含本次）。 */
    public long refundedAmount(UUID paymentId) {
        Long value = jdbcTemplate.queryForObject(
                "SELECT refunded_amount FROM payment WHERE id = ?", Long.class, paymentId);
        return value == null ? 0 : value;
    }

    /** 记录本次退款额（与支付状态更新同一事务）。 */
    public void addRefundedAmount(UUID paymentId, long amount) {
        int updated = jdbcTemplate.update(
                "UPDATE payment SET refunded_amount = refunded_amount + ? WHERE id = ?",
                amount, paymentId);
        if (updated != 1) {
            throw new IllegalStateException("退款额累计失败: " + paymentId);
        }
    }

    public void update(Payment payment) {
        int updated = jdbcTemplate.update("""
                UPDATE payment
                SET status = ?, provider_transaction_id = ?, paid_at = ?
                WHERE id = ?
                """,
                payment.status().name(),
                payment.providerTransactionId(),
                payment.paidAt() == null ? null : java.sql.Timestamp.from(payment.paidAt()),
                payment.id());
        if (updated != 1) {
            throw new IllegalStateException("Payment not found: " + payment.id());
        }
    }

    /** 该订单最近一次支付记录（退款按它记录的渠道原路退回）。 */
    public Payment findLatestByOrderId(UUID orderId) {
        List<Payment> payments = jdbcTemplate.query("""
                SELECT id, order_id, provider, status, amount_amount, amount_currency,
                       provider_transaction_id, paid_at, created_at
                  FROM payment WHERE order_id = ? ORDER BY created_at DESC LIMIT 1
                """, (rs, rowNum) -> map(rs), orderId);
        return payments.stream().findFirst().orElse(null);
    }

    public Payment findByOrderIdAndProvider(UUID orderId, String provider) {
        List<Payment> payments = jdbcTemplate.query("""
                SELECT id, order_id, provider, status, amount_amount, amount_currency,
                       provider_transaction_id, paid_at, created_at
                FROM payment
                WHERE order_id = ? AND provider = ?
                ORDER BY created_at DESC
                LIMIT 1
                """, (rs, rowNum) -> map(rs), orderId, provider);
        return payments.stream().findFirst().orElse(null);
    }

    public void insertEvent(UUID paymentId, String eventType, String payloadJson) {
        jdbcTemplate.update("""
                INSERT INTO payment_event (id, payment_id, event_type, payload_json, created_at)
                VALUES (?, ?, ?, ?::jsonb, now())
                """, UUID.randomUUID(), paymentId, eventType, payloadJson);
    }

    private Payment map(ResultSet rs) throws SQLException {
        return new Payment(
                rs.getObject("id", UUID.class),
                rs.getObject("order_id", UUID.class),
                rs.getString("provider"),
                PaymentStatus.valueOf(rs.getString("status")),
                new Money(rs.getLong("amount_amount"), rs.getString("amount_currency")),
                rs.getString("provider_transaction_id"),
                rs.getTimestamp("paid_at") == null ? null : rs.getTimestamp("paid_at").toInstant(),
                rs.getTimestamp("created_at").toInstant());
    }
}