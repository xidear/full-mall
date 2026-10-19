package com.fullmall.supplier.api;

import java.time.Instant;
import java.util.UUID;

public record SupplierCredentialView(
        UUID supplierId,
        String code,
        String name,
        boolean active,
        String accessKey,
        boolean hasCredential,
        Instant rotatedAt
) {
}
