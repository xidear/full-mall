package com.fullmall.order.domain;

import java.time.Instant;
import java.util.UUID;

public record IdempotencyRecord(
        UUID id,
        String scope,
        String idempotencyKey,
        String requestHash,
        UUID responseOrderId,
        String status,
        Instant createdAt
) {
}