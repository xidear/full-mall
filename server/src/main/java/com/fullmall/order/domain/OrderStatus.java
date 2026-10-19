package com.fullmall.order.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    ALLOCATED,
    PARTIALLY_FULFILLED,
    FULFILLED,
    CANCELLED,
    PARTIALLY_REFUNDED,
    REFUNDED
}