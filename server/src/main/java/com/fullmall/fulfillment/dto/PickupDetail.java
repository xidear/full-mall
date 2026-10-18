package com.fullmall.fulfillment.dto;

import com.fullmall.platform.money.Money;

import java.time.Instant;
import java.util.List;

public record PickupDetail(
        String pickupCode,
        String status,
        String storeName,
        String orderNo,
        String customerName,
        String customerPhone,
        Money total,
        int totalUses,
        int usedCount,
        int remainingUses,
        Instant expiresAt,
        List<PickupItem> items
) {
    public record PickupItem(String title, int quantity) {
    }
}