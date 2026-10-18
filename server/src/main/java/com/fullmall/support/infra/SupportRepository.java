package com.fullmall.support.infra;

import com.fullmall.support.domain.SupportConversation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class SupportRepository {

    private final JdbcTemplate jdbcTemplate;

    public SupportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SupportConversation insert(UUID orderId, String locale) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO support_conversation (id, customer_id, agent_id, order_id, status, locale, created_at)
                VALUES (?, NULL, NULL, ?, 'OPEN', ?, now())
                """, id, orderId, locale);
        return new SupportConversation(id, orderId, "OPEN", locale, java.time.Instant.now());
    }

    public SupportConversation findById(UUID conversationId) {
        List<SupportConversation> conversations = jdbcTemplate.query("""
                SELECT id, order_id, status, locale, created_at
                FROM support_conversation
                WHERE id = ?
                """, (rs, rowNum) -> new SupportConversation(
                rs.getObject("id", UUID.class),
                rs.getObject("order_id", UUID.class),
                rs.getString("status"),
                rs.getString("locale"),
                rs.getTimestamp("created_at").toInstant()), conversationId);
        return conversations.stream().findFirst().orElse(null);
    }
}