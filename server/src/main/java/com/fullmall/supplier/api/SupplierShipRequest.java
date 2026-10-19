package com.fullmall.supplier.api;

import jakarta.validation.constraints.NotBlank;

public record SupplierShipRequest(
        @NotBlank String carrierCode,
        @NotBlank String trackingNo
) {
}
