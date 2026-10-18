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