package com.fullmall.order.infra;

import com.fullmall.order.domain.IdempotencyRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class IdempotencyRepository {

    private final JdbcTemplate jdbcTemplate;

    public IdempotencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Serializes requests with the same scope + key until the current transaction commits.
     * This prevents two concurrent requests from creating two orders before the unique row is visible.
     */
    public void lock(String scope, String key) {
        String lockKey = scope + ":" + key;
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(hashtext(?))", (PreparedStatementCallback<Void>) statement -> {
            statement.setString(1, lockKey);
            statement.execute();
            return null;
        });
    }

    public Optional<IdempotencyRecord> find(String scope, String key) {
        List<IdempotencyRecord> records = jdbcTemplate.query("""
                SELECT id, scope, idempotency_key, request_hash, response_order_id, status, created_at
                FROM idempotency_record
                WHERE scope = ? AND idempotency_key = ?
                """, (rs, rowNum) -> new IdempotencyRecord(
                rs.getObject("id", UUID.class),
                rs.getString("scope"),
                rs.getString("idempotency_key"),
                rs.getString("request_hash"),
                rs.getObject("response_order_id", UUID.class),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()), scope, key);
        return records.stream().findFirst();
    }

    public void insert(String scope, String key, String requestHash, UUID orderId) {
        jdbcTemplate.update("""
                INSERT INTO idempotency_record (id, scope, idempotency_key, request_hash,
                                                response_order_id, status, created_at)
                VALUES (?, ?, ?, ?, ?, 'COMPLETED', now())
                """, UUID.randomUUID(), scope, key, requestHash, orderId);
    }
}