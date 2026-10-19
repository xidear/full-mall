package com.fullmall.platform.i18n;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 内容语言回退（I18N-001）。
 *
 * <p>**只有两层，没有占位概念**：
 * <pre>
 * 本地语言内容 → 英文默认内容
 * </pre>
 *
 * <p>规则来源（用户 2026-10-19）：默认图永远是英文；本地语言决定显示哪张图；
 * 没有对应语言的图就显示英文默认图。当前展示中文图，是因为所在市场是中文——
 * 中文对我们是"本地语言"，不是默认语言。
 *
 * <p>英文默认内容缺失属于**数据完整性问题**：读取时直接失败、发布前必须校验，
 * 不返回占位图。
 */
public final class LocaleFallback {

    /** 默认内容语言：固定英文，不随站点配置改变。 */
    public static final String FALLBACK_LOCALE = "en";

    private static final Pattern LOCALE = Pattern.compile("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})?$");

    private LocaleFallback() {
    }

    public enum Source {
        /** 命中所请求语言的本地化内容。 */
        LOCALIZED,
        /** 回退到英文默认内容。 */
        DEFAULT
    }

    public record Resolution(String locale, Source source) {
    }

    /**
     * 决定前端应请求哪种语言：用户语言优先，未知时用站点主语言（当前 zh-CN，可后台配置）。
     */
    public static String preferred(String requested, String siteDefault) {
        String normalizedRequested = normalize(requested);
        if (normalizedRequested != null) {
            return normalizedRequested;
        }
        String normalizedSiteDefault = normalize(siteDefault);
        if (normalizedSiteDefault == null) {
            throw new IllegalArgumentException("站点主语言未配置或格式非法: " + siteDefault);
        }
        return normalizedSiteDefault;
    }

    /**
     * 命中判定：本地语言有内容就用本地；否则必须存在英文默认内容。
     *
     * @throws IllegalStateException 英文默认内容缺失（发布前校验应已阻止）
     */
    public static Resolution resolve(String wantedLocale, boolean hasLocalizedContent,
                                     boolean hasDefaultContent) {
        String wanted = normalize(wantedLocale);
        if (wanted == null) {
            throw new IllegalArgumentException("待查语言为空或格式非法: " + wantedLocale);
        }
        if (hasLocalizedContent) {
            return new Resolution(wanted,
                    wanted.equals(FALLBACK_LOCALE) ? Source.DEFAULT : Source.LOCALIZED);
        }
        if (hasDefaultContent) {
            return new Resolution(FALLBACK_LOCALE, Source.DEFAULT);
        }
        throw new IllegalStateException(
                "内容缺少英文默认版本（locale=" + wanted + "），属于数据完整性问题；"
                        + "发布前必须保证存在 " + FALLBACK_LOCALE + " 版本");
    }

    /**
     * 发布前校验：没有英文默认内容不允许发布。
     */
    public static void requirePublishable(boolean hasDefaultContent, String resource) {
        if (!hasDefaultContent) {
            throw new IllegalStateException("资源缺少英文默认内容，不能发布: " + resource);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (!LOCALE.matcher(trimmed).matches()) {
            return null;
        }
        String[] parts = trimmed.split("-");
        return parts.length == 2
                ? parts[0].toLowerCase(Locale.ROOT) + "-" + parts[1].toUpperCase(Locale.ROOT)
                : parts[0].toLowerCase(Locale.ROOT);
    }
}