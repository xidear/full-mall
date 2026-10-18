package com.fullmall.inventory.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.fullmall.platform.error.ApiException;

public final class FefoAllocator {

    private FefoAllocator() {
    }

    /**
     * Plan is pure: it does not mutate stock. The caller must perform an atomic
     * reservation for every allocation and fail if any reservation loses a race.
     */
    public static AllocationPlan plan(List<InventoryBatch> batches, int requested, java.time.LocalDate minimumExpiry) {
        if (requested <= 0) {
            throw ApiException.badRequest("INVALID_QUANTITY", "Requested quantity must be positive");
        }
        java.time.LocalDate minExpiry = minimumExpiry == null ? java.time.LocalDate.MIN : minimumExpiry;
        List<InventoryBatch> eligible = batches.stream()
                .filter(InventoryBatch::isSellable)
                .filter(batch -> batch.expiryDate() == null || !batch.expiryDate().isBefore(minExpiry))
                .sorted(Comparator
                        .comparing(InventoryBatch::expiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(InventoryBatch::receivedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<ReservationAllocation> allocations = new ArrayList<>();
        int remaining = requested;
        for (InventoryBatch batch : eligible) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(batch.availableQuantity(), remaining);
            if (take <= 0) {
                continue;
            }
            allocations.add(new ReservationAllocation(batch.id(), take, batch.expiryDate(), batch.unitCost()));
            remaining -= take;
        }
        return new AllocationPlan(allocations, remaining);
    }
}