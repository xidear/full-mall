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

    public static Result choose(List<Rule> rules, long subtotalAmount) {
        if (subtotalAmount < 0) {
            throw new IllegalArgumentException("小计不能为负: " + subtotalAmount);
        }
        if (rules == null || rules.isEmpty()) {
            return new Result(null, 0, "无可用促销");
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

    /** 供上层排序展示用：按优先级降序、编码升序，保证稳定。 */
    public static Comparator<Rule> displayOrder() {
        return Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::code);
    }
}
