package com.fullmall.promotion.app;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionEngineTest {

    @Test
    void returnsNoneWhenThresholdNotReached() {
        var result = PromotionEngine.choose(
                List.of(new PromotionEngine.Rule("FULL-200", PromotionEngine.Type.FULL_REDUCTION, 20_000, 2_000, 10)),
                19_999);

        assertThat(result.applied()).isFalse();
        assertThat(result.discountAmount()).isZero();
    }

    @Test
    void fullReductionAppliesWhenReached() {
        var result = PromotionEngine.choose(
                List.of(new PromotionEngine.Rule("FULL-200", PromotionEngine.Type.FULL_REDUCTION, 20_000, 2_000, 10)),
                25_000);

        assertThat(result.appliedRuleCode()).isEqualTo("FULL-200");
        assertThat(result.discountAmount()).isEqualTo(2_000);
    }

    @Test
    void fullDiscountIsPercentageRoundedHalfUp() {
        var result = PromotionEngine.choose(
                List.of(new PromotionEngine.Rule("PCT-90", PromotionEngine.Type.FULL_DISCOUNT, 1_000, 90, 10)),
                3_333);

        assertThat(result.discountAmount()).isEqualTo(333);
    }

    @Test
    void picksLargestDiscountAcrossRules() {
        var result = PromotionEngine.choose(List.of(
                new PromotionEngine.Rule("A", PromotionEngine.Type.FULL_REDUCTION, 10_000, 500, 100),
                new PromotionEngine.Rule("B", PromotionEngine.Type.FULL_DISCOUNT, 10_000, 90, 1)), 10_000);

        assertThat(result.appliedRuleCode()).isEqualTo("B");
        assertThat(result.discountAmount()).isEqualTo(1_000);
    }

    @Test
    void tiesBreakByPriorityThenCodeForStability() {
        var byPriority = PromotionEngine.choose(List.of(
                new PromotionEngine.Rule("X", PromotionEngine.Type.FULL_REDUCTION, 1_000, 100, 1),
                new PromotionEngine.Rule("Y", PromotionEngine.Type.FULL_REDUCTION, 1_000, 100, 9)), 5_000);
        assertThat(byPriority.appliedRuleCode()).isEqualTo("Y");

        var byCode = PromotionEngine.choose(List.of(
                new PromotionEngine.Rule("B", PromotionEngine.Type.FULL_REDUCTION, 1_000, 100, 5),
                new PromotionEngine.Rule("A", PromotionEngine.Type.FULL_REDUCTION, 1_000, 100, 5)), 5_000);
        assertThat(byCode.appliedRuleCode()).isEqualTo("A");
    }

    @Test
    void discountNeverExceedsSubtotal() {
        var result = PromotionEngine.choose(
                List.of(new PromotionEngine.Rule("BIG", PromotionEngine.Type.FULL_REDUCTION, 1_000, 99_999, 1)),
                2_000);

        assertThat(result.discountAmount()).isEqualTo(2_000);
    }

    @Test
    void rejectsInvalidRules() {
        assertThatThrownBy(() -> PromotionEngine.choose(
                List.of(new PromotionEngine.Rule("BAD", PromotionEngine.Type.FULL_DISCOUNT, 1_000, 100, 1)), 5_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1..99");
        assertThatThrownBy(() -> PromotionEngine.choose(
                List.of(new PromotionEngine.Rule("BAD", PromotionEngine.Type.FULL_REDUCTION, 0, 100, 1)), 5_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("门槛");
    }
}
