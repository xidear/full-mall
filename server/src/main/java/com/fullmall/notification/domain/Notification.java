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

    /** 投递结果的不可变副本：让投递方法能把 SENT/FAILED 直接返回给调用方判断。 */
    public Notification withDelivery(String deliveryStatus, Instant deliveredAt, String deliveryError) {
        return new Notification(id, recipientType, recipientId, eventType, channel, title, content,
                deliveryStatus, readAt, deliveredAt, deliveryError, createdAt);
    }
}