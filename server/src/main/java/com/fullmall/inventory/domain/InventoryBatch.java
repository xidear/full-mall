package com.fullmall.inventory.domain;

import com.fullmall.platform.money.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryBatch(
        UUID id,
        UUID skuId,
        LocationType locationType,
        UUID locationId,
        String batchNo,
        LocalDate productionDate,
        LocalDate expiryDate,
        int quantity,
        int reservedQuantity,
        InventoryBatchStatus status,
        Money unitCost,
        Instant receivedAt
) {
    public int availableQuantity() {
        return quantity - reservedQuantity;
    }

    public boolean isSellable() {
        return (status == InventoryBatchStatus.AVAILABLE || status == InventoryBatchStatus.NEAR_EXPIRY)
                && availableQuantity() > 0
                && (expiryDate == null || !expiryDate.isBefore(LocalDate.now()));
    }
}