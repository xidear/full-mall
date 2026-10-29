package com.fullmall.setting.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.validation.ValidationRules;
import com.fullmall.setting.infra.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 系统配置的唯一读写入口（app 层）：配置项存在 `system_setting` 表的 setting_key/setting_value 行里，
 * `feature_flag` 是另一张表（按功能与灰度维度的开关），本类不碰。
 *
 * <p>site_status / default_locale 是部署不变量（V7 迁移插入）：缺行时直接抛异常，而不是假定 OPEN、zh-CN；
 * 其余键缺失按空串处理，"空 = 未配置" 由调用方判断。非 OPEN 时顾客前台 /api/v1/** 会被
 * FeatureGuardFilter 拦成 503（错误码 "SITE_" + 状态值），后台等入口不受影响。
 */
@Service
public class SystemSettingService {

    private static final Set<String> PUBLIC_KEYS = Set.of(
            "site_name", "company_name", "icp_number", "public_security_record",
            "service_phone", "service_email", "site_status", "maintenance_message",
            "default_locale", "default_currency", "default_market",
            "site_domain", "site_scheme", "business_license_no",
            "site_tagline", "branding_attribution", "enabled_locales", "app.timezone");

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "site_name", "company_name", "icp_number", "public_security_record",
            "service_phone", "service_email", "site_status", "maintenance_message",
            "default_locale", "default_currency", "default_market",
            "site_domain", "site_scheme", "business_license_no",
            "site_tagline", "branding_attribution", "enabled_locales",
            "media_public_base_url",
            "upload.admin-image-max-bytes", "upload.admin-video-max-bytes",
            "upload.member-image-max-bytes", "upload.member-video-max-bytes",
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
            "onboarding.data-mode",
            "app.timezone");

    /** 敏感配置：读接口必须脱敏，写接口不得把掩码值写回库。 */
    private static final Set<String> SECRET_KEYS = Set.of(
            "sms_access_key", "sms_secret_key", "logistics_key", "map_key");

    /** 掩码占位符：前端原样回传表示"不修改"。 */
    public static final String SECRET_MASK = "******";

    public static final String SITE_STATUS_KEY = "site_status";
    public static final String MAINTENANCE_MESSAGE_KEY = "maintenance_message";
    public static final String DEFAULT_LOCALE_KEY = "default_locale";
    /** 已启用语言（逗号分隔，不含源语言时自动补上前面的默认语言）。 */
    public static final String ENABLED_LOCALES_KEY = "enabled_locales";
    /** 新装默认启用英文与日文：与后台译文的默认目标语言一致。 */
    public static final String DEFAULT_ENABLED_LOCALES = "en,ja";
    private static final String LOCALE_PATTERN = "^[a-z]{2,3}(-[A-Za-z0-9]{2,8})?$";
    public static final String SITE_DOMAIN_KEY = "site_domain";
    public static final String SITE_SCHEME_KEY = "site_scheme";

    public static final String DEFAULT_MARKET_KEY = "default_market";
    /** 首次配置决定的"示例数据"策略：DEMO（安装演示数据） / BLANK（完全空白）。 */
    public static final String ONBOARDING_DATA_MODE_KEY = "onboarding.data-mode";
    public static final String BRANDING_ATTRIBUTION_KEY = "branding_attribution";

    public static final String SITE_STATUS_OPEN = "OPEN";
    public static final String SITE_STATUS_MAINTENANCE = "MAINTENANCE";
    public static final String SITE_STATUS_CLOSED = "CLOSED";

    private static final Set<String> SITE_STATUS_VALUES =
            Set.of(SITE_STATUS_OPEN, SITE_STATUS_MAINTENANCE, SITE_STATUS_CLOSED);

    /** 已开通的市场。多地区共用一套前台，地区政策按 market 生效。 */
    private static final Set<String> MARKET_VALUES = Set.of("CN", "HK", "MO", "TW");

    private static final Set<String> BRANDING_ATTRIBUTION_VALUES = Set.of("SHOW", "HIDDEN");

    private static final Set<String> STORAGE_PROVIDER_VALUES =
            Set.of("local", "mock", "qiniu", "cos", "aliyun-oss", "s3");

    // 素材/评价上传体积上限（字节）。默认值与可配范围镜像 docs/validation.schema.json 的
    // $defs.assetUpload，由 ValidationRulesContractTest 守护；这里只放"键名"和读取逻辑。
    public static final String UPLOAD_ADMIN_IMAGE_MAX_KEY = "upload.admin-image-max-bytes";
    public static final String UPLOAD_ADMIN_VIDEO_MAX_KEY = "upload.admin-video-max-bytes";
    public static final String UPLOAD_MEMBER_IMAGE_MAX_KEY = "upload.member-image-max-bytes";
    public static final String UPLOAD_MEMBER_VIDEO_MAX_KEY = "upload.member-video-max-bytes";

    /** 键 → 后台展示/编辑顺序，adminSettings() 用它补齐未保存过的默认值，避免设置页出现空输入框。 */
    private static final java.util.List<String> UPLOAD_LIMIT_KEYS = java.util.List.of(
            UPLOAD_ADMIN_IMAGE_MAX_KEY, UPLOAD_ADMIN_VIDEO_MAX_KEY,
            UPLOAD_MEMBER_IMAGE_MAX_KEY, UPLOAD_MEMBER_VIDEO_MAX_KEY);

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

    /**
     * 已启用语言（默认语言排第一）。
     *
     * <p>后台译文页、"已启用语言的译文" tab、前台语言切换都以这个列表为唯一来源，
     * 避免前端各自硬编码 `['en','ja']` 导致"配置改了页面没变"。
     * 配置缺失时使用 {@link #DEFAULT_ENABLED_LOCALES}（迁移 V123 已插入），
     * 出现非法语言代码则直接失败，而不是静默丢弃。
     */
    public java.util.List<String> enabledLocales() {
        String raw = repository.findAll().get(ENABLED_LOCALES_KEY);
        String value = raw == null || raw.isBlank() ? DEFAULT_ENABLED_LOCALES : raw;
        java.util.List<String> locales = new java.util.ArrayList<>();
        for (String part : value.split(",")) {
            String locale = part.trim();
            if (locale.isEmpty()) {
                continue;
            }
            if (!locale.matches(LOCALE_PATTERN)) {
                throw new IllegalStateException(
                        "system_setting." + ENABLED_LOCALES_KEY + " 含非法语言代码: " + locale);
            }
            if (!locales.contains(locale)) {
                locales.add(locale);
            }
        }
        String defaultLocale = defaultLocale();
        locales.remove(defaultLocale);
        locales.add(0, defaultLocale);
        return java.util.List.copyOf(locales);
    }

    /** 目标语言（已启用语言里去掉默认语言）：译文编辑的目标集合。 */
    public java.util.List<String> translationLocales() {
        String defaultLocale = defaultLocale();
        return enabledLocales().stream().filter(locale -> !locale.equals(defaultLocale)).toList();
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
        // 上传上限没有对应的迁移行（默认值只存在 schema 里，避免两处数字漂移）：
        // 这里补上生效值，设置页才不会显示空输入框、也不会让人误以为"没配置=不限"。
        for (String key : UPLOAD_LIMIT_KEYS) {
            values.putIfAbsent(key, String.valueOf(uploadDefaultBytes(key)));
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

    /**
     * 管理端图片上传上限（字节）。
     *
     * <p>读库而不是读环境变量：运营在后台改完即对下一次上传生效，不必重启容器。
     * 行缺失/为空时用 schema 默认值；有值但非法（非整数、越界）直接抛 {@link IllegalStateException}，
     * 不静默回落到默认值——否则"后台写错了但看起来还能传"最难查。
     */
    public long adminImageMaxBytes() {
        return uploadLimitBytes(UPLOAD_ADMIN_IMAGE_MAX_KEY, ValidationRules.UPLOAD_ADMIN_IMAGE_DEFAULT_BYTES,
                ValidationRules.UPLOAD_ADMIN_IMAGE_MAX_CONFIGURABLE_BYTES);
    }

    /** 管理端视频上传上限（字节），语义同 {@link #adminImageMaxBytes()}。 */
    public long adminVideoMaxBytes() {
        return uploadLimitBytes(UPLOAD_ADMIN_VIDEO_MAX_KEY, ValidationRules.UPLOAD_ADMIN_VIDEO_DEFAULT_BYTES,
                ValidationRules.UPLOAD_ADMIN_VIDEO_MAX_CONFIGURABLE_BYTES);
    }

    /** 顾客端（评价晒图）图片上传上限（字节），语义同 {@link #adminImageMaxBytes()}。 */
    public long memberImageMaxBytes() {
        return uploadLimitBytes(UPLOAD_MEMBER_IMAGE_MAX_KEY, ValidationRules.UPLOAD_MEMBER_IMAGE_DEFAULT_BYTES,
                ValidationRules.UPLOAD_MEMBER_IMAGE_MAX_CONFIGURABLE_BYTES);
    }

    /** 顾客端（评价）视频上传上限（字节），语义同 {@link #adminImageMaxBytes()}。 */
    public long memberVideoMaxBytes() {
        return uploadLimitBytes(UPLOAD_MEMBER_VIDEO_MAX_KEY, ValidationRules.UPLOAD_MEMBER_VIDEO_DEFAULT_BYTES,
                ValidationRules.UPLOAD_MEMBER_VIDEO_MAX_CONFIGURABLE_BYTES);
    }

    private long uploadLimitBytes(String key, long defaultValue, long maxConfigurable) {
        String raw = repository.findAll().get(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        long value;
        try {
            value = Long.parseLong(raw.trim());
        } catch (NumberFormatException error) {
            throw new IllegalStateException(
                    "system_setting." + key + " 必须是整数（字节），实际=" + raw, error);
        }
        if (value < ValidationRules.UPLOAD_MIN_CONFIGURABLE_BYTES || value > maxConfigurable) {
            throw new IllegalStateException("system_setting." + key + " 超出允许范围 ["
                    + ValidationRules.UPLOAD_MIN_CONFIGURABLE_BYTES + ", " + maxConfigurable
                    + "]，实际=" + value);
        }
        return value;
    }

    private static long uploadDefaultBytes(String key) {
        return switch (key) {
            case UPLOAD_ADMIN_IMAGE_MAX_KEY -> ValidationRules.UPLOAD_ADMIN_IMAGE_DEFAULT_BYTES;
            case UPLOAD_ADMIN_VIDEO_MAX_KEY -> ValidationRules.UPLOAD_ADMIN_VIDEO_DEFAULT_BYTES;
            case UPLOAD_MEMBER_IMAGE_MAX_KEY -> ValidationRules.UPLOAD_MEMBER_IMAGE_DEFAULT_BYTES;
            case UPLOAD_MEMBER_VIDEO_MAX_KEY -> ValidationRules.UPLOAD_MEMBER_VIDEO_DEFAULT_BYTES;
            default -> throw new IllegalArgumentException("未登记的上传上限键: " + key);
        };
    }

    private static long uploadMaxConfigurableBytes(String key) {
        return switch (key) {
            case UPLOAD_ADMIN_IMAGE_MAX_KEY -> ValidationRules.UPLOAD_ADMIN_IMAGE_MAX_CONFIGURABLE_BYTES;
            case UPLOAD_ADMIN_VIDEO_MAX_KEY -> ValidationRules.UPLOAD_ADMIN_VIDEO_MAX_CONFIGURABLE_BYTES;
            case UPLOAD_MEMBER_IMAGE_MAX_KEY -> ValidationRules.UPLOAD_MEMBER_IMAGE_MAX_CONFIGURABLE_BYTES;
            case UPLOAD_MEMBER_VIDEO_MAX_KEY -> ValidationRules.UPLOAD_MEMBER_VIDEO_MAX_CONFIGURABLE_BYTES;
            default -> throw new IllegalArgumentException("未登记的上传上限键: " + key);
        };
    }

    /**
     * 整批保存同一事务：任一键非法则整体回滚，不会出现"一半生效"；成功后清空本实例站点缓存。
     * 缓存不跨实例也不广播失效，别的实例最长要等一个 TTL 才看到新配置。
     */
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
            if (DEFAULT_MARKET_KEY.equals(entry.getKey()) && !MARKET_VALUES.contains(value)) {
                throw ApiException.badRequest("INVALID_DEFAULT_MARKET",
                        "default_market 只能是 CN / HK / MO / TW，实际=" + value);
            }
            if (BRANDING_ATTRIBUTION_KEY.equals(entry.getKey())
                    && !BRANDING_ATTRIBUTION_VALUES.contains(value)) {
                throw ApiException.badRequest("INVALID_BRANDING_ATTRIBUTION",
                        "branding_attribution 只能是 SHOW / HIDDEN，实际=" + value);
            }
            if ("app.timezone".equals(entry.getKey()) && !value.isBlank()) {
                try {
                    ZoneId.of(value);
                } catch (DateTimeException error) {
                    throw ApiException.badRequest("INVALID_TIMEZONE",
                            "app.timezone 必须是 IANA 时区（如 Asia/Shanghai），实际=" + value);
                }
            }
            if ("media_public_base_url".equals(entry.getKey()) && !value.isBlank()
                    && !value.matches("^https?://[^\\s]+$")) {
                throw ApiException.badRequest("INVALID_MEDIA_BASE_URL",
                        "media_public_base_url 必须是 http(s):// 绝对地址，实际=" + value);
            }
            if ("storage_provider".equals(entry.getKey())
                    && !STORAGE_PROVIDER_VALUES.contains(value.toLowerCase(java.util.Locale.ROOT))) {
                throw ApiException.badRequest("INVALID_STORAGE_PROVIDER",
                        "storage_provider 只能是 local/mock/qiniu/cos/aliyun-oss/s3，实际=" + value);
            }
            if (SITE_DOMAIN_KEY.equals(entry.getKey())) {
                value = normalizeSiteDomain(value);
            }
            if (ENABLED_LOCALES_KEY.equals(entry.getKey())) {
                if (value.isBlank()) {
                    throw ApiException.badRequest("INVALID_ENABLED_LOCALES",
                            "enabled_locales 至少需要一种语言（例如 en,ja）");
                }
                for (String part : value.split(",")) {
                    String locale = part.trim();
                    if (!locale.isEmpty() && !locale.matches(LOCALE_PATTERN)) {
                        throw ApiException.badRequest("INVALID_ENABLED_LOCALES",
                                "enabled_locales 含非法语言代码: " + locale);
                    }
                }
            }
            if (UPLOAD_LIMIT_KEYS.contains(entry.getKey())) {
                validateUploadLimit(entry.getKey(), value);
            }
            if (SITE_SCHEME_KEY.equals(entry.getKey()) && !value.matches("https?")) {
                throw ApiException.badRequest("INVALID_SITE_SCHEME",
                        "site_scheme 只能是 http 或 https，实际=" + value);
            }
            repository.upsert(entry.getKey(), value);
        }
        siteCache = null;
        return adminSettings();
    }

    /**
     * 上传上限保存校验：必须是整数、落在 schema 给出的 [最小值, 可配最大值] 内，且不超过
     * Spring multipart 单文件天花板——超过天花板时请求在解析层就被拒，跟"后台填了 100MB 却传不上"
     * 的业务现象完全对不上，所以这里提前拦住。
     */
    private void validateUploadLimit(String key, String value) {
        long maxConfigurable = uploadMaxConfigurableBytes(key);
        if (value.isBlank()) {
            throw ApiException.badRequest("INVALID_UPLOAD_LIMIT",
                    key + " 不能为空（字节数），默认 " + uploadDefaultBytes(key));
        }
        long parsed;
        try {
            parsed = Long.parseLong(value);
        } catch (NumberFormatException error) {
            throw ApiException.badRequest("INVALID_UPLOAD_LIMIT",
                    key + " 必须是整数字节数，实际=" + value);
        }
        if (parsed < ValidationRules.UPLOAD_MIN_CONFIGURABLE_BYTES || parsed > maxConfigurable) {
            throw ApiException.badRequest("INVALID_UPLOAD_LIMIT", key + " 必须在 "
                    + ValidationRules.UPLOAD_MIN_CONFIGURABLE_BYTES + "–" + maxConfigurable
                    + " 字节之间，实际=" + parsed);
        }
        if (parsed > ValidationRules.UPLOAD_MULTIPART_CEILING_BYTES) {
            throw ApiException.badRequest("UPLOAD_LIMIT_EXCEEDS_MULTIPART_CEILING", key
                    + " 不能超过 multipart 单文件天花板 "
                    + ValidationRules.UPLOAD_MULTIPART_CEILING_BYTES + " 字节，实际=" + parsed);
        }
    }

    /**
     * 对外域名只接受 host[:port]（例如 company.binnarui.com:443、192.168.1.15:41285）。
     * 不接受协议、路径或空白：协议由部署层（HTTP/HTTPS）决定，写进配置会让
     * storefront 的 canonical/分享链接出现 http://https:// 这类拼接错误。
     */
    private static String normalizeSiteDomain(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.contains("://") || value.contains("/") || value.contains("?")
                || value.contains("#") || value.contains("@") || !value.equals(value.trim())) {
            throw ApiException.badRequest("INVALID_SITE_DOMAIN",
                    "site_domain 只能是 host[:port]，不能包含协议、路径或空格，实际=" + value);
        }
        String host = value;
        int port = -1;
        int colon = value.lastIndexOf(':');
        if (colon >= 0) {
            host = value.substring(0, colon);
            String portText = value.substring(colon + 1);
            if (!portText.matches("\\d{1,5}")) {
                throw ApiException.badRequest("INVALID_SITE_DOMAIN",
                        "site_domain 端口必须是 1-65535 的整数，实际=" + value);
            }
            port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                throw ApiException.badRequest("INVALID_SITE_DOMAIN",
                        "site_domain 端口必须是 1-65535 的整数，实际=" + value);
            }
        }
        if (host.isBlank() || host.length() > 253
                || !host.matches("^[a-zA-Z0-9](?:[a-zA-Z0-9.-]*[a-zA-Z0-9])?$")) {
            throw ApiException.badRequest("INVALID_SITE_DOMAIN",
                    "site_domain 主机名格式不正确，实际=" + value);
        }
        if (host.contains("..") || host.startsWith(".") || host.endsWith(".")) {
            throw ApiException.badRequest("INVALID_SITE_DOMAIN",
                    "site_domain 主机名格式不正确，实际=" + value);
        }
        String normalized = host.toLowerCase(java.util.Locale.ROOT);
        return port > 0 ? normalized + ":" + port : normalized;
    }

    /**
     * 站点三元组的读缓存：TTL 取 `app.feature.cache-ttl-seconds`（默认 30 秒，<=0 启动即失败）。
     * 过期后在 synchronized 里复查一次再回源，避免并发同时打库；置空只发生在写接口里，且只作用于本实例。
     */
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
