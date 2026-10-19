package com.fullmall.promotion.domain;

import com.fullmall.platform.money.Money;

import java.time.Instant;
import java.util.UUID;

public record GroupBuy(
        UUID id,
        String code,
        UUID promotionId,
        UUID skuId,
        Money price,
        int groupSize,
        String creatorPhone,
        String status,
        Instant expiresAt,
        Instant createdAt,
        Instant completedAt,
        int joinedCount,
        int paidCount
) {
    public boolean isForming() {
        return "FORMING".equals(status);
    }
}
