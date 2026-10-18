package com.fullmall.theme.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ThemeSaveRequest(
        @NotBlank String channel,
        @NotBlank String market,
        @NotNull JsonNode config
) {
}