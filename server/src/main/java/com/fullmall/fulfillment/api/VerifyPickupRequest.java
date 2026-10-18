package com.fullmall.fulfillment.api;

import jakarta.validation.constraints.NotBlank;

public record VerifyPickupRequest(
        @NotBlank String verificationType,
        @NotBlank String verificationValue
) {
}