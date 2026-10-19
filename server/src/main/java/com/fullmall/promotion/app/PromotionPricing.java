package com.fullmall.promotion.app;

import com.fullmall.platform.money.Money;
import com.fullmall.promotion.domain.PromotionType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record PromotionPricing(
        Map<UUID, Money> priceOverrides,
        List<GiftLine> gifts,
        List<AppliedPromotion> applied,
        UUID groupBuyId
) {

    public PromotionPricing {
        priceOverrides = Map.copyOf(priceOverrides);
        gifts = List.copyOf(gifts);
        applied = List.copyOf(applied);
    }

    public Optional<AppliedPromotion> pricePromotionForSku(UUID skuId) {
        if (!priceOverrides.containsKey(skuId)) {
            return Optional.empty();
        }
        return applied.stream()
                .filter(promotion -> promotion.skuId().equals(skuId)
                        && promotion.type() != PromotionType.BUY_GIFT)
                .findFirst();
    }

    public Optional<AppliedPromotion> promotionForSku(UUID skuId) {
        return applied.stream().filter(promotion -> promotion.skuId().equals(skuId)).findFirst();
    }

    public record GiftLine(UUID promotionId, UUID skuId, int quantity) {
    }

    public record AppliedPromotion(
            UUID promotionId,
            String code,
            String title,
            PromotionType type,
            UUID skuId,
            int quantity,
            int giftQuantity
    ) {
    }
}
