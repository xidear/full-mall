package com.fullmall.pos.domain;

import com.fullmall.platform.money.Money;

import java.util.UUID;

public record PosSaleAllocation(
        UUID id,
        UUID saleLineId,
        UUID batchId,
        int quantity,
        int refundedQuantity,
        Money unitCost
) {
}
