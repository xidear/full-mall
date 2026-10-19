package com.fullmall.supplier.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 结算金额计算（SUP-020），纯函数，便于对账与单测。
 *
 * <p>口径（当前阶段）：
 * <ul>
 *   <li>应付 = 已支付金额 − 退款 − 佣金/服务费；</li>
 *   <li>佣金费率来自配置，**未配置时为 0**，不臆造费率；</li>
 *   <li>金额一律最小货币单位整数（分），四舍五入用 {@link RoundingMode#HALF_UP}。</li>
 * </ul>
 *
 * <p>已知缺口：退款聚合与退货冲减尚未接入（退款金额传 0），
 * 待售后/退货对账完成后替换入参，公式不变。
 */
public final class SettlementCalculator {

    private SettlementCalculator() {
    }

    public record SettlementAmounts(long grossAmount, long refundAmount, long commissionAmount,
                                    long payableAmount) {
    }

    public static SettlementAmounts calculate(long grossAmount, long refundAmount,
                                              BigDecimal commissionRate) {
        if (grossAmount < 0) {
            throw new IllegalArgumentException("结算毛额不能为负: " + grossAmount);
        }
        if (refundAmount < 0) {
            throw new IllegalArgumentException("退款金额不能为负: " + refundAmount);
        }
        if (refundAmount > grossAmount) {
            throw new IllegalArgumentException(
                    "退款金额不能大于毛额: refund=" + refundAmount + ", gross=" + grossAmount);
        }
        if (commissionRate == null || commissionRate.signum() < 0
                || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("佣金费率必须在 0..1 之间，实际=" + commissionRate);
        }

        long commission = BigDecimal.valueOf(grossAmount)
                .multiply(commissionRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        long payable = grossAmount - refundAmount - commission;
        if (payable < 0) {
            throw new IllegalStateException(
                    "应付金额为负，结算口径需人工复核: gross=" + grossAmount
                            + ", refund=" + refundAmount + ", commission=" + commission);
        }
        return new SettlementAmounts(grossAmount, refundAmount, commission, payable);
    }
}