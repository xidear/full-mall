package com.fullmall.supplier.api;

import jakarta.validation.constraints.NotBlank;

public record SupplierRejectRequest(@NotBlank String reason) {
}
