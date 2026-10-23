package com.fullmall.promotion;

import com.fullmall.promotion.app.PromotionLadder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** C9：满赠取最高档；第二件半价按降序两两分组。 */
class PromotionLadderTest {

    @Test
    void giftTakesHighestMatchedTier() {
        var tiers = List.of(
                new PromotionLadder.GiftTier(10_000, "GIFT-A", 1),
                new PromotionLadder.GiftTier(20_000, "GIFT-B", 2));

        assertThat(PromotionLadder.gift(5_000, tiers).triggered()).isFalse();
        assertThat(PromotionLadder.gift(10_000, tiers).giftSkuId()).isEqualTo("GIFT-A");
        var best = PromotionLadder.gift(25_000, tiers);
        assertThat(best.giftSkuId()).isEqualTo("GIFT-B");
        assertThat(best.giftQuantity()).isEqualTo(2);
    }

    @Test
    void secondHalfDiscountPairsByDescendingPrice() {
        // 10000, 6000, 2000 → 分组 (10000,6000) 第二件 3000；(2000) 不成对
        assertThat(PromotionLadder.secondHalfDiscount(List.of(10_000L, 6_000L, 2_000L)))
                .isEqualTo(3_000L);
        assertThat(PromotionLadder.secondHalfDiscount(List.of(10_000L))).isZero();
        assertThat(PromotionLadder.secondHalfDiscount(List.of(10_000L, 10_000L, 8_000L, 8_000L)))
                .isEqualTo(5_000L + 4_000L);
    }

    @Test
    void invalidInputsAreRejected() {
        assertThatThrownBy(() -> PromotionLadder.gift(1_000, List.of()))
                .hasMessageContaining("阶梯不能为空");
        assertThatThrownBy(() -> PromotionLadder.gift(1_000,
                List.of(new PromotionLadder.GiftTier(0, "G", 1))))
                .hasMessageContaining("必须大于 0");
        assertThatThrownBy(() -> PromotionLadder.secondHalfDiscount(List.of(0L)))
                .hasMessageContaining("单价必须大于 0");
    }
}
