package com.fullmall.supplier.api;

import java.util.UUID;

public record SupplierCredentialCreated(
        UUID supplierId,
        String accessKey,
        String secret
) {
}
