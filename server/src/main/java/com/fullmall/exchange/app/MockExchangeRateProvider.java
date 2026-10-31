package com.fullmall.exchange.app;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 演示用固定汇率（不联网、不需要 Key）。
 *
 * <p>仅在 `app.mode=demo` 或显式选择 mock 供应商时使用；生产环境不会自动启用。
 * 以 CNY 为基准：1 CNY = rate 目标币种。
 */
public class MockExchangeRateProvider implements ExchangeRateProvider {

    private static final Map<String, BigDecimal> CNY_RATES = Map.of(
            "USD", new BigDecimal("0.1389"),
            "EUR", new BigDecimal("0.1280"),
            "JPY", new BigDecimal("21.5000"),
            "HKD", new BigDecimal("1.0800"),
            "KWD", new BigDecimal("0.0427"),
            "SGD", new BigDecimal("0.1880"));

    @Override
    public String code() {
        return "mock";
    }

    @Override
    public Map<String, BigDecimal> rates(String baseCurrency) {
        if (!"CNY".equalsIgnoreCase(baseCurrency)) {
            throw new IllegalArgumentException("mock 汇率只支持 CNY 基准，实际=" + baseCurrency);
        }
        return new LinkedHashMap<>(CNY_RATES);
    }
}