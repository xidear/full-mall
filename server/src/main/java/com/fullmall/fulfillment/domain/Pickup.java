package com.fullmall.fulfillment.domain;

import java.time.Instant;
import java.util.UUID;

public record Pickup(
        UUID id,
        UUID fulfillmentOrderId,
        UUID storeId,
        String pickupCode,
        PickupStatus status,
        Instant expiresAt,
        Instant completedAt,
        String contactPhone,
        int totalUses,
        int usedCount
) {
    public int remainingUses() {
        return Math.max(0, totalUses - usedCount);
    }
}