package com.fullmall.promotion.api;

import com.fullmall.platform.money.Money;
import com.fullmall.promotion.domain.PromotionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PromotionDtos {

    private PromotionDtos() {
    }

    public record PromotionView(
            UUID id,
            String code,
            PromotionType type,
            String title,
            String description,
            String status,
            Instant startsAt,
            Instant endsAt,
            boolean active,
            boolean soldOut,
            UUID skuId,
            String skuCode,
            UUID productId,
            String productTitle,
            Money originalPrice,
            Money promoPrice,
            UUID giftSkuId,
            String giftSkuCode,
            String giftTitle,
            Integer buyQuantity,
            Integer giftQuantity,
            Integer maxGiftPerOrder,
            Integer stockLimit,
            int soldQuantity,
            Integer remainingStock,
            Integer perUserLimit,
            Integer groupSize,
            Integer groupDurationHours,
            int priority
    ) {
    }

    public record CreatePromotionRequest(
            @NotBlank @Size(max = 64) String code,
            @NotNull PromotionType type,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 1000) String description,
            @NotNull Instant startsAt,
            @NotNull Instant endsAt,
            @NotNull UUID skuId,
            Long promoPriceAmount,
            UUID giftSkuId,
            @Min(1) Integer buyQuantity,
            @Min(1) Integer giftQuantity,
            @Min(1) Integer maxGiftPerOrder,
            @Min(1) Integer stockLimit,
            @Min(1) Integer perUserLimit,
            @Min(2) Integer groupSize,
            @Min(1) Integer groupDurationHours,
            Integer priority
    ) {
    }

    public record GroupBuyView(
            UUID id,
            String code,
            UUID promotionId,
            UUID skuId,
            String skuCode,
            String productTitle,
            Money price,
            int groupSize,
            int joinedCount,
            int paidCount,
            int remainingSlots,
            String status,
            Instant expiresAt,
            Instant createdAt,
            Instant completedAt,
            List<GroupBuyMemberView> members
    ) {
    }

    public record GroupBuyMemberView(
            String maskedPhone,
            String role,
            boolean paid,
            Instant joinedAt
    ) {
    }

    public record CreateGroupBuyRequest(
            @NotNull UUID promotionId,
            @NotBlank @Pattern(regexp = "^[0-9+\\- ]{6,32}$", message = "must be a valid phone number")
            String phone
    ) {
    }
}