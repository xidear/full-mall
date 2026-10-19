package com.fullmall.pos.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpenShiftRequest(
        @NotNull UUID storeId,
        @NotBlank String cashierName,
        @Min(0) long openingFloat,
        String currency
) {
}
