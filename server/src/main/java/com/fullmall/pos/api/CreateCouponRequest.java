package com.fullmall.pos.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

public record CreateCouponRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String discountType,
        @Positive long discountValue,
        @Min(0) long minSpendAmount,
        Long maxDiscountAmount,
        UUID memberId,
        @NotNull Instant validFrom,
        @NotNull Instant validTo,
        String currency
) {
}
