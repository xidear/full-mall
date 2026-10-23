package com.fullmall.catalog.app;

/**
 * 移动加权平均成本计算（纯函数）。
 *
 * <p>规则：
 * <ul>
 *   <li>入库：`newAvg = (期初金额 + 入库金额) / (期初数量 + 入库数量)`，金额累加**不四舍五入**，
 *       均价四舍五入到分；差额留在金额里，避免长期累计偏差；</li>
 *   <li>出库：按当前均价结转，**均价不变**（这是"移动平均"的核心：销售不改变单位成本）；</li>
 *   <li>数量归零：保留最后均价并标记 `stale`，便于"卖完后仍有成本参考"，但不参与后续入库以外的计算。</li>
 * </ul>
 *
 * <p>金额单位统一为分，数量为件；不使用 double 表示金额。
 */
public final class MovingAverageCost {

    private MovingAverageCost() {
    }

    public record State(long quantity, long amount, long avgCost, boolean stale) {
    }

    /** 入库：数量增减 + 金额累加，重算均价。 */
    public static State receive(State current, long receiptQuantity, long receiptUnitCost) {
        if (receiptQuantity <= 0) {
            throw new IllegalArgumentException("入库数量必须大于 0，实际=" + receiptQuantity);
        }
        if (receiptUnitCost < 0) {
            throw new IllegalArgumentException("入库成本不能为负，实际=" + receiptUnitCost);
        }
        long quantity = Math.addExact(current.quantity(), receiptQuantity);
        long amount = Math.addExact(current.amount(),
                Math.multiplyExact(receiptQuantity, receiptUnitCost));
        return new State(quantity, amount, divideRound(amount, quantity), false);
    }

    /** 出库：按均价结转，**均价不变**；数量不足直接报错（不做负数库存）。 */
    public static State issue(State current, long issueQuantity) {
        if (issueQuantity <= 0) {
            throw new IllegalArgumentException("出库数量必须大于 0，实际=" + issueQuantity);
        }
        if (issueQuantity > current.quantity()) {
            throw new IllegalArgumentException(
                    "出库数量超过库存：" + issueQuantity + " > " + current.quantity());
        }
        long quantity = current.quantity() - issueQuantity;
        long amount = Math.max(0, current.amount() - issueQuantity * current.avgCost());
        if (quantity == 0) {
            // 卖完：金额清零，保留均价供参考，标记 stale
            return new State(0, 0, current.avgCost(), true);
        }
        return new State(quantity, amount, current.avgCost(), false);
    }

    /** 仅用于测试与初始状态。 */
    public static State initial() {
        return new State(0, 0, 0, false);
    }

    private static long divideRound(long amount, long quantity) {
        return java.math.BigDecimal.valueOf(amount)
                .divide(java.math.BigDecimal.valueOf(quantity), 0, java.math.RoundingMode.HALF_UP)
                .longValueExact();
    }
}
