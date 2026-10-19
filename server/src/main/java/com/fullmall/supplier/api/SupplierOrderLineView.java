package com.fullmall.supplier.api;

import com.fullmall.platform.money.Money;

import java.util.UUID;

public record SupplierOrderLineView(
        UUID skuId,
        String title,
        String skuCode,
        String supplierSkuCode,
        int quantity,
        Money unitPrice
) {
}
