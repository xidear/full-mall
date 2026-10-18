package com.fullmall.inventory.jobs;

import com.fullmall.inventory.app.InventoryService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
public class ReservationExpiryJob {

    private final InventoryService inventoryService;

    public ReservationExpiryJob(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Scheduled(fixedDelayString = "${app.reservation.release-interval-ms:60000}")
    public void releaseExpiredReservations() {
        inventoryService.releaseExpiredReservations();
    }
}