package com.fullmall.pos.api;

import jakarta.validation.constraints.NotBlank;

public record RefundPosSaleRequest(
        @NotBlank String reason,
        String operatorName,
        String managerUsername,
        String managerPin
) {
}
