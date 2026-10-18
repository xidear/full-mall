package com.fullmall.platform.money;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void addsAndMultipliesWithoutFloatingPoint() {
        Money price = Money.of(1990, "cny");
        assertEquals(Money.of(3980, "CNY"), price.multiply(2));
        assertEquals(Money.of(4480, "CNY"), price.multiply(2).plus(Money.of(500, "CNY")));
    }

    @Test
    void rejectsCurrencyMismatch() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(100, "CNY").plus(Money.of(100, "HKD")));
    }

    @Test
    void rejectsInvalidCurrencyCode() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(100, "RMB"));
    }
}