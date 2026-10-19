package com.fullmall.promotion.domain;

import java.time.Instant;
import java.util.UUID;

public record GroupBuyMember(
        UUID id,
        UUID groupBuyId,
        UUID orderId,
        String phone,
        String role,
        Instant joinedAt,
        Instant paidAt
) {
}
