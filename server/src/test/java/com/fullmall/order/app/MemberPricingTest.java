package com.fullmall.order.app;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberPricingTest {

    @Test
    void anonymousGetsNoDiscount() {
        var result = MemberPricing.apply(10_000, 0, 1_000, null);

        assertThat(result.memberDiscountAmount()).isZero();
        assertThat(result.payableAmount()).isEqualTo(11_000);
    }

    @Test
    void discountAppliesAfterPromotionAndExcludesShipping() {
        // 小计 10000，促销减 2000 → 基数 8000；95 折 → 减 400；运费 1000
        var result = MemberPricing.apply(10_000, 2_000, 1_000, 95);

        assertThat(result.memberDiscountAmount()).isEqualTo(400);
        assertThat(result.payableAmount()).isEqualTo(8_600);
    }

    @Test
    void roundingIsHalfUpOnMinorUnits() {
        // 3333 * 5% = 166.65 → 167
        var result = MemberPricing.apply(3_333, 0, 0, 95);

        assertThat(result.memberDiscountAmount()).isEqualTo(167);
        assertThat(result.payableAmount()).isEqualTo(3_166);
    }

    @Test
    void rateAtOrAboveHundredIsNoDiscount() {
        assertThat(MemberPricing.apply(1_000, 0, 0, 100).memberDiscountAmount()).isZero();
        assertThat(MemberPricing.apply(1_000, 0, 0, 120).memberDiscountAmount()).isZero();
    }

    @Test
    void rejectsImpossibleAmounts() {
        assertThatThrownBy(() -> MemberPricing.apply(-1, 0, 0, 95))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MemberPricing.apply(100, 200, 0, 95))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能超过小计");
    }
}
