package com.fullmall.notification.app;

import com.fullmall.notification.channel.NotificationChannelSender;
import com.fullmall.notification.domain.Notification;
import com.fullmall.notification.infra.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final Map<String, NotificationChannelSender> senders;

    public NotificationService(NotificationRepository repository,
                               JdbcTemplate jdbcTemplate,
                               List<NotificationChannelSender> senders) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.senders = senders.stream().collect(Collectors.toMap(
                NotificationChannelSender::channel,
                Function.identity()));
    }

    public void notifyStaff(String eventType, String title, String content) {
        deliver("STAFF", "ALL", eventType, "IN_APP", title, content);
    }

    public void notifyOrderEvent(UUID orderId, String eventType, String title, String content,
                                 List<String> channels) {
        List<String> phones = jdbcTemplate.query("""
                SELECT contact_phone FROM orders WHERE id = ?
                """, (rs, rowNum) -> rs.getString("contact_phone"), orderId);
        if (phones.isEmpty()) {
            throw new IllegalStateException("Order not found for notification: " + orderId);
        }
        notifyStaff(eventType, title, content);

        // 身份统一：订单已绑定会员时，站内消息写到 MEMBER/<会员ID>（消息中心只认会员），
        // 短信等外部渠道仍按手机号投递（短信必须用号码）。
        UUID memberId = jdbcTemplate.query("""
                SELECT member_id FROM orders WHERE id = ?
                """, (rs, rowNum) -> rs.getObject("member_id", UUID.class), orderId)
                .stream().filter(java.util.Objects::nonNull).findFirst().orElse(null);

        for (String channel : channels) {
            if (memberId != null && "IN_APP".equals(channel)) {
                deliver("MEMBER", memberId.toString(), eventType, channel, title, content);
            } else {
                deliver("CUSTOMER", phones.get(0), eventType, channel, title, content);
            }
        }
    }

    /**
     * 给会员写一条站内消息（订单事件用）。
     *
     * @param memberId 会员 UUID；为空时静默跳过属于错误用法，调用方必须自行判空
     */
    public void notifyMember(String memberId, String eventType, String title, String content) {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("notifyMember 需要会员 ID");
        }
        deliver("MEMBER", memberId, eventType, "IN_APP", title, content);
    }

    /**
     * 清理旧消息（保留策略）。
     *
     * @param days 保留天数，必须 &gt; 0
     * @return 删除条数
     */
    @org.springframework.transaction.annotation.Transactional
    public int purgeOlderThan(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("保留天数必须大于 0，实际=" + days);
        }
        return jdbcTemplate.update("""
                DELETE FROM notification WHERE created_at < now() - make_interval(days => ?)
                """, days);
    }

    /** 顾客侧消息列表（按收件人）。 */
    public List<Notification> findForRecipient(String recipientType, String recipientId, int limit) {
        if (recipientId == null || recipientId.isBlank()) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "RECIPIENT_REQUIRED", "缺少收件人");
        }
        if (limit <= 0 || limit > 100) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "INVALID_LIMIT", "查询条数必须在 1..100，实际=" + limit);
        }
        return repository.findForRecipient(recipientType, recipientId, limit);
    }

    /** 标记单条已读（只能标记自己的）。 */
    public boolean markRead(String recipientType, String recipientId, java.util.UUID notificationId) {
        if (recipientId == null || recipientId.isBlank()) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "RECIPIENT_REQUIRED", "缺少收件人");
        }
        return repository.markRead(recipientType, recipientId, notificationId);
    }

    public int unreadCount(String recipientType, String recipientId) {
        return repository.unreadCount(recipientType, recipientId);
    }

    public List<Notification> findRecent(int limit, String channel, String recipientType) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return repository.findRecent(safeLimit, channel, recipientType);
    }

    public void markRead(UUID id) {
        repository.markRead(id);
    }

    public long countUnreadStaff() {
        return repository.countUnreadStaff();
    }

    private void deliver(String recipientType, String recipientId, String eventType,
                         String channel, String title, String content) {
        Notification base = new Notification(
                UUID.randomUUID(),
                recipientType,
                recipientId,
                eventType,
                channel,
                title,
                content,
                "PENDING",
                null,
                null,
                null,
                Instant.now());
        repository.insert(base);

        NotificationChannelSender sender = senders.get(channel);
        if (sender == null) {
            repository.updateDelivery(base.id(), "FAILED", null, "No sender configured for " + channel);
            log.warn("No notification sender configured for channel {}", channel);
            return;
        }
        try {
            sender.send(base);
            repository.updateDelivery(base.id(), "SENT", Instant.now(), null);
        } catch (RuntimeException exception) {
            repository.updateDelivery(base.id(), "FAILED", null, exception.getMessage());
            log.error("Notification delivery failed channel={} recipient={}", channel, recipientId, exception);
        }
    }
}