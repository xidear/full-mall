package com.fullmall.catalog.api;

import com.fullmall.catalog.domain.InventoryTrackingLevel;
import com.fullmall.catalog.domain.ProductStatus;
import com.fullmall.catalog.domain.ShelfLifePolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductUpsertRequest(
        @NotBlank String title,
        String description,
        UUID categoryId,
        ProductStatus status,
        @NotNull InventoryTrackingLevel inventoryTrackingLevel,
        boolean shelfLifeManaged,
        @Valid @NotEmpty List<SkuUpsert> skus
) {

    public record SkuUpsert(
            @NotBlank String skuCode,
            String barcode,
            @PositiveOrZero long priceAmount,
            @NotBlank @Size(min = 3, max = 3) String currency,
            Long compareAtAmount,
            Map<String, String> options,
            String imageUrl,
            ShelfLifePolicy shelfLifePolicy
    ) {
    }
}