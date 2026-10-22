package com.fullmall.promotion;

import com.fullmall.promotion.app.PromotionAttributes;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 活动属性：门槛基准、排除项、叠券/积分开关。 */
class PromotionAttributesTest {

    private final UUID excluded = UUID.randomUUID();

    @Test
    void defaultsAreConservative() {
        var defaults = PromotionAttributes.defaults();

        assertThat(defaults.allowCouponStack()).as("默认不允许叠券").isFalse();
        assertThat(defaults.allowPoints()).as("默认不允许积分抵扣").isFalse();
        assertThat(defaults.thresholdBasis()).isEqualTo(PromotionAttributes.ThresholdBasis.AFTER_DISCOUNT);
    }

    @Test
    void thresholdBasisSwitchesBaseAmount() {
        var after = new PromotionAttributes(false, false,
                PromotionAttributes.ThresholdBasis.AFTER_DISCOUNT, Set.of(), null);
        var before = new PromotionAttributes(false, false,
                PromotionAttributes.ThresholdBasis.BEFORE_DISCOUNT, Set.of(), null);

        assertThat(after.thresholdBase(20_000, 3_000)).isEqualTo(17_000);
        assertThat(before.thresholdBase(20_000, 3_000)).isEqualTo(20_000);
        assertThat(after.thresholdBase(1_000, 5_000)).as("不能为负").isZero();
    }

    @Test
    void exclusionsAreRespected() {
        var attributes = new PromotionAttributes(true, true,
                PromotionAttributes.ThresholdBasis.AFTER_DISCOUNT, Set.of(excluded), "ORDER");

        assertThat(attributes.covers(excluded)).isFalse();
        assertThat(attributes.covers(UUID.randomUUID())).isTrue();
        assertThat(attributes.exclusiveGroup()).isEqualTo("ORDER");
    }
}
