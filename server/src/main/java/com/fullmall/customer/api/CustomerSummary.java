package com.fullmall.customer.api;

import com.fullmall.platform.money.Money;

import java.time.Instant;

public record CustomerSummary(
        String phone,
        String name,
        long orderCount,
        Money totalSpent,
        Instant lastOrderAt
) {
}