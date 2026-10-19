package com.fullmall.pos.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustPointsRequest(
        @NotNull Long points,
        @NotBlank String reason
) {
}
