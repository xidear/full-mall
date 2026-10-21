package com.fullmall.setting.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.setting.infra.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class SystemSettingService {

    private static final Set<String> PUBLIC_KEYS = Set.of(
            "site_name", "company_name", "icp_number", "public_security_record",
            "service_phone", "service_email", "site_status", "maintenance_message",
            "default_locale", "default_currency");

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "site_name", "company_name", "icp_number", "public_security_record",
            "service_phone", "service_email", "site_status", "maintenance_message",
            "default_locale", "default_currency",
            "storage_provider", "sms_provider", "payment_provider", "logistics_provider",
            // 第三方服务的凭据（后台填写，读写时脱敏；演示模式不生效）
            "sms_access_key", "sms_secret_key", "sms_sign_name",
            "logistics_key", "logistics_customer", "map_provider", "map_key",
            // 分销：比例/等级/冻结/提现全部可配，保存即生效
            "distribution.level1-mode", "distribution.level1-value",
            "distribution.level2-enabled", "distribution.level2-mode", "distribution.level2-value",
            "distribution.bind-mode", "distribution.bind-timing",
            "distribution.freeze-policy", "distribution.after-sale-window-days",
            "distribution.min-withdrawal", "distribution.payout-methods",
            "distribution.staff-payout", "distribution.staff-level2-enabled",
            "setup_completed", "member.points.expiry-mode", "member.points.expiry-days",
            "app.timezone");

    /** 敏感配置：读接口必须脱敏，写接口不得把掩码值写回库。 */
    private static final Set<String> SECRET_KEYS = Set.of(
            "sms_access_key", "sms_secret_key", "logistics_key", "map_key");

    /** 掩码占位符：前端原样回传表示"不修改"。 */
    public static final String SECRET_MASK = "******";

    public static final String SITE_STATUS_KEY = "site_status";
    public static final String MAINTENANCE_MESSAGE_KEY = "maintenance_message";
    public static final String DEFAULT_LOCALE_KEY = "default_locale";

    public static final String SITE_STATUS_OPEN = "OPEN";
    public static final String SITE_STATUS_MAINTENANCE = "MAINTENANCE";
    public static final String SITE_STATUS_CLOSED = "CLOSED";

    private static final Set<String> SITE_STATUS_VALUES =
            Set.of(SITE_STATUS_OPEN, SITE_STATUS_MAINTENANCE, SITE_STATUS_CLOSED);

    private final SystemSettingRepository repository;
    private final Clock clock;
    private final Duration ttl;
    private volatile SiteCache siteCache;

    public SystemSettingService(SystemSettingRepository repository,
                                Clock clock,
                                @Value("${app.feature.cache-ttl-seconds:30}") long cacheTtlSeconds) {
        this.repository = repository;
        this.clock = clock;
        if (cacheTtlSeconds <= 0) {
            throw new IllegalArgumentException(
                    "app.feature.cache-ttl-seconds 必须大于 0，实际=" + cacheTtlSeconds);
        }
        this.ttl = Duration.ofSeconds(cacheTtlSeconds);
    }

    /**
     * 站点状态（FLAG-003）。行缺失视为部署错误，直接失败而不是假定 OPEN。
     */
    public String siteStatus() {
        return siteState().status();
    }

    public String maintenanceMessage() {
        return siteState().maintenanceMessage();
    }

    /**
     * 系统默认语言（I18N-001）：媒体与文案回退的唯一来源，当前 `zh-CN`，出海改配置即可。
     */
    public String defaultLocale() {
        return siteState().defaultLocale();
    }

    public Map<String, String> publicSettings() {
        Map<String, String> all = repository.findAll();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : all.entrySet()) {
            if (PUBLIC_KEYS.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public Map<String, String> adminSettings() {
        Map<String, String> values = new java.util.LinkedHashMap<>(repository.findAll());
        for (String key : SECRET_KEYS) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) {
                values.put(key, SECRET_MASK);   // 已配置只回显掩码，不回传明文
            }
        }
        return values;
    }

    /** 内部使用：取真实值（**不要**暴露给接口输出）。 */
    public String rawValue(String key) {
        if (!ALLOWED_KEYS.contains(key)) {
            throw ApiException.badRequest("INVALID_SETTING_KEY", "不支持的配置项: " + key);
        }
        return repository.findAll().getOrDefault(key, "");
    }

    @Transactional
    public Map<String, String> save(Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!ALLOWED_KEYS.contains(entry.getKey())) {
                throw ApiException.badRequest("INVALID_SETTING_KEY", "不支持的配置项: " + entry.getKey());
            }
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (SECRET_KEYS.contains(entry.getKey()) && SECRET_MASK.equals(value)) {
                continue;   // 前端未修改密钥：保留库中原值
            }
            if (DEFAULT_LOCALE_KEY.equals(entry.getKey())
                    && !value.matches("^[a-z]{2,3}(-[A-Za-z0-9]{2,8})?$")) {
                throw ApiException.badRequest("INVALID_DEFAULT_LOCALE",
                        "default_locale 格式应为 xx 或 xx-YY，实际=" + value);
            }
            if (SITE_STATUS_KEY.equals(entry.getKey()) && !SITE_STATUS_VALUES.contains(value)) {
                throw ApiException.badRequest("INVALID_SITE_STATUS",
                        "site_status 只能是 OPEN / MAINTENANCE / CLOSED，实际=" + value);
            }
            repository.upsert(entry.getKey(), value);
        }
        siteCache = null;
        return adminSettings();
    }

    private SiteState siteState() {
        SiteCache cached = siteCache;
        Instant now = clock.instant();
        if (cached != null && !now.isAfter(cached.loadedAt().plus(ttl))) {
            return cached.state();
        }
        synchronized (this) {
            cached = siteCache;
            now = clock.instant();
            if (cached != null && !now.isAfter(cached.loadedAt().plus(ttl))) {
                return cached.state();
            }
            Map<String, String> all = repository.findAll();
            String status = all.get(SITE_STATUS_KEY);
            if (status == null || status.isBlank()) {
                throw new IllegalStateException(
                        "system_setting 缺少 " + SITE_STATUS_KEY + "；V7 迁移应已插入默认值，请检查数据库");
            }
            if (!SITE_STATUS_VALUES.contains(status)) {
                throw new IllegalStateException(
                        "system_setting." + SITE_STATUS_KEY + " 取值非法: " + status);
            }
            String message = all.getOrDefault(MAINTENANCE_MESSAGE_KEY, "");
            String locale = all.get(DEFAULT_LOCALE_KEY);
            if (locale == null || locale.isBlank()) {
                throw new IllegalStateException(
                        "system_setting 缺少 " + DEFAULT_LOCALE_KEY + "；V7 迁移应已插入默认值");
            }
            SiteState state = new SiteState(status, message, locale.trim());
            siteCache = new SiteCache(state, now);
            return state;
        }
    }

    private record SiteState(String status, String maintenanceMessage, String defaultLocale) {
    }

    private record SiteCache(SiteState state, Instant loadedAt) {
    }
}
