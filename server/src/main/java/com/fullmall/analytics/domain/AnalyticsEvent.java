package com.fullmall.analytics.domain;

import java.time.Instant;
import java.util.UUID;

public record AnalyticsEvent(
        UUID id,
        String eventType,
        Instant occurredAt,
        String marketCode,
        String channel,
        UUID orderId,
        UUID skuId,
        String propertiesJson,
        int schemaVersion
) {
}