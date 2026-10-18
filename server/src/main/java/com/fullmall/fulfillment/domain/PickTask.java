package com.fullmall.fulfillment.domain;

import java.util.List;
import java.util.UUID;

public record PickTask(
        UUID id,
        UUID fulfillmentOrderId,
        UUID storeId,
        String status,
        List<PickTaskLine> lines
) {
    public PickTask {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}