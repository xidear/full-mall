package com.fullmall.inventory.domain;

import java.util.UUID;

public record StockMove(
        UUID batchId,
        UUID skuId,
        LocationType locationType,
        UUID locationId,
        String moveType,
        int quantity,
        String referenceType,
        UUID referenceId
) {
}