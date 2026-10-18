package com.fullmall.legal.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LegalAcceptanceRequest(
        @NotNull UUID documentVersionId,
        @NotBlank String locale,
        @NotBlank @jakarta.validation.constraints.Pattern(regexp = "CLICKWRAP|E_SIGN") String method,
        UUID orderId,
        String sessionId
) {
}