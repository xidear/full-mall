package com.fullmall.pos.api;

import com.fullmall.platform.money.Money;

import java.util.UUID;

public record PosProductView(
        UUID skuId,
        String skuCode,
        String barcode,
        String title,
        Money price,
        int availableQuantity,
        String imageUrl
) {
}
