package com.fullmall.platform.channel;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.secret.IntegrationCatalog;
import com.fullmall.platform.secret.IntegrationConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 「用途 → 供应商」路由（ADR-0018）。
 *
 * <p>解决的问题：一个能力只能启用一个供应商时，验证码与告警只能共用同一条短信通道；
 * 而供应商换成别家又要改代码。现在供应商可以并存，由本表决定某个用途实际走谁。
 *
 * <p>未绑定 = 该用途没有通道：调用点必须明确失败并提示去后台配置，
 * 不允许回退到「第一个启用的供应商」——那会把配置错误伪装成「发送成功」。
 */
@Service
public class IntegrationRouteService {

    private final JdbcTemplate jdbc;
    private final IntegrationConfigService configs;

    public IntegrationRouteService(JdbcTemplate jdbc, IntegrationConfigService configs) {
        this.jdbc = jdbc;
        this.configs = configs;
    }

    public record Route(String capability, String usageKey, String provider, String recipient,
                        Instant updatedAt) {
    }

    /** 解析结果：供应商标识 + 解密后的配置（只给发送实现用，绝不返回给 HTTP 层）。 */
    public record Resolved(String capability, String usageKey, String provider, String recipient,
                           Map<String, String> config) {
    }

    /** 后台勾选矩阵的一行：某用途当前绑定的供应商 + 可选的已配置供应商。 */
    public record UsageView(String capability, String key, String label, String description,
                            String provider, String recipient, boolean needsRecipient,
                            List<String> availableProviders) {
    }

    public List<Route> list() {
        return jdbc.query("""
                SELECT capability, usage_key, provider, recipient, updated_at FROM integration_route
                 ORDER BY capability, usage_key
                """, (rs, rowNum) -> new Route(rs.getString("capability"), rs.getString("usage_key"),
                rs.getString("provider"), rs.getString("recipient"),
                rs.getTimestamp("updated_at").toInstant()));
    }

    public List<UsageView> matrix() {
        Map<String, Route> bound = new java.util.HashMap<>();
        for (Route route : list()) {
            bound.put(route.capability() + "/" + route.usageKey(), route);
        }
        List<UsageView> rows = new ArrayList<>();
        for (ChannelUsage.Usage usage : ChannelUsage.all()) {
            Route route = bound.get(usage.capability() + "/" + usage.key());
            rows.add(new UsageView(usage.capability(), usage.key(), usage.label(), usage.description(),
                    route == null ? null : route.provider(),
                    route == null ? null : route.recipient(),
                    usage.recipientRequired(),
                    configs.enabledProviders(usage.capability())));
        }
        return rows;
    }

    /**
     * 绑定某用途到某供应商。三步顺序不能改：先验用途、再验供应商属于该能力、最后验它已经配好且启用——
     * 否则会留下「绑到一个没有凭证的供应商」的死路由，出问题时报的是供应商鉴权失败，排查方向全错。
     */
    @Transactional
    public Route bind(String capability, String usageKey, String provider, String recipient) {
        ChannelUsage.Usage usage = ChannelUsage.find(capability, usageKey)
                .orElseThrow(() -> ApiException.badRequest("CHANNEL_USAGE_UNSUPPORTED",
                        "未登记的通信用途：" + capability + "/" + usageKey));
        String normalizedProvider = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (IntegrationCatalog.provider(usage.capability(), normalizedProvider).isEmpty()) {
            throw ApiException.badRequest("INTEGRATION_PROVIDER_UNSUPPORTED",
                    "能力「" + usage.capability() + "」不支持供应商：" + provider
                            + "；可选：" + IntegrationCatalog.providersOf(usage.capability()));
        }
        configs.resolveProvider(usage.capability(), normalizedProvider);
        String normalizedRecipient = recipient == null || recipient.isBlank() ? null : recipient.trim();
        if (usage.recipientRequired() && normalizedRecipient == null) {
            throw ApiException.badRequest("CHANNEL_RECIPIENT_REQUIRED",
                    "用途「" + usage.label() + "」必须填写收件人（该用途把消息发给固定的值班人）");
        }
        jdbc.update("""
                INSERT INTO integration_route (capability, usage_key, provider, recipient, updated_at)
                VALUES (?, ?, ?, ?, now())
                ON CONFLICT (capability, usage_key) DO UPDATE
                   SET provider = EXCLUDED.provider, recipient = EXCLUDED.recipient,
                       updated_at = now()
                """, usage.capability(), usage.key(), normalizedProvider, normalizedRecipient);
        return new Route(usage.capability(), usage.key(), normalizedProvider, normalizedRecipient,
                Instant.now());
    }

    @Transactional
    public void unbind(String capability, String usageKey) {
        ChannelUsage.Usage usage = ChannelUsage.find(capability, usageKey)
                .orElseThrow(() -> ApiException.badRequest("CHANNEL_USAGE_UNSUPPORTED",
                        "未登记的通信用途：" + capability + "/" + usageKey));
        int removed = jdbc.update("""
                DELETE FROM integration_route WHERE capability = ? AND usage_key = ?
                """, usage.capability(), usage.key());
        if (removed == 0) {
            throw ApiException.notFound("CHANNEL_ROUTE_NOT_FOUND",
                    "用途「" + usage.label() + "」当前未绑定任何供应商");
        }
    }

    /** 运行时解析：未绑定返回 empty，由调用方决定「演示模式用 mock」还是「生产中明确失败」。 */
    public Optional<Resolved> find(String capability, String usageKey) {
        ChannelUsage.Usage usage = ChannelUsage.find(capability, usageKey).orElse(null);
        if (usage == null) {
            return Optional.empty();
        }
        List<Route> routes = jdbc.query("""
                SELECT capability, usage_key, provider, recipient, updated_at FROM integration_route
                 WHERE capability = ? AND usage_key = ?
                """, (rs, rowNum) -> new Route(rs.getString("capability"), rs.getString("usage_key"),
                rs.getString("provider"), rs.getString("recipient"),
                rs.getTimestamp("updated_at").toInstant()),
                usage.capability(), usage.key());
        if (routes.isEmpty()) {
            return Optional.empty();
        }
        Route route = routes.get(0);
        return Optional.of(new Resolved(usage.capability(), usage.key(), route.provider(),
                route.recipient(), configs.resolveProvider(usage.capability(), route.provider())));
    }

    /** 生产路径用：未绑定即失败，错误信息直接告诉运营去哪里配。 */
    public Resolved require(String capability, String usageKey) {
        return find(capability, usageKey).orElseThrow(() -> missing(capability, usageKey));
    }

    /** 「未绑定通道」的稳定错误：调用方既可抛出，也可在演示模式下先判断再决定。 */
    public ApiException missing(String capability, String usageKey) {
        String label = ChannelUsage.find(capability, usageKey).map(ChannelUsage.Usage::label)
                .orElse(capability + "/" + usageKey);
        return ApiException.conflict("CHANNEL_ROUTE_NOT_CONFIGURED",
                "用途「" + label + "」尚未绑定通道：请到「系统设置 → 集成配置 → 通道路由」勾选供应商");
    }
}
