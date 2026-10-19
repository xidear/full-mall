package com.fullmall.pos.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record CreatePosSaleRequest(
        @NotNull UUID storeId,
        @NotNull UUID shiftId,
        String cashierName,
        UUID memberId,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotNull UUID skuId,
            @Positive int quantity
    ) {
    }
}
