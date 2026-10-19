package com.fullmall.pos.api;

import jakarta.validation.constraints.Min;

public record CloseShiftRequest(
        @Min(0) long actualCash,
        String reason
) {
}
