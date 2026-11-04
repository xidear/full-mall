package com.fullmall.market.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 按市场启用第三方通道（ADR-0014 §8.4）。
 *
 * <p>分工：**凭据**仍在 {@code integration_config}（ADR-005，加密存储、永不回显）；
 * 本表只表达"哪个市场在用哪个服务商"。两者分开的理由：
 * 凭据是密钥生命周期问题，启用关系是运营决策问题，混在一起会让"换个市场用另一家"变成复制密钥。
 *
 * <p>同一市场同一能力**只能有一个启用**（部分唯一索引兜底）：两个同时启用会让"用哪家"不确定。
 *
 * <p>没有该市场的记录时返回 empty。**不回退到任何全局开关**：通道用哪家由「第三方服务 → 通道路由」
 * （ADR-0018）按用途决定，本表只回答“这个市场是否允许该能力”。此前的 `sms_provider` 一类全局键
 * 已随 V10 迁移清掉，不要在这里再引回来。
 */
@Service
public class MarketIntegrationService {

    /** 与 integration_config 的能力白名单保持一致，外加地址校验。 */
    private static final Set<String> CAPABILITIES =
            Set.of("sms", "payment", "storage", "moderation", "logistics", "ai", "address");

    /**
     * 每个能力支持的服务商（AGENTS §13：枚举必须可选、禁止手填）。
     *
     * <p>这里是**唯一来源**：后台下拉从 {@link #catalog()} 取，保存时用同一份清单校验，
     * 避免"填错一个字、永远不生效"的死配置。新增服务商只改这里。
     *
     * <p>`ai` 不在静态清单里——它的服务商来自 `ai_provider` 表（由后台 AI 供应商页维护）。
     */
    private static final Map<String, List<String>> PROVIDERS = Map.of(
            "payment", List.of("mock", "alipay", "wechat", "stripe", "paypal"),
            "logistics", List.of("mock", "kuaidi100", "dhl", "fedex"),
            "sms", List.of("mock", "aliyun", "tencent"),
            "storage", List.of("local", "aliyun-oss", "qiniu", "s3"),
            "moderation", List.of("mock", "aliyun", "tencent"),
            "address", List.of("amap", "tencent-map", "google"));

    private final JdbcTemplate jdbcTemplate;

    public MarketIntegrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Row(String marketCode, String capability, String provider, boolean enabled) {
    }

    /** 后台下拉用：能力 → 可选服务商。`ai` 的候选来自已配置的 AI 供应商，没配就是空列表。 */
    public Map<String, List<String>> catalog() {
        Map<String, List<String>> result = new java.util.TreeMap<>(PROVIDERS);
        result.put("ai", jdbcTemplate.queryForList(
                "SELECT DISTINCT code FROM ai_provider ORDER BY code", String.class));
        return result;
    }

    public List<Row> list(String marketCode) {
        String market = normalize(marketCode);
        requireMarket(market);
        return jdbcTemplate.query("""
                SELECT market_code, capability, provider, enabled
                  FROM market_integration WHERE market_code = ? ORDER BY capability, provider
                """, (rs, rowNum) -> new Row(rs.getString("market_code"), rs.getString("capability"),
                rs.getString("provider"), rs.getBoolean("enabled")), market);
    }

    /**
     * 该市场该能力启用的服务商；没有则 empty（调用方决定是否回退全局）。
     *
     * <p>能力名**同样校验**：拼错的能力如果在读路径静默返回 empty，就会表现成
     * "明明配了却没生效"，比直接报错难查得多。
     */
    public Optional<String> enabledProvider(String marketCode, String capability) {
        if (marketCode == null || marketCode.isBlank()) {
            return Optional.empty();
        }
        String cap = requireCapability(capability);
        return jdbcTemplate.query("""
                SELECT provider FROM market_integration
                 WHERE market_code = ? AND capability = ? AND enabled
                """, (rs, rowNum) -> rs.getString("provider"),
                normalize(marketCode), cap).stream().findFirst();
    }

    public boolean isEnabled(String marketCode, String capability, String provider) {
        return enabledProvider(marketCode, capability)
                .map(enabled -> enabled.equalsIgnoreCase(provider))
                .orElse(false);
    }

    /**
     * 启用/停用某市场某能力的服务商。
     *
     * <p>启用时先关掉同市场同能力的其它 provider（同事务）：否则会撞部分唯一索引，
     * 而且"两个都启用"本身就是不确定状态。
     */
    @Transactional
    public List<Row> setEnabled(String marketCode, String capability, String provider, boolean enabled) {
        String market = normalize(marketCode);
        requireMarket(market);
        String cap = requireCapability(capability);
        String prov = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (prov.isEmpty()) {
            throw ApiException.badRequest("INTEGRATION_PROVIDER_REQUIRED", "服务商标识不能为空");
        }
        // 候选集合校验（AGENTS §13）：只有前端下拉是不够的，请求可以绕过页面
        if (!catalog().getOrDefault(cap, List.of()).contains(prov)) {
            throw ApiException.badRequest("INTEGRATION_PROVIDER_UNSUPPORTED",
                    "能力 " + cap + " 不支持服务商 " + prov + "，可选：" + catalog().get(cap));
        }
        if (enabled) {
            // 先把市场行锁住再"关旧的、开新的"：否则两个并发启用会各自读到"没有别的启用项"，
            // 双双执行 INSERT，最后一个人撞部分唯一索引拿 500（而且"用哪家"取决于提交顺序）。
            jdbcTemplate.queryForObject(
                    "SELECT code FROM market WHERE code = ? FOR UPDATE", String.class, market);
            jdbcTemplate.update("""
                    UPDATE market_integration SET enabled = FALSE
                     WHERE market_code = ? AND capability = ? AND enabled AND provider <> ?
                    """, market, cap, prov);
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO market_integration (market_code, capability, provider, enabled)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (market_code, capability, provider) DO UPDATE SET enabled = EXCLUDED.enabled
                    """, market, cap, prov, enabled);
        } catch (org.springframework.dao.DataIntegrityViolationException conflict) {
            // 兜底：即便行锁也没拦住（例如另一条路径直接写表），也要给出稳定错误码而不是 500
            throw ApiException.conflict("INTEGRATION_ENABLE_CONFLICT",
                    "该市场该能力已有另一个启用中的服务商，请刷新后重试");
        }
        return list(market);
    }

    /** 能力名白名单：拼错就是一条永不生效的死配置，读写都必须在入口拒绝。 */
    private String requireCapability(String capability) {
        String cap = capability == null ? "" : capability.trim().toLowerCase(Locale.ROOT);
        if (!CAPABILITIES.contains(cap)) {
            throw ApiException.badRequest("INTEGRATION_CAPABILITY_INVALID",
                    "不支持的能力: " + capability + "，可选：" + CAPABILITIES);
        }
        return cap;
    }

    private void requireMarket(String marketCode) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM market WHERE code = ?", Long.class, marketCode);
        if (count == null || count == 0) {
            throw ApiException.notFound("MARKET_NOT_FOUND", "市场不存在: " + marketCode);
        }
    }

    private String normalize(String marketCode) {
        return marketCode == null ? "" : marketCode.trim().toUpperCase(Locale.ROOT);
    }
}
