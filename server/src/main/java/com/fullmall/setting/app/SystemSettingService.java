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
            "storage_provider", "sms_provider", "payment_provider", "logistics_provider");

    public static final String SITE_STATUS_KEY = "site_status";
    public static final String MAINTENANCE_MESSAGE_KEY = "maintenance_message";

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
        return repository.findAll();
    }

    @Transactional
    public Map<String, String> save(Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!ALLOWED_KEYS.contains(entry.getKey())) {
                throw ApiException.badRequest("INVALID_SETTING_KEY", "不支持的配置项: " + entry.getKey());
            }
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
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
            SiteState state = new SiteState(status, message);
            siteCache = new SiteCache(state, now);
            return state;
        }
    }

    private record SiteState(String status, String maintenanceMessage) {
    }

    private record SiteCache(SiteState state, Instant loadedAt) {
    }
}
