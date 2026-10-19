package com.fullmall.pos.api;

import jakarta.validation.constraints.Positive;

public record RedeemPointsRequest(@Positive long points) {
}
