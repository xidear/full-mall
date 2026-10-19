package com.fullmall.promotion.domain;

import java.util.UUID;

public record PromotionClaim(
        UUID promotionId,
        PromotionType type,
        int saleQuantity,
        int giftQuantity
) {
}
