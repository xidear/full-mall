package com.fullmall.catalog.domain;

import java.util.List;
import java.util.UUID;

public record Product(
        UUID id,
        String title,
        String description,
        UUID categoryId,
        ProductStatus status,
        InventoryTrackingLevel inventoryTrackingLevel,
        boolean shelfLifeManaged,
        List<Sku> skus
) {
    public Product {
        skus = skus == null ? List.of() : List.copyOf(skus);
    }
}