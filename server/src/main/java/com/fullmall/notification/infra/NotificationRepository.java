package com.fullmall.notification.infra;

import com.fullmall.notification.domain.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class NotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Notification insert(Notification notification) {
        jdbcTemplate.update("""
                INSERT INTO notification (id, recipient_type, recipient_id, event_type, channel,
                                          title, content, status, read_at, sent_at, error_message, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """,
                notification.id(),
                notification.recipientType(),
                notification.recipientId(),
                notification.eventType(),
                notification.channel(),
                notification.title(),
                notification.content(),
                notification.status(),
                notification.readAt() == null ? null : java.sql.Timestamp.from(notification.readAt()),
                notification.sentAt() == null ? null : java.sql.Timestamp.from(notification.sentAt()),
                notification.errorMessage());
        return notification;
    }

    public void updateDelivery(UUID id, String status, Instant sentAt, String errorMessage) {
        int updated = jdbcTemplate.update("""
                UPDATE notification SET status = ?, sent_at = ?, error_message = ? WHERE id = ?
                """, status, sentAt == null ? null : java.sql.Timestamp.from(sentAt), errorMessage, id);
        if (updated != 1) {
            throw new IllegalStateException("Notification not found: " + id);
        }
    }

    public List<Notification> findRecent(int limit, String channel, String recipientType) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, recipient_type, recipient_id, event_type, channel, title, content,
                       status, read_at, sent_at, error_message, created_at
                FROM notification
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (channel != null && !channel.isBlank()) {
            sql.append(" AND channel = ?");
            args.add(channel);
        }
        if (recipientType != null && !recipientType.isBlank()) {
            sql.append(" AND recipient_type = ?");
            args.add(recipientType);
        }
        sql.append(" ORDER BY created_at DESC LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new Notification(
                rs.getObject("id", UUID.class),
                rs.getString("recipient_type"),
                rs.getString("recipient_id"),
                rs.getString("event_type"),
                rs.getString("channel"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("status"),
                rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant(),
                rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toInstant(),
                rs.getString("error_message"),
                rs.getTimestamp("created_at").toInstant()), args.toArray());
    }

    public void markRead(UUID id) {
        jdbcTemplate.update("UPDATE notification SET read_at = now() WHERE id = ? AND read_at IS NULL", id);
    }

    public long countUnreadStaff() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notification
                WHERE recipient_type = 'STAFF' AND read_at IS NULL
                """, Long.class);
        if (count == null) {
            throw new IllegalStateException("Unread notification count returned null");
        }
        return count;
    }
}