package com.fullmall.notification.domain;

import java.time.Instant;
import java.util.UUID;

public record Notification(
        UUID id,
        String recipientType,
        String recipientId,
        String eventType,
        String channel,
        String title,
        String content,
        String status,
        Instant readAt,
        Instant sentAt,
        String errorMessage,
        Instant createdAt
) {
}