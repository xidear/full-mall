package com.fullmall.catalog.api;

import com.fullmall.catalog.domain.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusUpdateRequest(@NotNull ProductStatus status) {
}