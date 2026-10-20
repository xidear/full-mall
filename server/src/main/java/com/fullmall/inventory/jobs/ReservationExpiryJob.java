package com.fullmall.inventory.jobs;

import com.fullmall.inventory.app.InventoryService;
import org.springframework.stereotype.Component;

/**
 * 库存预留释放的**执行逻辑**（不含调度）。
 * 调度由 `ReservationReleaseTask` 通过注册表驱动，因此不再需要 worker profile。
 */
@Component
public class ReservationExpiryJob {

    private final InventoryService inventoryService;

    public ReservationExpiryJob(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    public void releaseExpiredReservations() {
        inventoryService.releaseExpiredReservations();
    }
}