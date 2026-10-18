package com.fullmall.order.api;

import com.fullmall.platform.money.Money;

import java.time.Instant;
import java.util.UUID;

public record OrderSummary(
        UUID id,
        String orderNo,
        String status,
        Money total,
        String fulfillmentMode,
        String contactName,
        Instant createdAt
) {
}