package com.fullmall.promotion;

import com.fullmall.promotion.app.PromotionStackingPolicy;
import com.fullmall.promotion.app.PromotionStackingPolicy.Candidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 叠加策略：默认只取最优；STACK 模式下同互斥组只生效一条。 */
class PromotionStackingPolicyTest {

    private final List<Candidate> candidates = List.of(
            new Candidate("FULL-200-20", 2_000, 10, "ORDER_DISCOUNT"),
            new Candidate("FULL-500-80", 8_000, 20, "ORDER_DISCOUNT"),
            new Candidate("COUPON-50", 5_000, 30, null));

    @Test
    void bestOnlyKeepsSingleBest() {
        var applied = PromotionStackingPolicy.apply(PromotionStackingPolicy.Mode.BEST_ONLY, candidates);

        assertThat(applied.applied()).hasSize(1);
        assertThat(applied.applied().get(0).code()).isEqualTo("FULL-500-80");
        assertThat(applied.totalDiscount()).isEqualTo(8_000);
    }

    @Test
    void stackRespectsExclusiveGroup() {
        var applied = PromotionStackingPolicy.apply(PromotionStackingPolicy.Mode.STACK, candidates);

        assertThat(applied.applied()).extracting(Candidate::code)
                .containsExactly("COUPON-50", "FULL-500-80");
        assertThat(applied.totalDiscount()).isEqualTo(13_000);
        assertThat(applied.applied()).extracting(Candidate::code)
                .as("同组只允许一条，FULL-200-20 必须被排除")
                .doesNotContain("FULL-200-20");
    }

    @Test
    void emptyCandidatesYieldZero() {
        assertThat(PromotionStackingPolicy.apply(PromotionStackingPolicy.Mode.STACK, List.of()).totalDiscount())
                .isZero();
    }
}
