package com.fullmall.inventory.domain;

import java.util.List;

public record AllocationPlan(List<ReservationAllocation> allocations, int shortfall) {

    public AllocationPlan {
        allocations = List.copyOf(allocations);
    }

    public boolean isComplete() {
        return shortfall == 0;
    }
}