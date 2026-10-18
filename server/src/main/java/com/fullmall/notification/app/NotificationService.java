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
        for (String channel : channels) {
            deliver("CUSTOMER", phones.get(0), eventType, channel, title, content);
        }
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