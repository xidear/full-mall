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
                                          title, content, status, read_at, sent_at, error_message, created_at, link)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?)
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
                notification.errorMessage(),
                notification.link());
        return notification;
    }

    /** 按 id 读取（重试等运维操作需要原记录）。 */
    public java.util.Optional<Notification> findById(UUID id) {
        return jdbcTemplate.query("""
                SELECT id, recipient_type, recipient_id, event_type, channel, title, content,
                       status, read_at, sent_at, error_message, created_at
                  FROM notification WHERE id = ?
                """, (rs, rowNum) -> new Notification(
                rs.getObject("id", UUID.class), rs.getString("recipient_type"),
                rs.getString("recipient_id"), rs.getString("event_type"), rs.getString("channel"),
                rs.getString("title"), rs.getString("content"), rs.getString("status"),
                rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant(),
                rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toInstant(),
                rs.getString("error_message"),
                rs.getTimestamp("created_at").toInstant()), id).stream().findFirst();
    }

    public void updateDelivery(UUID id, String status, Instant sentAt, String errorMessage) {
        int updated = jdbcTemplate.update("""
                UPDATE notification SET status = ?, sent_at = ?, error_message = ? WHERE id = ?
                """, status, sentAt == null ? null : java.sql.Timestamp.from(sentAt), errorMessage, id);
        if (updated != 1) {
            throw new IllegalStateException("Notification not found: " + id);
        }
    }

    private static final String COLUMNS = """
            SELECT id, recipient_type, recipient_id, event_type, channel, title, content,
                   status, read_at, sent_at, error_message, created_at
            FROM notification
            """;

    public List<Notification> findForRecipient(String recipientType, String recipientId, int limit) {
        return jdbcTemplate.query(COLUMNS + """
                WHERE recipient_type = ? AND recipient_id = ?
                ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> new Notification(
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
                rs.getTimestamp("created_at").toInstant()),
                recipientType, recipientId, limit);
    }

    public boolean markRead(String recipientType, String recipientId, java.util.UUID notificationId) {
        return jdbcTemplate.update("""
                UPDATE notification SET read_at = now()
                 WHERE id = ? AND recipient_type = ? AND recipient_id = ? AND read_at IS NULL
                """, notificationId, recipientType, recipientId) == 1;
    }

    public int unreadCount(String recipientType, String recipientId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notification
                 WHERE recipient_type = ? AND recipient_id = ? AND read_at IS NULL
                """, Integer.class, recipientType, recipientId);
        if (count == null) {
            throw new IllegalStateException("未读消息计数返回 null");
        }
        return count;
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

    /**
     * 后台员工可见的通知：**只含发给自己的或广播（recipient_id='ALL'）的 STAFF 通知**。
     *
     * <p>原实现返回全表（含 CUSTOMER/MEMBER 通知，接收人是手机号），等于任何运营都能看到
     * 全站顾客手机号与事件——这是隐私事故。客户通知要看请走「客户详情」上下文并单独授权。
     */
    public List<Notification> findForStaff(String username, int limit) {
        return jdbcTemplate.query("""
                SELECT n.id, n.recipient_type, n.recipient_id, n.event_type, n.channel, n.title, n.content,
                       n.status, COALESCE(r.read_at, n.read_at) AS read_at, n.sent_at, n.error_message, n.created_at, n.state, n.link
                  FROM notification n
                  LEFT JOIN notification_read r
                    ON r.notification_id = n.id AND r.principal_type = 'STAFF' AND r.principal_id = ?
                 WHERE n.recipient_type = 'STAFF'
                   AND (n.recipient_id = 'ALL' OR n.recipient_id = ?)
                 ORDER BY n.created_at DESC
                 LIMIT ?
                """, (rs, rowNum) -> new Notification(
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
                rs.getTimestamp("created_at").toInstant(),
                rs.getString("state"),
                rs.getString("link")), username, username, Math.min(Math.max(limit, 1), 200));
    }

    /** 按人标记已读：只能标记"自己可见"的通知。 */
    public boolean markReadForStaff(String username, UUID notificationId) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO notification_read (notification_id, principal_type, principal_id)
                SELECT n.id, 'STAFF', ? FROM notification n
                 WHERE n.id = ? AND n.recipient_type = 'STAFF'
                   AND (n.recipient_id = 'ALL' OR n.recipient_id = ?)
                ON CONFLICT DO NOTHING
                """, username, notificationId, username);
        return inserted >= 1;
    }

    /** 认领：OPEN -> ACK。已读不等于已处理，这一步才是"有人接手了"。 */
    public int ack(String username, UUID notificationId) {
        return jdbcTemplate.update("""
                UPDATE notification SET state = 'ACK', ack_by = ?, ack_at = now()
                 WHERE id = ? AND recipient_type = 'STAFF'
                   AND (recipient_id = 'ALL' OR recipient_id = ?) AND state = 'OPEN'
                """, username, notificationId, username);
    }

    /** 修复完成：OPEN/ACK -> RESOLVED。 */
    public int resolve(String username, UUID notificationId) {
        return jdbcTemplate.update("""
                UPDATE notification SET state = 'RESOLVED', resolved_by = ?, resolved_at = now(),
                       ack_by = COALESCE(ack_by, ?), ack_at = COALESCE(ack_at, now())
                 WHERE id = ? AND recipient_type = 'STAFF'
                   AND (recipient_id = 'ALL' OR recipient_id = ?) AND state <> 'RESOLVED'
                """, username, username, notificationId, username);
    }

    /** 全部已读：只作用于当前员工可见且未读的通知（广播按人算）。 */
    public int markAllReadForStaff(String username) {
        return jdbcTemplate.update("""
                INSERT INTO notification_read (notification_id, principal_type, principal_id)
                SELECT n.id, 'STAFF', ? FROM notification n
                 WHERE n.recipient_type = 'STAFF'
                   AND (n.recipient_id = 'ALL' OR n.recipient_id = ?)
                   AND NOT EXISTS (
                       SELECT 1 FROM notification_read r
                        WHERE r.notification_id = n.id AND r.principal_type = 'STAFF' AND r.principal_id = ?)
                ON CONFLICT DO NOTHING
                """, username, username, username);
    }

    /** 当前员工未读数：别人读过不影响我。 */
    public long unreadCountForStaff(String username) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notification n
                 WHERE n.recipient_type = 'STAFF'
                   AND (n.recipient_id = 'ALL' OR n.recipient_id = ?)
                   AND NOT EXISTS (
                       SELECT 1 FROM notification_read r
                        WHERE r.notification_id = n.id AND r.principal_type = 'STAFF' AND r.principal_id = ?)
                """, Long.class, username, username);
        if (count == null) {
            throw new IllegalStateException("Unread notification count returned null for " + username);
        }
        return count;
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