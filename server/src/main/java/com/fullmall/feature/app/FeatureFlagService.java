package com.fullmall.feature.app;

import com.fullmall.feature.domain.FeatureFlag;
import com.fullmall.feature.infra.FeatureFlagRepository;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * 功能开关读取与变更（FLAG-001..005）。
 *
 * <p>缓存 TTL 默认 30 秒（FLAG-004 要求 ≤ 30s），读取路径命中缓存时不访问数据库。
 * 编码不存在、影响行数不为 1 等情况直接抛错，不做默认值兜底（见 AGENTS.md 第 1 节）。
 */
@Service
public class FeatureFlagService {

    public static final String SUPPLIER_ENABLED = "supplier.enabled";
    public static final String POS_SALE_ENABLED = "pos.sale.enabled";
    public static final String POS_PICKUP_ENABLED = "pos.pickup.enabled";

    public static final Set<String> KNOWN_CODES = Set.of(SUPPLIER_ENABLED, POS_SALE_ENABLED, POS_PICKUP_ENABLED);

    private final FeatureFlagRepository repository;
    private final AuditEventService auditEvents;
    private final Clock clock;
    private final Duration ttl;
    private volatile Snapshot snapshot;

    public FeatureFlagService(FeatureFlagRepository repository,
                              AuditEventService auditEvents,
                              Clock clock,
                              @Value("${app.feature.cache-ttl-seconds:30}") long cacheTtlSeconds) {
        this.repository = repository;
        this.auditEvents = auditEvents;
        this.clock = clock;
        if (cacheTtlSeconds <= 0) {
            throw new IllegalArgumentException(
                    "app.feature.cache-ttl-seconds 必须大于 0，实际=" + cacheTtlSeconds);
        }
        this.ttl = Duration.ofSeconds(cacheTtlSeconds);
    }

    public boolean isEnabled(String code) {
        FeatureFlag flag = current().get(code);
        if (flag == null) {
            throw new IllegalStateException(
                    "未知功能开关: " + code + "；请检查 feature_flag 表初始数据与特征编码常量是否一致");
        }
        return flag.enabled();
    }

    public Map<String, FeatureFlag> findAll() {
        return Map.copyOf(current());
    }

    public FeatureFlag get(String code) {
        FeatureFlag flag = current().get(code);
        if (flag == null) {
            throw ApiException.notFound("UNKNOWN_FEATURE_FLAG", "未知功能开关: " + code);
        }
        return flag;
    }

    /**
     * 变更开关。必须提供原因（FLAG-004 审计要求），未知编码返回 400。
     */
    public FeatureFlag update(String code, boolean enabled, String reason, String actor) {
        if (!KNOWN_CODES.contains(code)) {
            throw ApiException.badRequest("UNKNOWN_FEATURE_FLAG", "未知功能开关: " + code);
        }
        if (reason == null || reason.isBlank()) {
            throw ApiException.badRequest("REASON_REQUIRED", "变更功能开关必须填写原因");
        }
        String updatedBy = (actor == null || actor.isBlank()) ? "anonymous" : actor.trim();
        Instant now = clock.instant();
        int updated = repository.update(code, enabled, updatedBy, reason.trim(), now);
        if (updated != 1) {
            throw new IllegalStateException(
                    "更新功能开关失败: code=" + code + "，影响行数=" + updated + "（期望 1）");
        }
        invalidate();
        auditEvents.record(new AuditEventService.AuditEvent(
                "FEATURE_FLAG_CHANGED", updatedBy, "feature_flag", code,
                "enabled=" + enabled + ";reason=" + reason.trim(), null));
        return get(code);
    }

    public void invalidate() {
        snapshot = null;
    }

    private Map<String, FeatureFlag> current() {
        Snapshot cached = snapshot;
        Instant now = clock.instant();
        if (cached != null && !now.isAfter(cached.loadedAt().plus(ttl))) {
            return cached.flags();
        }
        synchronized (this) {
            cached = snapshot;
            now = clock.instant();
            if (cached != null && !now.isAfter(cached.loadedAt().plus(ttl))) {
                return cached.flags();
            }
            Map<String, FeatureFlag> loaded = repository.findAll();
            snapshot = new Snapshot(Map.copyOf(loaded), now);
            return loaded;
        }
    }

    private record Snapshot(Map<String, FeatureFlag> flags, Instant loadedAt) {
    }
}