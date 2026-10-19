package com.fullmall.platform.i18n;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 内容语言回退（I18N-001）。
 *
 * <p>规则只有一条：**请求语言 → 系统默认语言 → 占位**。
 * 默认语言来自配置（`system_setting.default_locale`），当前为 `zh-CN`；
 * 出海时只改配置，代码不动——**这里不写死英文，也不对任何语言做特殊处理**。
 *
 * <p>同时负责标记内容来源，避免把默认语言内容伪装成"已本地化"。
 */
public final class LocaleFallback {

    private static final Pattern LOCALE = Pattern.compile("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})?$");

    private LocaleFallback() {
    }

    public enum Source {
        /** 命中所请求语言的本地化内容。 */
        SELF,
        /** 回退到系统默认语言的内容。 */
        DEFAULT,
        /** 连默认语言都没有，前端显示占位。 */
        PLACEHOLDER
    }

    public record Resolution(String locale, Source source) {
    }

    /**
     * @param requested     客户端请求语言，可为空
     * @param defaultLocale 系统默认语言（必填，来自配置）
     */
    public static String preferred(String requested, String defaultLocale) {
        String fallback = normalize(defaultLocale);
        if (fallback == null) {
            throw new IllegalArgumentException("系统默认语言未配置或格式非法: " + defaultLocale);
        }
        String normalizedRequested = normalize(requested);
        return normalizedRequested == null ? fallback : normalizedRequested;
    }

    /**
     * 查询到内容后确定来源：请求语言有内容 → SELF；否则默认语言有内容 → DEFAULT；否则 PLACEHOLDER。
     */
    public static Resolution resolve(String requested, String defaultLocale,
                                     boolean hasRequestedContent, boolean hasDefaultContent) {
        String wanted = preferred(requested, defaultLocale);
        String fallback = normalize(defaultLocale);

        if (hasRequestedContent || (wanted.equals(fallback) && hasDefaultContent)) {
            return new Resolution(wanted, Source.SELF);
        }
        if (hasDefaultContent) {
            return new Resolution(fallback, Source.DEFAULT);
        }
        return new Resolution(fallback, Source.PLACEHOLDER);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (!LOCALE.matcher(trimmed).matches()) {
            return null;
        }
        if ("zh-cn".equalsIgnoreCase(trimmed)) {
            return "zh-CN";
        }
        String[] parts = trimmed.split("-");
        return parts.length == 2
                ? parts[0].toLowerCase(Locale.ROOT) + "-" + parts[1].toUpperCase(Locale.ROOT)
                : parts[0].toLowerCase(Locale.ROOT);
    }
}