package com.fullmall.promotion;

import com.fullmall.promotion.app.BargainPricing;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 砍价底价规则：不设成本下限，但低于成本/成本未知必须显式确认。 */
class BargainPricingTest {

    @Test
    void normalPriceGetsRecommendation() {
        var decision = BargainPricing.evaluate(1_200, 1_000L, false, null);

        assertThat(decision.recommendedPrice()).isEqualTo(1_050);   // 成本 × 1.05
        assertThat(decision.belowCost()).isFalse();
        assertThat(decision.requiresConfirmation()).isFalse();
    }

    @Test
    void belowCostRequiresExplicitConfirmation() {
        assertThatThrownBy(() -> BargainPricing.evaluate(800, 1_000L, false, null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("底价低于成本");

        var confirmed = BargainPricing.evaluate(800, 1_000L, true, "新品引流");
        assertThat(confirmed.belowCost()).isTrue();
        assertThat(confirmed.requiresConfirmation()).isTrue();
    }

    @Test
    void unknownCostRequiresConfirmationButStillAllowed() {
        assertThatThrownBy(() -> BargainPricing.evaluate(500, null, false, null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("没有成本记录");

        var confirmed = BargainPricing.evaluate(500, null, true, "成本待补");
        assertThat(confirmed.costUnknown()).isTrue();
        assertThat(confirmed.recommendedPrice()).isZero();   // 无成本不给推荐值，不按 0 算
    }

    @Test
    void invalidFloorIsRejected() {
        assertThatThrownBy(() -> BargainPricing.evaluate(0, 1_000L, true, "x"))
                .hasMessageContaining("必须大于 0");
        assertThatCode(() -> BargainPricing.evaluate(1, 1_000L, true, "清库存"))
                .doesNotThrowAnyException();
    }
}
