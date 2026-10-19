package com.fullmall.pos.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateMemberLevelRequest(
        @Min(1) @Max(100) int discountRate,
        @Min(1) int pointsMultiplier,
        @Min(0) long birthdayPoints
) {
}
