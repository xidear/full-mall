package com.fullmall.fulfillment.app;

import com.fullmall.inventory.domain.ReservationAllocation;

import java.util.List;
import java.util.UUID;

public record LineAllocation(
        UUID orderLineId,
        UUID skuId,
        int quantity,
        List<ReservationAllocation> allocations
) {
    public LineAllocation {
        allocations = allocations == null ? List.of() : List.copyOf(allocations);
    }
}