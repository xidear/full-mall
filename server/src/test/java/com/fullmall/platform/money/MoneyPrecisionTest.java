package com.fullmall.platform.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 金额精度：分↔元必须精确，禁止 double 参与；分摊四舍五入到分。 */
class MoneyPrecisionTest {

    @Test
    void yuanFormattingIsExactForLargeAndAwkwardValues() {
        assertThat(Money.toYuan(0)).isEqualTo("0.00");
        assertThat(Money.toYuan(1)).isEqualTo("0.01");
        assertThat(Money.toYuan(1990)).isEqualTo("19.90");
        // double 会算成 1234567890123.45 附近的误差值，这里必须精确
        assertThat(Money.toYuan(123456789012345L)).isEqualTo("1234567890123.45");
        assertThat(Money.parseYuan("1234567890123.45")).isEqualTo(123456789012345L);
    }

    @Test
    void parseRejectsSubFenAmounts() {
        assertThatThrownBy(() -> Money.parseYuan("1.005"))
                .isInstanceOf(ArithmeticException.class);
        assertThat(Money.toYuan(1L)).isEqualTo("0.01");
    }

    @Test
    void majorFormattingFollowsCurrencyDigits() {
        assertThat(Money.toMajor(1234, "JPY")).isEqualTo("1234");
        assertThat(Money.toMajor(1234, "USD")).isEqualTo("12.34");
        assertThat(Money.toMajor(1234, "KWD")).isEqualTo("1.234");
        assertThat(Money.parseMajor("1234", "JPY")).isEqualTo(1234);
        assertThat(Money.parseMajor("1.234", "KWD")).isEqualTo(1234);
        // USD 只有 2 位，多给一位必须报错而不是静默截断
        assertThatThrownBy(() -> Money.parseMajor("1.234", "USD"))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void currencyPrecisionFollowsIsoMinorUnits() {
        assertThat(CurrencyPrecision.minorUnits("JPY")).isZero();
        assertThat(CurrencyPrecision.minorUnits("KWD")).isEqualTo(3);
        // TWD/HUF 是 2 位；旧实现误写成 0，会让金额放大 100 倍
        assertThat(CurrencyPrecision.minorUnits("TWD")).isEqualTo(2);
        assertThat(CurrencyPrecision.minorUnits("HUF")).isEqualTo(2);
        assertThatThrownBy(() -> CurrencyPrecision.minorUnits("CNY1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mulDivRoundsHalfUpWithoutFloatingPointDrift() {
        // 1990 * 1 / 3 = 663.33 → 663；1990*2/3 = 1326.67 → 1327
        assertThat(Money.mulDiv(1990, 1, 3)).isEqualTo(663);
        assertThat(Money.mulDiv(1990, 2, 3)).isEqualTo(1327);
        // 大数：(10^15+1)/3 用 double 会丢精度
        assertThat(Money.mulDiv(1_000_000_000_000_001L, 1, 3))
                .isEqualTo(new BigDecimal("1000000000000001")
                        .divide(BigDecimal.valueOf(3), 0, java.math.RoundingMode.HALF_UP)
                        .longValueExact());
        assertThatThrownBy(() -> Money.mulDiv(1, 1, 0))
                .hasMessageContaining("分母不能为 0");
    }
}
