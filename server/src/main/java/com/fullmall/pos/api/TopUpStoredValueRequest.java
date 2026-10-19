package com.fullmall.pos.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TopUpStoredValueRequest(
        @Positive long amount,
        @NotBlank String reason
) {
}
