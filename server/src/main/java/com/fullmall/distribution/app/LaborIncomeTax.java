package com.fullmall.distribution.app;

/**
 * 劳务报酬个人所得税（预扣预缴，简版，单位：分）。
 *
 * <ul>
 *   <li>≤800 元：不预扣；</li>
 *   <li>800–4000 元：应纳税所得额 = 收入 − 800；</li>
 *   <li>&gt;4000 元：应纳税所得额 = 收入 × 80%；</li>
 *   <li>税额 = 应纳税所得额 × 税率 − 速算扣除数（≤2万 20%/0；2–5万 30%/2000；&gt;5万 40%/7000）。</li>
 * </ul>
 */
public final class LaborIncomeTax {

    private LaborIncomeTax() {
    }

    public record Result(long taxable, long tax, long net) {
    }

    public static Result compute(long grossFen) {
        if (grossFen < 0) {
            throw new IllegalArgumentException("收入不能为负：" + grossFen);
        }
        long taxable;
        if (grossFen <= 80_000L) {
            taxable = 0;
        } else if (grossFen <= 400_000L) {
            taxable = grossFen - 80_000L;
        } else {
            taxable = grossFen * 80 / 100;
        }
        long tax;
        if (taxable <= 2_000_000L) {
            tax = taxable * 20 / 100;
        } else if (taxable <= 5_000_000L) {
            tax = taxable * 30 / 100 - 200_000L;
        } else {
            tax = taxable * 40 / 100 - 700_000L;
        }
        tax = Math.max(tax, 0);
        if (tax > grossFen) {
            tax = grossFen;
        }
        return new Result(taxable, tax, grossFen - tax);
    }
}
