package com.fullmall.promotion.domain;

import java.time.Instant;
import java.util.UUID;

public record Promotion(
        UUID id,
        String code,
        PromotionType type,
        String title,
        String description,
        PromotionStatus status,
        Instant startsAt,
        Instant endsAt,
        UUID skuId,
        Long promoPriceAmount,
        UUID giftSkuId,
        Integer buyQuantity,
        Integer giftQuantity,
        Integer maxGiftPerOrder,
        Integer stockLimit,
        int soldQuantity,
        Integer perUserLimit,
        Integer groupSize,
        Integer groupDurationHours,
        int priority,
        Instant createdAt
) {

    public boolean isActiveAt(Instant now) {
        return status == PromotionStatus.ACTIVE
                && !now.isBefore(startsAt)
                && now.isBefore(endsAt);
    }

    public boolean isSoldOut() {
        return type == PromotionType.FLASH_SALE
                && stockLimit != null
                && soldQuantity >= stockLimit;
    }
}
