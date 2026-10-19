package com.fullmall.supplier.api;

import java.time.Instant;
import java.util.UUID;

public record SupplierPingView(
        UUID supplierId,
        String supplierName,
        Instant serverTime
) {
}
