package com.fullmall.pos.api;

import jakarta.validation.constraints.NotBlank;

public record ApplyCouponRequest(@NotBlank String code) {
}
