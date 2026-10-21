package com.fullmall.promotion.app;

import com.fullmall.platform.error.ApiException;

/**
 * 砍价定价规则（纯函数，便于测试与复用）。
 *
 * <p>业务口径（用户确认 2026-10-19）：
 * <ul>
 *   <li>底价**运营手填**，系统只给"推荐值 = 成本 × 1.05"；</li>
 *   <li>**不设成本硬下限**——低价引流是合法策略；</li>
 *   <li>但低于成本必须显式声明（`allowBelowCost=true` + 原因），便于追责与报表标注；</li>
 *   <li>成本未知时允许启用，但同样要显式确认，报表显示"成本未知"而不是 0。</li>
 * </ul>
 */
public final class BargainPricing {

    private BargainPricing() {
    }

    public record Decision(long floorPrice, long recommendedPrice, boolean belowCost,
                           boolean costUnknown, boolean requiresConfirmation) {
    }

    /**
     * @param floorPrice      运营填写的成交底价（分）
     * @param cost            当前加权成本（分）；null 表示未知
     * @param allowBelowCost  是否显式允许低于成本
     * @param reason          低于成本或成本未知时的说明
     */
    public static Decision evaluate(long floorPrice, Long cost, boolean allowBelowCost, String reason) {
        if (floorPrice < 1) {
            throw ApiException.badRequest("BARGAIN_FLOOR_INVALID", "砍价底价必须大于 0 分");
        }
        if (cost == null) {
            if (!allowBelowCost || reason == null || reason.isBlank()) {
                throw ApiException.conflict("BARGAIN_COST_UNKNOWN",
                        "该商品没有成本记录，启用砍价需显式确认（allowBelowCost=true 并填写原因）");
            }
            return new Decision(floorPrice, 0, false, true, true);
        }
        if (cost < 0) {
            throw ApiException.badRequest("BARGAIN_COST_INVALID", "成本不能为负：" + cost);
        }
        long recommended = Math.round(cost * 1.05);
        boolean belowCost = floorPrice < cost;
        if (belowCost && (!allowBelowCost || reason == null || reason.isBlank())) {
            throw ApiException.conflict("BARGAIN_BELOW_COST_UNCONFIRMED",
                    "底价低于成本（成本 " + cost + "，底价 " + floorPrice
                            + "），如确为引流请显式确认并填写原因");
        }
        return new Decision(floorPrice, recommended, belowCost, false, belowCost);
    }
}
