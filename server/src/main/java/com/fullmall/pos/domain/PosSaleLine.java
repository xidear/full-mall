package com.fullmall.pos.domain;

import com.fullmall.platform.money.Money;

import java.util.UUID;

public record PosSaleLine(
        UUID id,
        UUID skuId,
        String title,
        String skuCode,
        int quantity,
        Money unitPrice,
        Money lineTotal,
        int refundedQuantity,
        Money refundedAmount,
        String batchSummary
) {
}
