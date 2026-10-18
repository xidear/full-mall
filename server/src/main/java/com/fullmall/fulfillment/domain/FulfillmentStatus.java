package com.fullmall.fulfillment.domain;

public enum FulfillmentStatus {
    PENDING,
    ALLOCATED,
    PICKING,
    PACKED,
    SHIPPED,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED
}