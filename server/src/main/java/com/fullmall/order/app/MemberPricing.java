package com.fullmall.order.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 会员等级价（MEM-002）：按等级折扣率在**促销后金额**上再打折。
 *
 * <p>口径（写死在文档里，避免各处不一致）：
 * <ul>
 *   <li>折扣基数是"商品小计 − 促销优惠"，**不含运费**（运费不打折）；</li>
 *   <li>匿名或等级折扣率 ≥100 时不产生会员优惠；</li>
 *   <li>四舍五入到最小货币单位（HALF_UP），绝不出现浮点。</li>
 * </ul>
 */
public final class MemberPricing {

    private MemberPricing() {
    }

    public record Discount(long memberDiscountAmount, long payableAmount) {
    }

    /**
     * @param subtotalAmount        商品小计
     * @param promotionDiscount    促销优惠（正数表示减免）
     * @param shippingFeeAmount    运费
     * @param discountRate         等级折扣率，如 95 表示 95 折；null 表示无会员
     */
    public static Discount apply(long subtotalAmount, long promotionDiscount, long shippingFeeAmount,
                                 Integer discountRate) {
        if (subtotalAmount < 0 || promotionDiscount < 0 || shippingFeeAmount < 0) {
            throw new IllegalArgumentException("金额不能为负: subtotal=" + subtotalAmount
                    + ", promo=" + promotionDiscount + ", shipping=" + shippingFeeAmount);
        }
        if (promotionDiscount > subtotalAmount) {
            throw new IllegalArgumentException("促销优惠不能超过小计: " + promotionDiscount);
        }
        long base = subtotalAmount - promotionDiscount;
        long memberDiscount = 0;
        if (discountRate != null && discountRate > 0 && discountRate < 100) {
            memberDiscount = BigDecimal.valueOf(base)
                    .multiply(BigDecimal.valueOf(100 - discountRate))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .longValueExact();
        }
        long payable = base - memberDiscount + shippingFeeAmount;
        return new Discount(memberDiscount, payable);
    }
}
