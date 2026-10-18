package com.fullmall.inventory.domain;

import com.fullmall.platform.money.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FefoAllocatorTest {

    private static final Money COST = Money.of(1000, "CNY");

    @Test
    void picksEarliestExpiryFirst() {
        InventoryBatch later = batch(LocalDate.now().plusDays(10), 10, "later");
        InventoryBatch earlier = batch(LocalDate.now().plusDays(3), 4, "earlier");
        InventoryBatch middle = batch(LocalDate.now().plusDays(5), 5, "middle");

        AllocationPlan plan = FefoAllocator.plan(List.of(later, middle, earlier), 8, LocalDate.now());

        assertTrue(plan.isComplete());
        assertEquals(2, plan.allocations().size());
        assertEquals(earlier.id(), plan.allocations().get(0).batchId());
        assertEquals(4, plan.allocations().get(0).quantity());
        assertEquals(middle.id(), plan.allocations().get(1).batchId());
        assertEquals(4, plan.allocations().get(1).quantity());
    }

    @Test
    void ignoresExpiredBatchesAndReportsShortfall() {
        InventoryBatch expired = new InventoryBatch(
                UUID.randomUUID(), UUID.randomUUID(), LocationType.WAREHOUSE, UUID.randomUUID(),
                "expired", LocalDate.now().minusDays(10), LocalDate.now().minusDays(1),
                100, 0, InventoryBatchStatus.AVAILABLE, COST, Instant.now());
        InventoryBatch fresh = batch(LocalDate.now().plusDays(5), 3, "fresh");

        AllocationPlan plan = FefoAllocator.plan(List.of(expired, fresh), 5, LocalDate.now());

        assertEquals(1, plan.allocations().size());
        assertEquals(3, plan.allocations().get(0).quantity());
        assertEquals(2, plan.shortfall());
    }

    @Test
    void respectsMinimumRemainingShelfLife() {
        InventoryBatch soon = batch(LocalDate.now().plusDays(2), 10, "soon");
        InventoryBatch later = batch(LocalDate.now().plusDays(10), 10, "later");

        AllocationPlan plan = FefoAllocator.plan(List.of(soon, later), 5, LocalDate.now().plusDays(7));

        assertEquals(1, plan.allocations().size());
        assertEquals(later.id(), plan.allocations().get(0).batchId());
    }

    private InventoryBatch batch(LocalDate expiry, int quantity, String batchNo) {
        return new InventoryBatch(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocationType.WAREHOUSE,
                UUID.randomUUID(),
                batchNo,
                expiry.minusDays(30),
                expiry,
                quantity,
                0,
                InventoryBatchStatus.AVAILABLE,
                COST,
                Instant.now());
    }
}