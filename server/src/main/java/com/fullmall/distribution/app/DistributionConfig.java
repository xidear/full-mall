package com.fullmall.distribution.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.setting.app.SystemSettingService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 分销配置：**比例、等级、冻结策略、最低提现、打款方式全部来自后台设置**。
 *
 * <p>每次读取都重新查设置表（不缓存），因此**保存后立即生效**——这是明确要求。
 *
 * <p>价值单位：`rate-value` 对 `GROSS` 是百分比（如 5 = 5%），对 `FIXED` 是最小货币单位（如 500 = 5.00 元）。
 */
@Component
public class DistributionConfig {

    public enum RateMode { GROSS, FIXED }

    public record LevelRate(int level, RateMode mode, long value) {
    }

    public record Snapshot(LevelRate level1, LevelRate level2, boolean level2Enabled,
                           long minWithdrawal, List<String> payoutMethods, String freezePolicy,
                           int freezeDays) {
    }

    private final SystemSettingService settings;

    public DistributionConfig(SystemSettingService settings) {
        this.settings = settings;
    }

    public Snapshot load() {
        boolean level2Enabled = bool("distribution.level2-enabled", true);
        LevelRate l1 = readRate(1, "distribution.level1-mode", "distribution.level1-value");
        LevelRate l2 = level2Enabled
                ? readRate(2, "distribution.level2-mode", "distribution.level2-value")
                : new LevelRate(2, RateMode.GROSS, 0);

        long minWithdrawal = positiveLong("distribution.min-withdrawal", 10_000);
        List<String> methods = settings.rawValue("distribution.payout-methods").isBlank()
                ? List.of("ALIPAY", "BANK")
                : java.util.Arrays.stream(settings.rawValue("distribution.payout-methods").split(","))
                    .map(String::trim).filter(s -> !s.isBlank())
                    .map(s -> s.toUpperCase(Locale.ROOT)).toList();
        if (methods.isEmpty()) {
            throw ApiException.conflict("DISTRIBUTION_NO_PAYOUT_METHOD",
                    "未配置任何打款方式（distribution.payout-methods）");
        }

        String freezePolicy = settings.rawValue("distribution.freeze-policy");
        freezePolicy = freezePolicy == null || freezePolicy.isBlank()
                ? "AFTER_SALE_CLOSED" : freezePolicy.trim().toUpperCase(Locale.ROOT);
        if (!List.of("AFTER_SALE_CLOSED", "FIXED_DAYS").contains(freezePolicy)) {
            throw ApiException.conflict("DISTRIBUTION_INVALID_FREEZE_POLICY",
                    "冻结策略只能是 AFTER_SALE_CLOSED 或 FIXED_DAYS，实际=" + freezePolicy);
        }
        int freezeDays = (int) positiveLong("distribution.after-sale-window-days", 7);

        return new Snapshot(l1, l2, level2Enabled, minWithdrawal, methods, freezePolicy, freezeDays);
    }

    private LevelRate readRate(int level, String modeKey, String valueKey) {
        String modeRaw = settings.rawValue(modeKey);
        String normalized = modeRaw == null || modeRaw.isBlank() ? "GROSS" : modeRaw.trim().toUpperCase(Locale.ROOT);
        RateMode mode;
        try {
            mode = RateMode.valueOf(normalized);
        } catch (IllegalArgumentException error) {
            throw ApiException.conflict("DISTRIBUTION_INVALID_RATE_MODE",
                    modeKey + " 只能是 GROSS 或 FIXED，实际=" + modeRaw);
        }
        long value = positiveLong(valueKey, mode == RateMode.GROSS ? 5 : 500);
        if (mode == RateMode.GROSS && (value <= 0 || value > 100)) {
            throw ApiException.conflict("DISTRIBUTION_INVALID_RATE",
                    valueKey + " 作为百分比必须在 1..100，实际=" + value);
        }
        return new LevelRate(level, mode, value);
    }

    private long positiveLong(String key, long fallback) {
        String raw = settings.rawValue(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        long value;
        try {
            value = Long.parseLong(raw.trim());
        } catch (NumberFormatException error) {
            throw ApiException.conflict("DISTRIBUTION_INVALID_NUMBER", key + " 必须是整数，实际=" + raw);
        }
        if (value < 0) {
            throw ApiException.conflict("DISTRIBUTION_INVALID_NUMBER", key + " 不能为负，实际=" + value);
        }
        return value;
    }

    private boolean bool(String key, boolean fallback) {
        String raw = settings.rawValue(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return "true".equalsIgnoreCase(raw.trim());
    }

    /** 按配置计算佣金（四舍五入到分；GROSS 按百分比）。 */
    public static long commission(Snapshot config, int level, long orderAmount) {
        if (orderAmount <= 0) {
            throw new IllegalArgumentException("订单金额必须大于 0，实际=" + orderAmount);
        }
        LevelRate rate = level == 1 ? config.level1() : config.level2();
        return switch (rate.mode()) {
            case GROSS -> (orderAmount * rate.value() + 50) / 100;
            case FIXED -> rate.value();
        };
    }
}
