package com.fullmall.promotion.app;

import com.fullmall.platform.error.ApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * 阶梯类促销（C9）：满赠 与 第二件半价。
 *
 * <p>纯函数，便于单测与在结算引擎里复用。
 * <ul>
 *   <li>满赠：达到门槛赠送指定 SKU（**不折现**，赠品数量按阶梯取最高档）；</li>
 *   <li>第二件半价：按"每两件一组"计算，取较便宜那件打五折（不足两件不优惠）。</li>
 * </ul>
 */
public final class PromotionLadder {

    private PromotionLadder() {
    }

    /** 满赠阶梯：thresholdFen 升序，giftSkuId 为赠品。 */
    public record GiftTier(long thresholdFen, String giftSkuId, int giftQuantity) {
    }

    public record GiftResult(boolean triggered, String giftSkuId, int giftQuantity) {
    }

    public static GiftResult gift(long subtotalFen, List<GiftTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw ApiException.badRequest("LADDER_EMPTY", "满赠阶梯不能为空");
        }
        GiftResult best = new GiftResult(false, null, 0);
        for (GiftTier tier : tiers) {
            if (tier.thresholdFen() <= 0 || tier.giftQuantity() <= 0
                    || tier.giftSkuId() == null || tier.giftSkuId().isBlank()) {
                throw ApiException.badRequest("LADDER_INVALID",
                        "阶梯门槛与赠品数量必须大于 0，赠品 SKU 不能为空");
            }
            if (subtotalFen >= tier.thresholdFen()) {
                best = new GiftResult(true, tier.giftSkuId(), tier.giftQuantity());
            }
        }
        return best;
    }

    /**
     * 第二件半价：对传入的单价列表计算优惠总额（分）。
     *
     * <p>规则：按价格降序两两分组，每组第二件五折；单件剩余不优惠。
     */
    public static long secondHalfDiscount(List<Long> unitPricesFen) {
        if (unitPricesFen == null || unitPricesFen.isEmpty()) {
            return 0;
        }
        List<Long> prices = new ArrayList<>(unitPricesFen);
        for (Long price : prices) {
            if (price == null || price <= 0) {
                throw ApiException.badRequest("LADDER_PRICE_INVALID", "单价必须大于 0");
            }
        }
        prices.sort(java.util.Comparator.reverseOrder());
        long discount = 0;
        for (int i = 1; i < prices.size(); i += 2) {
            discount += prices.get(i) / 2;
        }
        return discount;
    }
}
