package com.fullmall.inventory.domain;

import com.fullmall.platform.money.Money;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationAllocation(UUID batchId, int quantity, LocalDate expiryDate, Money unitCost) {
}