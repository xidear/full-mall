package com.fullmall.payment.domain;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    PAID,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED
}