package com.fullmall.inventory.domain;

public enum InventoryBatchStatus {
    AVAILABLE,
    QUARANTINE,
    NEAR_EXPIRY,
    EXPIRED,
    BLOCKED
}