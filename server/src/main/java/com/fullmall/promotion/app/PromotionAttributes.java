package com.fullmall.promotion.app;

import java.util.Set;
import java.util.UUID;

/**
 * 活动属性（用户口径 2026-10-19）。
 *
 * @param allowCouponStack 该活动覆盖的商品是否还能用券
 * @param allowPoints      是否允许积分抵扣
 * @param thresholdBasis   门槛按促销前还是促销后金额判断
 * @param excludeSkuIds    参与范围内需要排除的商品
 * @param exclusiveGroup   互斥组（同组只能生效一个）
 */
public record PromotionAttributes(boolean allowCouponStack, boolean allowPoints,
                                  ThresholdBasis thresholdBasis, Set<UUID> excludeSkuIds,
                                  String exclusiveGroup) {

    public enum ThresholdBasis { BEFORE_DISCOUNT, AFTER_DISCOUNT }

    public static PromotionAttributes defaults() {
        // 默认组合：**不允许叠券、不允许积分**、门槛按促销后判断（与现有行为最接近）
        return new PromotionAttributes(false, false, ThresholdBasis.AFTER_DISCOUNT, Set.of(), null);
    }

    public boolean covers(UUID skuId) {
        return excludeSkuIds == null || !excludeSkuIds.contains(skuId);
    }

    /**
     * 门槛判断金额。
     *
     * @param originalAmount   原价小计
     * @param promotionDiscount 活动优惠（非负）
     */
    public long thresholdBase(long originalAmount, long promotionDiscount) {
        if (thresholdBasis == ThresholdBasis.BEFORE_DISCOUNT) {
            return originalAmount;
        }
        return Math.max(0, originalAmount - Math.max(0, promotionDiscount));
    }
}
