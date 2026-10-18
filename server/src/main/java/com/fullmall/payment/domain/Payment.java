package com.fullmall.payment.domain;

import com.fullmall.platform.money.Money;

import java.time.Instant;
import java.util.UUID;

public record Payment(
        UUID id,
        UUID orderId,
        String provider,
        PaymentStatus status,
        Money amount,
        String providerTransactionId,
        Instant paidAt,
        Instant createdAt
) {
}