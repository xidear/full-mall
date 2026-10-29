package com.fullmall.platform.media;

import com.fullmall.setting.app.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 媒体地址规范化：**对外输出一律使用绝对 http(s) URL，绝不把相对路径交给客户端**。
 *
 * <p>原因（用户 2026-10-19 明确）：
 * <ol>
 *   <li>域名会变；</li>
 *   <li>小程序/客户端要求 http(s):// 绝对地址；</li>
 *   <li>存储商与存储方案会整体迁移（测试期本地 → 上线云存储）。</li>
 * </ol>
 *
 * <p>规则：
 * <ul>
 *   <li>已是 http(s):// 绝对地址 → 原样保留（历史快照指向当时的存储/CDN）；</li>
 *   <li>协议相对 `//cdn/x.jpg` → 补 `https:`；</li>
 *   <li>相对路径 → 拼接公共基址：优先 `app.media.public-base-url`，否则跟随后台配置的
 *       `site_domain` + `site_scheme`（30 秒缓存），最后才报错；</li>
 *   <li>含 `../` 或空白 → 拒绝（防止路径穿越与脏数据进快照）；</li>
 *   <li>null/空 → 返回 null（由调用方决定是否允许无图）。</li>
 * </ul>
 */
@Component
public class MediaUrlResolver {

    // 30 秒：baseUrl() 经 rawValue() 每次都查 system_setting（无缓存），而列表/详情是逐张图解析，必须缓存。
    private static final Duration SITE_CACHE_TTL = Duration.ofSeconds(30);

    // publicBaseUrl 来自 app.media.public-base-url（env MEDIA_PUBLIC_BASE_URL），构造后不变；cached* 是解析缓存，
    // 读多写极少故不加锁：volatile 保证读到完整值，并发最多重复解析一次。
    private final String publicBaseUrl;
    private final SystemSettingService settings;
    private volatile String cachedBase;
    private volatile Instant cachedAt;

    /** 单元测试/无 Spring 上下文时使用；未配置基址时仍按原语义抛错。 */
    public MediaUrlResolver(String publicBaseUrl) {
        this(publicBaseUrl, null);
    }

    @Autowired
    public MediaUrlResolver(@Value("${app.media.public-base-url:}") String publicBaseUrl,
                            SystemSettingService settings) {
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        this.settings = settings;
    }

    /**
     * 把库里的媒体地址转成绝对 http(s) URL，顺序不能调整：绝对地址原样返回（历史快照指向当时的存储/CDN）、
     * 协议相对补 {@code https:}、含 {@code ..} 抛 {@link IllegalArgumentException}、其余拼 {@link #baseUrl()}。
     * 基址为空抛 {@link IllegalStateException} 而不退回相对路径：相对地址到小程序/APP 就是裂图。
     *
     * <p>已知边界：{@code ..} 是纯字符串包含，{@code a..b.jpg} 也会被拒；中间空格不拦（只 trim 首尾）。
     */
    public String absolute(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();

        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("//")) {
            return "https:" + value;
        }
        if (value.contains("..")) {
            throw new IllegalArgumentException("媒体路径不允许包含 '..'： " + value);
        }
        String base = baseUrl();
        if (base.isBlank()) {
            throw new IllegalStateException(
                    "未配置 app.media.public-base-url 且 site_domain 为空，无法把相对路径转为绝对地址: " + value);
        }
        // 补/去斜杠：否则拼出 https://cdn.comassets/x.jpg，浏览器容忍但小程序与 CDN 回源会失败。
        String normalizedBase = base.endsWith("/")
                ? base.substring(0, base.length() - 1)
                : base;
        String path = value.startsWith("/") ? value : "/" + value;
        return normalizedBase + path;
    }

    /**
     * 公共基址的解析顺序：
     * 1. `MEDIA_PUBLIC_BASE_URL` / `app.media.public-base-url`：换 CDN 或对象存储时显式指定；
     * 2. 后台配置的站点域名：换域名后自动跟随，不需要改数据库里的相对路径；
     * 3. 空：调用方必须明确失败，不能静默返回相对地址。
     */
    private String baseUrl() {
        if (settings == null) {
            return publicBaseUrl;
        }
        // 不加锁：并发未命中时各查一次、结果等价；加锁会把逐图解析串行化。
        Instant now = Instant.now();
        String cached = cachedBase;
        if (cached != null && cachedAt != null
                && Duration.between(cachedAt, now).compareTo(SITE_CACHE_TTL) < 0) {
            return cached;
        }
        // 数据库优先：后台「媒体/静态资源基址」是运维可改的唯一来源；
        // env 只作为开发/CI 兜底；最后才跟随站点域名。
        String dbBase = settings.rawValue("media_public_base_url");
        String resolved;
        if (dbBase != null && !dbBase.isBlank()) {
            resolved = dbBase.trim();
        } else if (!publicBaseUrl.isBlank()) {
            resolved = publicBaseUrl;
        } else {
            Map<String, String> all = settings.publicSettings();
            String domain = all.get("site_domain");
            if (domain == null || domain.isBlank()) {
                // 三个来源全空：返回 "" 交 absolute() 报错，不造默认域名——硬编码会变成换域名后的死链。
                return "";
            }
            String scheme = all.get("site_scheme");
            // site_scheme 缺失/非法时按端口猜（443→https，其余→http）。反代在 80 强制跳 https 的部署必须显式配，
            // 否则拼出 http:// 会被浏览器当混合内容拦掉。
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                String port = domain.contains(":") ? domain.substring(domain.lastIndexOf(':') + 1) : "";
                scheme = port.isEmpty() || "443".equals(port) ? "https" : "http";
            }
            resolved = scheme + "://" + domain;
        }
        cachedBase = resolved;
        cachedAt = now;
        return resolved;
    }
}
