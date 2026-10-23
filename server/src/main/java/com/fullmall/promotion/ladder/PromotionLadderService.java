package com.fullmall.promotion.ladder;

import com.fullmall.promotion.app.PromotionLadder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 阶梯促销接入结算（C9）。
 *
 * <p>口径（**保守默认**，等你确认后可改）：
 * <ul>
 *   <li>默认关闭（`app.promotion.ladder-enabled=false`）；</li>
 *   <li>开启后：已有活动型优惠（秒杀/买赠）时**跳过**阶梯，避免叠加失控；</li>
 *   <li>满赠只给赠品（同 SKU 合并数量），第二件半价按单价降序两两分组。</li>
 * </ul>
 */
@Service
public class PromotionLadderService {

    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public PromotionLadderService(JdbcTemplate jdbc,
                                  @Value("${app.promotion.ladder-enabled:false}") boolean enabled) {
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    public record Gift(String skuId, int quantity, String code) {
    }

    public record Result(long discountAmount, List<Gift> gifts, String appliedCode) {
        static Result none() {
            return new Result(0, List.of(), null);
        }
    }

    /**
     * @param subtotalFen    本单商品小计
     * @param unitPricesFen  参与第二件半价的单价（同一商品按数量展开）
     * @param hasCampaignDiscount 是否已有活动型优惠（有则跳过）
     */
    public Result evaluate(long subtotalFen, List<Long> unitPricesFen, boolean hasCampaignDiscount) {
        if (!enabled || hasCampaignDiscount) {
            return Result.none();
        }
        var rows = jdbc.query("""
                SELECT id, code, kind, threshold_fen, gift_sku_id, gift_quantity
                  FROM promotion_ladder
                 WHERE active
                   AND starts_at <= now()
                   AND (ends_at IS NULL OR ends_at >= now())
                 ORDER BY priority DESC, code
                """, (rs, rowNum) -> new LadderRow(
                rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("kind"),
                rs.getLong("threshold_fen"),
                rs.getObject("gift_sku_id", UUID.class), rs.getInt("gift_quantity")));
        if (rows.isEmpty()) {
            return Result.none();
        }

        // 必须按门槛升序：PromotionLadder.gift 取"最后一个命中的档"，顺序错了会取错档
        List<PromotionLadder.GiftTier> giftTiers = rows.stream()
                .filter(row -> "GIFT".equals(row.kind()))
                .map(row -> new PromotionLadder.GiftTier(row.thresholdFen(),
                        row.giftSkuId().toString(), row.giftQuantity()))
                .sorted(java.util.Comparator.comparingLong(PromotionLadder.GiftTier::thresholdFen))
                .toList();
        List<Gift> gifts = new ArrayList<>();
        String appliedCode = null;
        if (!giftTiers.isEmpty()) {
            var gift = PromotionLadder.gift(subtotalFen, giftTiers);
            if (gift.triggered()) {
                gifts.add(new Gift(gift.giftSkuId(), gift.giftQuantity(), "GIFT"));
                appliedCode = "GIFT";
            }
        }
        long discount = 0;
        if (rows.stream().anyMatch(row -> "SECOND_HALF".equals(row.kind())) && !unitPricesFen.isEmpty()) {
            discount = PromotionLadder.secondHalfDiscount(unitPricesFen);
            if (discount > 0) {
                appliedCode = appliedCode == null ? "SECOND_HALF" : appliedCode + "+SECOND_HALF";
            }
        }
        return new Result(discount, List.copyOf(gifts), appliedCode);
    }

    private record LadderRow(UUID id, String code, String kind, long thresholdFen,
                             UUID giftSkuId, int giftQuantity) {
    }
}
