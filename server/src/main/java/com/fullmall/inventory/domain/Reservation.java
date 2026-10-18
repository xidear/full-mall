package com.fullmall.inventory.domain;

import java.time.Instant;
import java.util.UUID;

public record Reservation(
        UUID id,
        UUID orderId,
        UUID orderLineId,
        UUID batchId,
        int quantity,
        ReservationStatus status,
        Instant expiresAt
) {
}