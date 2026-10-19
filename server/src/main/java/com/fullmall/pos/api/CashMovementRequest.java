package com.fullmall.pos.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CashMovementRequest(
        @NotBlank String type,
        @Positive long amount,
        @NotBlank String reason,
        String operatorName
) {
}
