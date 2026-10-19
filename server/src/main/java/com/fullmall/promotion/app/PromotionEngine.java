package com.fullmall.promotion.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * 促销引擎（PRO-001/003 起步）：满减 / 满折，**取最优**。
 *
 * <p>口径：
 * <ul>
 *   <li>门槛与优惠都以"商品小计"为基数，不含运费；</li>
 *   <li>同一订单命中多条规则时**取优惠最大的一条**；优惠相同时按 `priority` 高者优先，
 *       再相同则按规则编码字典序（保证结果稳定、可复现）；</li>
 *   <li>满折按百分比，四舍五入到分；</li>
 *   <li>优惠不超过小计（不允许负应付）。</li>
 * </ul>
 *
 * <p>未实现（明确记录）：叠加/互斥配置、渠道隔离、按类目/品牌限定范围。
 */
public final class PromotionEngine {

    private PromotionEngine() {
    }

    public enum Type {
        FULL_REDUCTION,
        FULL_DISCOUNT
    }

    /**
     * @param thresholdAmount 满减/满折门槛（分）
     * @param value           满减为减免额（分）；满折为折扣百分比（如 90 表示 9 折）
     * @param priority        优先级，越大越优先
     */
    public record Rule(String code, Type type, long thresholdAmount, long value, int priority) {
    }

    public record Result(String appliedRuleCode, long discountAmount, String reason) {
        public boolean applied() {
            return appliedRuleCode != null;
        }
    }

    /**
     * 规则 + 该规则的计费基数（范围限定时，基数只算命中的行）。
     */
    public record ScopedRule(Rule rule, long eligibleSubtotal) {
    }

    /** 全量基数版本：所有规则共用同一基数。 */
    public static Result choose(List<Rule> rules, long subtotalAmount) {
        return chooseScoped(rules.stream()
                .map(rule -> new ScopedRule(rule, subtotalAmount))
                .toList());
    }

    /**
     * 按规则各自的基数挑选最优：门槛与折扣都用 {@code eligibleSubtotal}。
     *
     * <p>范围限定规则（只对部分商品生效）时，不能拿整单小计判断门槛——
     * 否则"某类目满 200 减 20"会被其它类目的金额凑满。
     */
    public static Result chooseScoped(List<ScopedRule> scopedRules) {
        if (scopedRules == null || scopedRules.isEmpty()) {
            return new Result(null, 0, "无可用促销");
        }
        return pickBest(scopedRules);
    }

    private static Result chooseLegacy(List<Rule> rules, long subtotalAmount) {
        if (subtotalAmount < 0) {
            throw new IllegalArgumentException("小计不能为负: " + subtotalAmount);
        }

        Rule best = null;
        long bestDiscount = 0;

        for (Rule rule : rules) {
            if (rule.thresholdAmount() <= 0) {
                throw new IllegalArgumentException("促销门槛必须大于 0: " + rule.code());
            }
            if (subtotalAmount < rule.thresholdAmount()) {
                continue;
            }
            long discount = switch (rule.type()) {
                case FULL_REDUCTION -> {
                    if (rule.value() <= 0) {
                        throw new IllegalArgumentException("满减金额必须大于 0: " + rule.code());
                    }
                    yield Math.min(rule.value(), subtotalAmount);
                }
                case FULL_DISCOUNT -> {
                    if (rule.value() <= 0 || rule.value() >= 100) {
                        throw new IllegalArgumentException("满折百分比必须在 1..99: " + rule.code());
                    }
                    long off = BigDecimal.valueOf(subtotalAmount)
                            .multiply(BigDecimal.valueOf(100 - rule.value()))
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                            .longValueExact();
                    yield Math.min(off, subtotalAmount);
                }
            };
            if (discount <= 0) {
                continue;
            }
            boolean better = best == null
                    || discount > bestDiscount
                    || (discount == bestDiscount && rule.priority() > best.priority())
                    || (discount == bestDiscount && rule.priority() == best.priority()
                        && rule.code().compareTo(best.code()) < 0);
            if (better) {
                best = rule;
                bestDiscount = discount;
            }
        }

        return best == null
                ? new Result(null, 0, "未达到任何促销门槛")
                : new Result(best.code(), bestDiscount, "命中 " + best.type());
    }

    private static Result pickBest(List<ScopedRule> scopedRules) {
        Rule best = null;
        long bestDiscount = 0;
        for (ScopedRule scoped : scopedRules) {
            Rule rule = scoped.rule();
            long base = scoped.eligibleSubtotal();
            if (base < 0) {
                throw new IllegalArgumentException("计费基数不能为负: " + rule.code());
            }
            if (rule.thresholdAmount() <= 0) {
                throw new IllegalArgumentException("促销门槛必须大于 0: " + rule.code());
            }
            if (base < rule.thresholdAmount()) {
                continue;
            }
            long discount = switch (rule.type()) {
                case FULL_REDUCTION -> {
                    if (rule.value() <= 0) {
                        throw new IllegalArgumentException("满减金额必须大于 0: " + rule.code());
                    }
                    yield Math.min(rule.value(), base);
                }
                case FULL_DISCOUNT -> {
                    if (rule.value() <= 0 || rule.value() >= 100) {
                        throw new IllegalArgumentException("满折百分比必须在 1..99: " + rule.code());
                    }
                    long off = BigDecimal.valueOf(base)
                            .multiply(BigDecimal.valueOf(100 - rule.value()))
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                            .longValueExact();
                    yield Math.min(off, base);
                }
            };
            if (discount <= 0) {
                continue;
            }
            boolean better = best == null
                    || discount > bestDiscount
                    || (discount == bestDiscount && rule.priority() > best.priority())
                    || (discount == bestDiscount && rule.priority() == best.priority()
                        && rule.code().compareTo(best.code()) < 0);
            if (better) {
                best = rule;
                bestDiscount = discount;
            }
        }
        return best == null
                ? new Result(null, 0, "未达到任何促销门槛")
                : new Result(best.code(), bestDiscount, "命中 " + best.type());
    }

    /** 供上层排序展示用：按优先级降序、编码升序，保证稳定。 */
    public static Comparator<Rule> displayOrder() {
        return Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::code);
    }
}
