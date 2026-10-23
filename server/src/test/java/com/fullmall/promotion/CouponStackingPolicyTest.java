package com.fullmall.promotion;

import com.fullmall.promotion.app.CouponStackingPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** C2：单券覆盖全局口径；NULL 跟随全局；非法配置明确报错。 */
class CouponStackingPolicyTest {

    @Test
    void couponFlagOverridesGlobalDefault() {
        assertThat(CouponStackingPolicy.resolve(
                CouponStackingPolicy.Mode.EXCLUSIVE, true))
                .isEqualTo(CouponStackingPolicy.Mode.STACKABLE);
        assertThat(CouponStackingPolicy.resolve(
                CouponStackingPolicy.Mode.STACKABLE, false))
                .isEqualTo(CouponStackingPolicy.Mode.EXCLUSIVE);
    }

    @Test
    void nullFollowsGlobal() {
        assertThat(CouponStackingPolicy.resolve(CouponStackingPolicy.Mode.EXCLUSIVE, null))
                .isEqualTo(CouponStackingPolicy.Mode.EXCLUSIVE);
        assertThat(CouponStackingPolicy.resolve(CouponStackingPolicy.Mode.STACKABLE, null))
                .isEqualTo(CouponStackingPolicy.Mode.STACKABLE);
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> CouponStackingPolicy.parse("both", "app.promotion.coupon.mode"))
                .hasMessageContaining("只能是 EXCLUSIVE 或 STACKABLE");
        assertThatThrownBy(() -> CouponStackingPolicy.resolve(null, true))
                .hasMessageContaining("全局券叠加模式不能为空");
    }
}
