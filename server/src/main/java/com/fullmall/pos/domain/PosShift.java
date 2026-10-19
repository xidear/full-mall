package com.fullmall.pos.domain;

import com.fullmall.platform.money.Money;

import java.time.Instant;
import java.util.UUID;

public record PosShift(
        UUID id,
        UUID storeId,
        UUID cashierId,
        String cashierName,
        PosShiftStatus status,
        Money openingFloat,
        Money expectedCash,
        Money actualCash,
        Money variance,
        String varianceReason,
        Instant openedAt,
        Instant closedAt
) {
}
