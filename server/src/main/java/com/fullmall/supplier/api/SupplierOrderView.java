package com.fullmall.supplier.api;

import com.fullmall.platform.money.Money;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SupplierOrderView(
        UUID fulfillmentId,
        UUID orderId,
        String orderNo,
        String status,
        Instant createdAt,
        Instant acceptedAt,
        String carrierCode,
        String trackingNo,
        String contactName,
        String contactPhone,
        String addressJson,
        Money total,
        String note,
        List<SupplierOrderLineView> lines
) {
}
