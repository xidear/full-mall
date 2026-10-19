package com.fullmall.pos.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AddTenderRequest(
        @NotBlank String type,
        @Positive long amount,
        Long cashTendered,
        String provider,
        String idempotencyKey,
        String operatorName
) {
}
