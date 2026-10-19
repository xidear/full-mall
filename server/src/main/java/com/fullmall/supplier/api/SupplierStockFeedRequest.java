package com.fullmall.supplier.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SupplierStockFeedRequest(
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotBlank String supplierSkuCode,
            @Min(0) int availableQuantity,
            Long priceAmount,
            String priceCurrency
    ) {
    }
}
