package com.fullmall.pos.domain;

import com.fullmall.platform.money.Money;

import java.time.Instant;
import java.util.UUID;

public record PosTender(
        UUID id,
        TenderType type,
        Money amount,
        String provider,
        String providerTradeNo,
        TenderStatus status,
        Money cashTendered,
        Money change,
        Money refundedAmount,
        String reversalStatus,
        Instant paidAt
) {
}
