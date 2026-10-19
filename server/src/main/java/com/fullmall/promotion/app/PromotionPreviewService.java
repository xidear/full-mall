package com.fullmall.promotion.app;

import com.fullmall.catalog.app.CatalogService;
import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 促销试算（PRO-008）：给定商品明细，返回命中规则、优惠额与应付。
 *
 * <p>只读取**当前生效**的满减/满折规则（`status=ACTIVE` 且时间窗内），
 * 规则参数从 `promotion.rules` JSONB 读取，不需要新表。
 *
 * <p>顺序约定（与结算保持一致）：**促销 → 会员折扣 → 积分抵扣**。
 * 本接口只算促销这一层，会员/积分由结算负责。
 */
@Service
public class PromotionPreviewService {

    private final JdbcTemplate jdbcTemplate;
    private final CatalogService catalogService;

    public PromotionPreviewService(JdbcTemplate jdbcTemplate, CatalogService catalogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.catalogService = catalogService;
    }

    public record Item(UUID skuId, int quantity) {
    }

    public record Preview(long subtotalAmount, long discountAmount, long payableAmount,
                          String appliedRuleCode, String locale) {
    }

    public Preview preview(List<Item> items, String locale) {
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("EMPTY_ITEMS", "试算至少需要一个商品");
        }
        String resolvedLocale = locale == null || locale.isBlank() ? "zh-CN" : locale;

        long subtotal = 0;
        String currency = null;
        for (Item item : items) {
            if (item.quantity() <= 0) {
                throw ApiException.badRequest("INVALID_QUANTITY", "数量必须大于 0");
            }
            var sku = catalogService.getSku(item.skuId());
            if (currency == null) {
                currency = sku.price().currency();
            } else if (!currency.equals(sku.price().currency())) {
                throw ApiException.badRequest("CURRENCY_MISMATCH", "试算商品币种不一致");
            }
            subtotal = Math.addExact(subtotal, sku.price().amount() * item.quantity());
        }

        List<PromotionEngine.Rule> rules = loadActiveRules();
        var result = PromotionEngine.choose(rules, subtotal);
        return new Preview(subtotal, result.discountAmount(),
                subtotal - result.discountAmount(), result.appliedRuleCode(), resolvedLocale);
    }

    /** 当前生效的满减/满折规则（供结算与试算共用）。 */
    public List<PromotionEngine.Rule> loadActiveRules() {
        List<PromotionEngine.Rule> rules = new ArrayList<>();
        jdbcTemplate.query("""
                SELECT code, type, priority,
                       (rules->>'thresholdAmount')::bigint AS threshold_amount,
                       (rules->>'value')::bigint AS rule_value
                  FROM promotion
                 WHERE status = 'ACTIVE'
                   AND type IN ('FULL_REDUCTION', 'FULL_DISCOUNT')
                   AND starts_at <= now() AND ends_at >= now()
                """, rs -> {
            long threshold = rs.getLong("threshold_amount");
            if (rs.wasNull()) {
                return; // 未配置门槛视为未启用（配置错误由引擎抛错）
            }
            long value = rs.getLong("rule_value");
            rules.add(new PromotionEngine.Rule(
                    rs.getString("code"),
                    PromotionEngine.Type.valueOf(rs.getString("type")),
                    threshold, value, rs.getInt("priority")));
        });
        return rules;
    }
}
