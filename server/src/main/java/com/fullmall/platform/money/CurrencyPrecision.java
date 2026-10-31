package com.fullmall.platform.money;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 币种最小单位（小数位）的单一来源。
 *
 * <p>为什么必须有：金额在库里是最小单位的整数（bigint），前端换算写死 ×100 / ÷100。
 * 一碰到日元（0 位小数）就整体放大 100 倍，碰到科威特第纳尔（3 位）又少一位——
 * 这类错误在运费、结算、发票上都是资金事故。
 *
 * <p>默认 2 位；下面只列例外。
 */
public final class CurrencyPrecision {

    private static final int DEFAULT_MINOR_UNITS = 2;

    private static final Map<String, Integer> OVERRIDES = new LinkedHashMap<>();

    static {
        // 0 位小数
        for (String code : new String[]{"JPY", "KRW", "VND", "CLP", "ISK"}) {
            OVERRIDES.put(code, 0);
        }
        // 3 位小数
        for (String code : new String[]{"KWD", "BHD", "JOD", "OMR", "TND", "IQD", "LYD"}) {
            OVERRIDES.put(code, 3);
        }
    }

    private CurrencyPrecision() {
    }

    public static int minorUnits(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException("币种不能为空");
        }
        String code = currencyCode.trim().toUpperCase(Locale.ROOT);
        Integer override = OVERRIDES.get(code);
        if (override != null) {
            return override;
        }
        // 以 JDK 的 ISO-4217 数据为默认（TWD/HUF 等易被手写错的币种也以它为准）
        try {
            int digits = java.util.Currency.getInstance(code).getDefaultFractionDigits();
            return digits < 0 ? DEFAULT_MINOR_UNITS : digits;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("不是有效的 ISO-4217 币种：" + currencyCode, exception);
        }
    }

    /** 供前端取用：一次拿全，避免逐个币种发请求。 */
    public static Map<String, Object> view() {
        return Map.of("defaultMinorUnits", DEFAULT_MINOR_UNITS, "overrides", Map.copyOf(OVERRIDES));
    }
}
