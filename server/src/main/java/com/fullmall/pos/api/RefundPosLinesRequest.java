package com.fullmall.pos.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record RefundPosLinesRequest(
        @NotEmpty List<@Valid Item> items,
        @NotBlank String reason,
        String operatorName
) {
    public record Item(
            @NotNull UUID saleLineId,
            @Positive int quantity
    ) {
    }
}
