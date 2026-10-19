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
        return preview(items, locale, "ONLINE");
    }

    public Preview preview(List<Item> items, String locale, String channel) {
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

        java.util.Map<UUID, Long> subtotalsBySku = new java.util.LinkedHashMap<>();
        for (Item item : items) {
            long line = catalogService.getSku(item.skuId()).price().amount() * item.quantity();
            subtotalsBySku.merge(item.skuId(), line, Long::sum);
        }
        var result = PromotionEngine.chooseScoped(scopedRules(channel, subtotalsBySku));
        return new Preview(subtotal, result.discountAmount(),
                subtotal - result.discountAmount(), result.appliedRuleCode(), resolvedLocale);
    }

    /**
     * 统一口径入口：试算与结算共用。
     *
     * @param subtotalsBySku 每个 SKU 的小计（分）
     */
    public List<PromotionEngine.ScopedRule> scopedRules(String channel,
                                                        java.util.Map<UUID, Long> subtotalsBySku) {
        if (subtotalsBySku == null || subtotalsBySku.isEmpty()) {
            return List.of();
        }
        long total = subtotalsBySku.values().stream().mapToLong(Long::longValue).sum();
        List<PromotionEngine.ScopedRule> result = new ArrayList<>();
        for (PromotionEngine.Rule rule : loadActiveRules(channel)) {
            java.util.Set<String> scopeIds = scopeIds(rule.code());
            long base = scopeIds.isEmpty() ? total
                    : subtotalsBySku.entrySet().stream()
                        .filter(entry -> scopeIds.contains(entry.getKey().toString()))
                        .mapToLong(java.util.Map.Entry::getValue)
                        .sum();
            result.add(new PromotionEngine.ScopedRule(rule, base));
        }
        return result;
    }

    /**
     * 规则命中范围内的金额。`rules.scopeIds` 目前按 SKU 过滤；缺省表示全场。
     */
    private long eligibleSubtotal(PromotionEngine.Rule rule, List<Item> items) {
        java.util.Set<String> scopeIds = scopeIds(rule.code());
        if (scopeIds.isEmpty()) {
            return items.stream()
                    .mapToLong(item -> catalogService.getSku(item.skuId()).price().amount() * item.quantity())
                    .sum();
        }
        return items.stream()
                .filter(item -> scopeIds.contains(item.skuId().toString()))
                .mapToLong(item -> catalogService.getSku(item.skuId()).price().amount() * item.quantity())
                .sum();
    }

    private java.util.Set<String> scopeIds(String ruleCode) {
        List<String> raw = jdbcTemplate.queryForList("""
                SELECT jsonb_array_elements_text(COALESCE(rules->'scopeIds', '[]'::jsonb)) AS id
                  FROM promotion WHERE code = ?
                """, String.class, ruleCode);
        return new java.util.HashSet<>(raw);
    }

    /**
     * 当前生效的满减/满折规则（供结算与试算共用）。
     *
     * <p>渠道隔离（PRO-004）：规则的 `rules.channel` 决定生效渠道，缺省为 `ONLINE`；
     * 线上只认 `ONLINE`，POS 路径不走本引擎（避免自提二次优惠，PRO-005）。
     */
    public List<PromotionEngine.Rule> loadActiveRules(String channel) {
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("渠道不能为空");
        }
        List<PromotionEngine.Rule> rules = new ArrayList<>();
        jdbcTemplate.query("""
                SELECT code, type, priority,
                       (rules->>'thresholdAmount')::bigint AS threshold_amount,
                       (rules->>'value')::bigint AS rule_value
                  FROM promotion
                 WHERE status = 'ACTIVE'
                   AND type IN ('FULL_REDUCTION', 'FULL_DISCOUNT')
                   AND starts_at <= now() AND ends_at >= now()
                   AND COALESCE(rules->>'channel', 'ONLINE') = ?
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
        }, channel);
        return rules;
    }
}
