package com.fullmall.pos.domain;

import com.fullmall.platform.money.Money;

import java.time.Instant;
import java.util.UUID;

public record PosCoupon(
        UUID id,
        String code,
        String name,
        CouponDiscountType discountType,
        long discountValue,
        Money minSpend,
        Money maxDiscount,
        String currency,
        CouponStatus status,
        UUID memberId,
        Instant validFrom,
        Instant validTo
) {
}
