package com.fullmall.licensing.app;

import com.fullmall.licensing.config.LicenseProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 许可证读取、缓存与只读判定。
 *
 * <p>设计取舍：
 * <ul>
 *   <li>默认 `app.license.enabled=false`：未授权时**不阻断启动**（与"缺配置不阻断启动"一致）；</li>
 *   <li>到期/域名不符/时钟回拨 → **只读模式**（GET 可用，写被拒），不锁数据；</li>
 *   <li>时钟回拨检测依赖 `license_state.last_seen_at`，**只前进不后退**。</li>
 * </ul>
 */
@Service
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final LicenseProperties properties;
    private final ObjectProvider<JdbcTemplate> jdbcTemplate;

    private volatile Cached cached;

    public LicenseService(LicenseProperties properties, ObjectProvider<JdbcTemplate> jdbcTemplate) {
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 对外可见，便于控制器拿到 refresh 结果。 */
    public record Cached(Instant checkedAt, LicensePayload payload, LicenseStatus status) {
    }

    /** 供过滤器/控制器读取；每 60 秒重新判定一次（到期无需重启即生效）。 */
    public LicenseStatus status() {
        Cached snapshot = cached;
        if (snapshot != null && Duration.between(snapshot.checkedAt(), Instant.now()).toSeconds() < 60) {
            return snapshot.status();
        }
        return refresh().status();
    }

    public synchronized Cached refresh() {
        Cached result = evaluate();
        cached = result;
        return result;
    }

    private Cached evaluate() {
        Instant now = Instant.now();
        if (!properties.isEnabled()) {
            return new Cached(now, null, new LicenseStatus("UNLICENSED",
                    "未启用许可校验（app.license.enabled=false）", null, "community",
                    List.of(), null, false));
        }
        if (properties.getPublicKey() == null || properties.getPublicKey().isBlank()) {
            return new Cached(now, null, new LicenseStatus("INVALID",
                    "缺少 app.license.public-key，无法验签", null, null, List.of(), null, true));
        }
        if (properties.getFile() == null || properties.getFile().isBlank()) {
            return new Cached(now, null, new LicenseStatus("TRIAL",
                    "未安装许可证，按试用运行（只读？否）", null, null, List.of(), null, false));
        }

        LicensePayload payload;
        try {
            String raw = Files.readString(Path.of(properties.getFile()));
            var signed = MAPPER.readValue(raw, LicenseVerifier.Signed.class);
            payload = LicenseVerifier.verify(signed, properties.getPublicKey());
        } catch (IOException error) {
            return new Cached(now, null, new LicenseStatus("INVALID",
                    "许可证文件读取失败：" + error.getMessage(), null, null, List.of(), null, true));
        } catch (RuntimeException error) {
            return new Cached(now, null, new LicenseStatus("INVALID",
                    error.getMessage(), null, null, List.of(), null, true));
        }
        if (payload == null) {
            return new Cached(now, null, new LicenseStatus("INVALID",
                    "许可证签名校验失败（文件被修改或非本系统签发）", null, null, List.of(), null, true));
        }

        // 时钟回拨：记录的最近运行时间晚于当前时间太多 → 拒绝（防止改系统时间续命）
        Instant lastSeen = lastSeenAt();
        if (lastSeen != null
                && now.plus(Duration.ofHours(properties.getClockSkewHours())).isBefore(lastSeen)) {
            return new Cached(now, payload, new LicenseStatus("CLOCK_ROLLBACK",
                    "检测到系统时间回拨（上次运行 " + lastSeen + "），进入只读",
                    payload.customer(), payload.edition(), payload.features(),
                    payload.expiresAt(), true));
        }
        recordLastSeen(now, payload);

        String host = currentHost();
        if (properties.isDomainCheck() && !LicenseVerifier.domainAllowed(payload.domains(), host)) {
            return new Cached(now, payload, new LicenseStatus("DOMAIN_MISMATCH",
                    "当前域名 " + host + " 不在许可证范围内：" + payload.domains(),
                    payload.customer(), payload.edition(), payload.features(),
                    payload.expiresAt(), true));
        }
        if (LicenseVerifier.expired(payload, now)) {
            return new Cached(now, payload, new LicenseStatus("EXPIRED",
                    "许可证已于 " + payload.expiresAt() + " 到期，进入只读（可查看与导出，不能新增/修改）",
                    payload.customer(), payload.edition(), payload.features(),
                    payload.expiresAt(), true));
        }
        return new Cached(now, payload, new LicenseStatus("ACTIVE",
                "许可证有效", payload.customer(), payload.edition(), payload.features(),
                payload.expiresAt(), false));
    }

    /** 当前请求域名（由过滤器写入），无请求上下文时返回 localhost。 */
    private static final ThreadLocal<String> CURRENT_HOST = new ThreadLocal<>();

    public static void setCurrentHost(String host) {
        CURRENT_HOST.set(host);
    }

    public static void clearCurrentHost() {
        CURRENT_HOST.remove();
    }

    private String currentHost() {
        String host = CURRENT_HOST.get();
        return host == null || host.isBlank() ? "localhost" : host;
    }

    private Instant lastSeenAt() {
        JdbcTemplate jdbc = jdbcTemplate.getIfAvailable();
        if (jdbc == null) {
            return null;
        }
        try {
            return jdbc.query("SELECT last_seen_at FROM license_state WHERE id = 1",
                    rs -> rs.next() ? rs.getTimestamp("last_seen_at").toInstant() : null);
        } catch (RuntimeException error) {
            log.warn("读取 license_state 失败（不影响启动）：{}", error.getMessage());
            return null;
        }
    }

    private void recordLastSeen(Instant now, LicensePayload payload) {
        JdbcTemplate jdbc = jdbcTemplate.getIfAvailable();
        if (jdbc == null) {
            return;
        }
        try {
            jdbc.update("""
                    INSERT INTO license_state (id, last_seen_at, license_id, customer, updated_at)
                    VALUES (1, ?, ?, ?, now())
                    ON CONFLICT (id) DO UPDATE
                       SET last_seen_at = GREATEST(license_state.last_seen_at, EXCLUDED.last_seen_at),
                           license_id = EXCLUDED.license_id,
                           customer = EXCLUDED.customer,
                           updated_at = now()
                    """, java.sql.Timestamp.from(now), payload.licenseId(), payload.customer());
        } catch (RuntimeException error) {
            log.warn("写入 license_state 失败（不影响运行）：{}", error.getMessage());
        }
    }
}
