package com.fullmall.order.domain;

import com.fullmall.platform.money.Money;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        String orderNo,
        OrderStatus status,
        String currency,
        Money subtotal,
        Money shippingFee,
        Money discount,
        Money total,
        String contactName,
        String contactPhone,
        String contactEmail,
        String addressJson,
        FulfillmentMode fulfillmentMode,
        UUID storeId,
        Instant createdAt,
        List<OrderLine> lines
) {
    public Order {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}