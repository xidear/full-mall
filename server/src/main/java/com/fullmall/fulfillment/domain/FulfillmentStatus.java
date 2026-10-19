package com.fullmall.fulfillment.domain;

public enum FulfillmentStatus {
    PENDING,
    ALLOCATED,
    ACCEPTED,
    PICKING,
    PACKED,
    SHIPPED,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED
}