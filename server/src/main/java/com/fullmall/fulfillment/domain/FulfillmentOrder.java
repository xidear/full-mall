package com.fullmall.fulfillment.domain;

import com.fullmall.platform.domain.FulfillmentType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FulfillmentOrder(
        UUID id,
        UUID orderId,
        FulfillmentType type,
        FulfillmentStatus status,
        UUID sourceId,
        String carrierCode,
        String trackingNo,
        String pickupCode,
        Instant pickupExpiresAt,
        List<FulfillmentLine> lines
) {
    public FulfillmentOrder {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}