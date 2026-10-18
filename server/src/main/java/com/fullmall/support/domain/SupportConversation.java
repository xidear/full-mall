package com.fullmall.support.domain;

import java.time.Instant;
import java.util.UUID;

public record SupportConversation(
        UUID id,
        UUID orderId,
        String status,
        String locale,
        Instant createdAt
) {
}