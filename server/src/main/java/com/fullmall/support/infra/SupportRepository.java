package com.fullmall.support.infra;

import com.fullmall.support.api.SupportDtos.AgentView;
import com.fullmall.support.api.SupportDtos.ConversationView;
import com.fullmall.support.api.SupportDtos.MessageView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class SupportRepository {

    private final JdbcTemplate jdbcTemplate;

    public SupportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ConversationView create(UUID orderId, String locale, String subject,
                                   String customerName, String customerPhone) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO support_conversation (id, order_id, status, locale, subject,
                                                  customer_name, customer_phone, last_message_at)
                VALUES (?, ?, 'OPEN', ?, ?, ?, ?, now())
                """, id, orderId, locale, subject, customerName, customerPhone);
        return find(id);
    }

    public ConversationView find(UUID conversationId) {
        List<ConversationView> rows = jdbcTemplate.query("""
                SELECT c.id, c.subject, c.order_id, c.status, c.locale, c.agent_id,
                       u.display_name AS agent_name, c.customer_name, c.customer_phone,
                       c.created_at, c.assigned_at, c.last_message_at
                FROM support_conversation c
                LEFT JOIN staff_user u ON u.id = c.agent_id
                WHERE c.id = ?
                """, (rs, rowNum) -> mapConversation(rs), conversationId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<ConversationView> list(UUID agentId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.subject, c.order_id, c.status, c.locale, c.agent_id,
                       u.display_name AS agent_name, c.customer_name, c.customer_phone,
                       c.created_at, c.assigned_at, c.last_message_at
                FROM support_conversation c
                LEFT JOIN staff_user u ON u.id = c.agent_id
                WHERE 1 = 1
                """);
        if (agentId != null) {
            sql.append(" AND (c.agent_id = ? OR c.agent_id IS NULL)");
            args.add(agentId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND c.status = ?");
            args.add(status.trim().toUpperCase());
        }
        sql.append(" ORDER BY COALESCE(c.last_message_at, c.created_at) DESC LIMIT 200");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapConversation(rs), args.toArray());
    }

    public List<MessageView> messages(UUID conversationId) {
        return jdbcTemplate.query("""
                SELECT id, sender_type, sender_id, message_type, content, metadata_json, created_at
                FROM support_message
                WHERE conversation_id = ?
                ORDER BY created_at
                """, (rs, rowNum) -> new MessageView(
                rs.getObject("id", UUID.class), rs.getString("sender_type"),
                rs.getObject("sender_id", UUID.class), rs.getString("message_type"),
                rs.getString("content"), rs.getString("metadata_json"),
                rs.getTimestamp("created_at").toInstant()), conversationId);
    }

    public MessageView insertMessage(UUID conversationId, String senderType, UUID senderId,
                                     String messageType, String content, String metadataJson) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO support_message (id, conversation_id, sender_type, sender_id,
                                             message_type, content, metadata_json)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
                """, id, conversationId, senderType, senderId, messageType, content, metadataJson);
        jdbcTemplate.update("""
                UPDATE support_conversation SET last_message_at = now(),
                    status = CASE WHEN status = 'CLOSED' THEN 'OPEN' ELSE status END
                WHERE id = ?
                """, conversationId);
        List<MessageView> rows = jdbcTemplate.query("""
                SELECT id, sender_type, sender_id, message_type, content, metadata_json, created_at
                FROM support_message WHERE id = ?
                """, (rs, rowNum) -> new MessageView(
                rs.getObject("id", UUID.class), rs.getString("sender_type"),
                rs.getObject("sender_id", UUID.class), rs.getString("message_type"),
                rs.getString("content"), rs.getString("metadata_json"),
                rs.getTimestamp("created_at").toInstant()), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean claim(UUID conversationId, UUID agentId) {
        int updated = jdbcTemplate.update("""
                UPDATE support_conversation
                SET agent_id = ?, assigned_at = now()
                WHERE id = ? AND agent_id IS NULL
                """, agentId, conversationId);
        return updated == 1;
    }

    public boolean transfer(UUID conversationId, UUID fromAgentId, UUID toAgentId) {
        int updated = jdbcTemplate.update("""
                UPDATE support_conversation
                SET agent_id = ?, assigned_at = now()
                WHERE id = ? AND (agent_id = ? OR agent_id IS NULL)
                """, toAgentId, conversationId, fromAgentId);
        return updated == 1;
    }

    public void insertTransfer(UUID conversationId, UUID fromAgentId, UUID toAgentId, String reason) {
        jdbcTemplate.update("""
                INSERT INTO support_transfer (id, conversation_id, from_agent_id, to_agent_id, reason)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), conversationId, fromAgentId, toAgentId, reason);
    }

    public List<AgentView> agents() {
        return jdbcTemplate.query("""
                SELECT id, display_name, role FROM staff_user
                WHERE status = 'ACTIVE' AND role IN ('CS', 'ADMIN', 'OPERATOR')
                ORDER BY role, display_name
                """, (rs, rowNum) -> new AgentView(
                rs.getObject("id", UUID.class), rs.getString("display_name"), rs.getString("role")));
    }

    private ConversationView mapConversation(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ConversationView(
                rs.getObject("id", UUID.class), rs.getString("subject"),
                rs.getObject("order_id", UUID.class), rs.getString("status"), rs.getString("locale"),
                rs.getObject("agent_id", UUID.class), rs.getString("agent_name"),
                rs.getString("customer_name"), rs.getString("customer_phone"),
                rs.getTimestamp("created_at").toInstant(), instant(rs, "assigned_at"),
                instant(rs, "last_message_at"));
    }

    private Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}