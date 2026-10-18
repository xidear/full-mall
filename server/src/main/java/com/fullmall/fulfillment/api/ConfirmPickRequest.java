package com.fullmall.fulfillment.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmPickRequest(
        @NotNull UUID batchId,
        @Min(1) int quantity,
        String scannedCode
) {
}