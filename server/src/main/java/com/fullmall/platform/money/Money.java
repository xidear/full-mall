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
     * 最小单位整数 → 主单位**精确**文本（小数位由币种决定）。
     *
     * <p>禁止用 `minor / 100.0`：double 不能精确表示 0.1/0.01；日元 0 位、第纳尔 3 位，
     * 写死两位还会直接差 100 倍。
     */
    public static String toMajor(long amountMinor, String currency) {
        int units = CurrencyPrecision.minorUnits(currency);
        return java.math.BigDecimal.valueOf(amountMinor)
                .movePointLeft(units)
                .setScale(units, java.math.RoundingMode.UNNECESSARY)
                .toPlainString();
    }

    /** 主单位字符串 → 最小单位整数；超出该币种精度直接报错，不静默截断。 */
    public static long parseMajor(String amountMajor, String currency) {
        int units = CurrencyPrecision.minorUnits(currency);
        return new java.math.BigDecimal(amountMajor.trim())
                .movePointRight(units)
                .setScale(0, java.math.RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    /** 兼容旧调用（支付宝/微信 CNY 报文）：等价于 {@code toMajor(fen, "CNY")}。 */
    public static String toYuan(long fen) {
        return toMajor(fen, "CNY");
    }

    /** 兼容旧调用：等价于 {@code parseMajor(yuan, "CNY")}。 */
    public static long parseYuan(String yuan) {
        return parseMajor(yuan, "CNY");
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