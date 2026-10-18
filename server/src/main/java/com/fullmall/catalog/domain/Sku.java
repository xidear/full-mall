package com.fullmall.catalog.domain;

import com.fullmall.platform.money.Money;

import java.util.Map;
import java.util.UUID;

public record Sku(
        UUID id,
        UUID productId,
        String skuCode,
        String barcode,
        Money price,
        Money compareAtPrice,
        Map<String, String> options,
        String imageUrl,
        ShelfLifePolicy shelfLifePolicy
) {
    public Sku {
        options = options == null ? Map.of() : Map.copyOf(options);
        shelfLifePolicy = shelfLifePolicy == null ? ShelfLifePolicy.none() : shelfLifePolicy;
    }
}