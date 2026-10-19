package com.fullmall.supplier.app;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementCalculatorTest {

    @Test
    void withoutCommissionPayableIsGross() {
        var amounts = SettlementCalculator.calculate(100_00, 0, BigDecimal.ZERO);

        assertThat(amounts.payableAmount()).isEqualTo(10_000);
        assertThat(amounts.commissionAmount()).isZero();
    }

    @Test
    void commissionRoundsHalfUpOnSmallestUnit() {
        // 3333 分 × 5% = 166.65 → 167
        var amounts = SettlementCalculator.calculate(3333, 0, new BigDecimal("0.05"));

        assertThat(amounts.commissionAmount()).isEqualTo(167);
        assertThat(amounts.payableAmount()).isEqualTo(3166);
    }

    @Test
    void refundReducesPayable() {
        var amounts = SettlementCalculator.calculate(10_000, 2_500, new BigDecimal("0.10"));

        assertThat(amounts.commissionAmount()).isEqualTo(1_000);
        assertThat(amounts.payableAmount()).isEqualTo(6_500);
    }

    @Test
    void rejectsImpossibleInputs() {
        assertThatThrownBy(() -> SettlementCalculator.calculate(-1, 0, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("毛额");
        assertThatThrownBy(() -> SettlementCalculator.calculate(100, 200, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("退款金额不能大于毛额");
        assertThatThrownBy(() -> SettlementCalculator.calculate(100, 0, new BigDecimal("1.5")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("0..1");
        assertThatThrownBy(() -> SettlementCalculator.calculate(100, 0, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("佣金费率");
    }

    @Test
    void payableCannotGoNegative() {
        assertThatThrownBy(() -> SettlementCalculator.calculate(100, 90, new BigDecimal("0.5")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("应付金额为负");
    }
}