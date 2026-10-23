package com.fullmall.platform.money;

import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * Money is always stored in the smallest currency unit (for CNY: fen).
 * Floating point amounts are forbidden in the commerce domain.
 */
public record Money(long amount, String currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be an ISO-4217 3-letter code: " + currency);
        }
        currency = currency.toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Currency must be a valid ISO-4217 code: " + currency, exception);
        }
    }

    public static Money of(long amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money zero(String currency) {
        return new Money(0, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(amount, other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(amount, other.amount), currency);
    }

    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must not be negative: " + quantity);
        }
        return new Money(Math.multiplyExact(amount, quantity), currency);
    }

    public void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    /**
     * 分 → 元的**精确**文本（两位小数），用于对接渠道报文。
     *
     * <p>禁止用 `fen / 100.0`：double 在 0.1/0.01 上不可精确表示，
     * 大额或连续四舍五入会产生一分钱误差（支付报文里这是事故）。
     */
    public static String toYuan(long fen) {
        return java.math.BigDecimal.valueOf(fen)
                .movePointLeft(2)
                .setScale(2, java.math.RoundingMode.UNNECESSARY)
                .toPlainString();
    }

    /** 元（字符串）→ 分，精确到 0.01；多余小数位直接报错而不是静默截断。 */
    public static long parseYuan(String yuan) {
        return new java.math.BigDecimal(yuan.trim())
                .movePointRight(2)
                .setScale(0, java.math.RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    /**
     * 按比例分摊并**精确**取整：`amount * numerator / denominator`，四舍五入到分。
     *
     * <p>用于退款按行分摊、佣金按退款比例扣回等场景；double 实现会出现 ±1 分偏差。
     */
    public static long mulDiv(long amount, long numerator, long denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("分摊分母不能为 0");
        }
        return java.math.BigDecimal.valueOf(amount)
                .multiply(java.math.BigDecimal.valueOf(numerator))
                .divide(java.math.BigDecimal.valueOf(denominator), 0, java.math.RoundingMode.HALF_UP)
                .longValueExact();
    }
}