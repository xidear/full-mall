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
    private final PromotionStackingPolicy.Mode stackingMode;

    public PromotionPreviewService(JdbcTemplate jdbcTemplate, CatalogService catalogService,
                                   @org.springframework.beans.factory.annotation.Value(
                                           "${app.promotion.stacking-mode:BEST_ONLY}")
                                   String stackingMode) {
        this.jdbcTemplate = jdbcTemplate;
        this.catalogService = catalogService;
        this.stackingMode = PromotionStackingPolicy.Mode.valueOf(
                stackingMode == null ? "BEST_ONLY" : stackingMode.trim().toUpperCase(java.util.Locale.ROOT));
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
        List<PromotionEngine.ScopedRule> rules = scopedRules(channel, subtotalsBySku);
        if (stackingMode == PromotionStackingPolicy.Mode.STACK) {
            // 叠加模式：逐条算优惠，按优先级与互斥组叠加
            List<PromotionStackingPolicy.Candidate> candidates = new ArrayList<>();
            for (PromotionEngine.ScopedRule scoped : rules) {
                long discount = PromotionEngine.discountOf(scoped.rule(), scoped.eligibleSubtotal());
                if (discount > 0) {
                    candidates.add(new PromotionStackingPolicy.Candidate(
                            scoped.rule().code(), discount, scoped.rule().priority(), null));
                }
            }
            var applied = PromotionStackingPolicy.apply(stackingMode, candidates);
            String codes = applied.applied().isEmpty() ? null
                    : applied.applied().stream().map(PromotionStackingPolicy.Candidate::code)
                            .collect(java.util.stream.Collectors.joining("+"));
            long discount = Math.min(applied.totalDiscount(), subtotal);
            return new Preview(subtotal, discount, subtotal - discount, codes, resolvedLocale);
        }
        var result = PromotionEngine.chooseScoped(rules);
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
        java.util.Map<String, RuleScope> scopes = loadScopes();
        java.util.Map<UUID, SkuAttributes> attributesOfSku = attributesOfSkus(subtotalsBySku.keySet());

        List<PromotionEngine.ScopedRule> result = new ArrayList<>();
        for (PromotionEngine.Rule rule : loadActiveRules(channel)) {
            RuleScope scope = scopes.getOrDefault(rule.code(), new RuleScope("ALL", java.util.Set.of()));
            // 只写了 scopeIds 未声明 scope 时按 SKU 处理（管理员更可能漏填类型而不是漏填 ID）
            String scopeType = scope.ids().isEmpty() ? "ALL"
                    : ("ALL".equals(scope.scope()) ? "SKU" : scope.scope());
            long base = switch (scopeType) {
                case "SKU" -> sumMatching(subtotalsBySku, skuId -> scope.ids().contains(skuId.toString()));
                case "CATEGORY" -> sumMatching(subtotalsBySku, skuId -> {
                    SkuAttributes attrs = attributesOfSku.get(skuId);
                    return attrs != null && attrs.categoryId() != null
                            && scope.ids().contains(attrs.categoryId().toString());
                });
                case "BRAND" -> sumMatching(subtotalsBySku, skuId -> {
                    SkuAttributes attrs = attributesOfSku.get(skuId);
                    return attrs != null && attrs.brandId() != null
                            && scope.ids().contains(attrs.brandId().toString());
                });
                default -> total;
            };
            result.add(new PromotionEngine.ScopedRule(rule, base));
        }
        return result;
    }

    private long sumMatching(java.util.Map<UUID, Long> subtotalsBySku,
                             java.util.function.Predicate<UUID> matcher) {
        return subtotalsBySku.entrySet().stream()
                .filter(entry -> matcher.test(entry.getKey()))
                .mapToLong(java.util.Map.Entry::getValue)
                .sum();
    }

    /** SKU 的类目与品牌（一次批量查询）。 */
    private record SkuAttributes(UUID categoryId, UUID brandId) {
    }

    private java.util.Map<UUID, SkuAttributes> attributesOfSkus(java.util.Set<UUID> skuIds) {
        if (skuIds.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<UUID, SkuAttributes> result = new java.util.HashMap<>();
        jdbcTemplate.query("""
                SELECT s.id AS sku_id, p.category_id, p.brand_id
                  FROM sku s JOIN product p ON p.id = s.product_id
                 WHERE s.id = ANY(?)
                """, rs -> {
            result.put(rs.getObject("sku_id", UUID.class), new SkuAttributes(
                    rs.getObject("category_id", UUID.class), rs.getObject("brand_id", UUID.class)));
        }, (Object) skuIds.toArray(new UUID[0]));
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

    private record RuleScope(String scope, java.util.Set<String> ids) {
    }

    /** 一次查询拿到所有生效规则的范围配置（避免每条规则一次查询）。 */
    private java.util.Map<String, RuleScope> loadScopes() {
        java.util.Map<String, RuleScope> scopes = new java.util.HashMap<>();
        jdbcTemplate.query("""
                SELECT code, COALESCE(rules->>'scope', 'ALL') AS scope,
                       COALESCE(rules->'scopeIds', '[]'::jsonb) AS ids
                  FROM promotion
                 WHERE status = 'ACTIVE' AND type IN ('FULL_REDUCTION', 'FULL_DISCOUNT')
                """, rs -> {
            String idsJson = rs.getString("ids");
            java.util.Set<String> ids = new java.util.HashSet<>();
            try {
                com.fasterxml.jackson.databind.JsonNode node =
                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(idsJson);
                node.forEach(element -> ids.add(element.asText()));
            } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
                throw new IllegalStateException("促销范围解析失败: " + rs.getString("code"), error);
            }
            scopes.put(rs.getString("code"), new RuleScope(rs.getString("scope").toUpperCase(), ids));
        });
        return scopes;
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
                SELECT code, type, priority, threshold_amount, rule_value
                  FROM promotion
                 WHERE status = 'ACTIVE'
                   AND type IN ('FULL_REDUCTION', 'FULL_DISCOUNT')
                   AND starts_at <= now() AND ends_at >= now()
                   AND COALESCE(channel, 'ONLINE') = ?
                """, rs -> {
            long threshold = rs.getLong("threshold_amount");
            if (rs.wasNull()) {
                // 不能静默跳过：门槛缺失说明数据未迁移或写入不完整，必须暴露
                throw new IllegalStateException(
                        "促销规则缺少 threshold_amount（code=" + rs.getString("code")
                                + "），请检查迁移 V102 回填或创建逻辑");
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
