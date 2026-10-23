package com.fullmall.promotion.app;

/**
 * 券与促销的叠加判定（C2）：把"全局默认 + 单券覆盖"收敛到一个地方。
 *
 * <p>规则：券模板 `stackable` 非空时以券为准，为空时用全局配置 `app.promotion.coupon.mode`。
 * 互斥语义（EXCLUSIVE）是"取更优者"，不是"券直接失效"——用户不该因为选了券反而更贵。
 */
public final class CouponStackingPolicy {

    public enum Mode { EXCLUSIVE, STACKABLE }

    private CouponStackingPolicy() {
    }

    public static Mode resolve(Mode globalDefault, Boolean couponStackable) {
        if (globalDefault == null) {
            throw new IllegalArgumentException("全局券叠加模式不能为空");
        }
        if (couponStackable == null) {
            return globalDefault;
        }
        return couponStackable ? Mode.STACKABLE : Mode.EXCLUSIVE;
    }

    public static Mode parse(String value, String source) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(source + " 不能为空");
        }
        try {
            return Mode.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    source + " 只能是 EXCLUSIVE 或 STACKABLE，实际=" + value);
        }
    }
}
