package com.fullmall.store.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Store(
        UUID id,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean pickupEnabled,
        String phone,
        String openingHours
) {
}