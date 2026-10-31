package com.fullmall.exchange.app;

import com.fullmall.platform.secret.IntegrationConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

/**
 * 自动汇率拉取（阶段 2）。
 *
 * <p>行为约定（用户明确要求）：
 * <ul>
 *   <li>生产未配置供应商：**不自动拉取**，继续用手工汇率；应用照常启动；</li>
 *   <li>演示模式（app.mode=demo）未配置：显式使用 mock，方便演示；</li>
 *   <li>配置了但拉取失败：**保留上次自动/手工汇率**，只记录失败原因，不影响换算；</li>
 *   <li>拉取成功：写入新汇率（生效时间=现在），后续换算自然使用新值。</li>
 * </ul>
 *
 * <p>只做直接汇率：provider 返回什么就存什么，不在这里做三角换算。
 */
@Service
public class ExchangeRateFetchService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateFetchService.class);

    public record Status(boolean configured, String provider, boolean demoMock,
                         Instant lastAttemptAt, Instant lastSuccessAt, String lastError) {
    }

    private final JdbcTemplate jdbc;
    private final IntegrationConfigService integrations;
    private final ExchangeRateService exchangeRates;
    private final String baseCurrency;
    private final String mode;

    public ExchangeRateFetchService(JdbcTemplate jdbc, IntegrationConfigService integrations,
                                    ExchangeRateService exchangeRates,
                                    @Value("${app.fx.base-currency}") String baseCurrency,
                                    @Value("${app.mode}") String mode) {
        this.jdbc = jdbc;
        this.integrations = integrations;
        this.exchangeRates = exchangeRates;
        this.baseCurrency = baseCurrency.trim().toUpperCase(java.util.Locale.ROOT);
        this.mode = mode == null ? "" : mode.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public Status status() {
        ensureStateRow();
        var rows = jdbc.query("""
                SELECT provider, last_attempt_at, last_success_at, last_error
                  FROM exchange_rate_fetch_state WHERE id = true
                """, (rs, rowNum) -> new Status(
                resolveProviderCode() != null,
                rs.getString("provider") == null ? resolveProviderCode() : rs.getString("provider"),
                isDemoMock(),
                rs.getTimestamp("last_attempt_at") == null ? null
                        : rs.getTimestamp("last_attempt_at").toInstant(),
                rs.getTimestamp("last_success_at") == null ? null
                        : rs.getTimestamp("last_success_at").toInstant(),
                rs.getString("last_error")));
        return rows.isEmpty()
                ? new Status(resolveProviderCode() != null, resolveProviderCode(), isDemoMock(),
                null, null, null)
                : rows.get(0);
    }

    /**
     * 立即拉取。失败不抛给调用方（避免"汇率源挂了导致后台页面 500"），
     * 而是把原因写进状态；手工汇率与已有自动汇率保持不变。
     */
    public Status refresh(String actor) {
        String providerCode = resolveProviderCode();
        if (providerCode == null) {
            return status();
        }
        Instant now = Instant.now();
        markAttempt(providerCode, now, null);
        try {
            ExchangeRateProvider provider = provider(providerCode);
            Map<String, BigDecimal> rates = provider.rates(baseCurrency);
            int written = 0;
            for (var entry : rates.entrySet()) {
                written += exchangeRates.recordAuto(baseCurrency, entry.getKey(), entry.getValue(),
                        providerCode, now);
            }
            if (written == 0) {
                throw new IllegalStateException("汇率源没有返回可写入的汇率");
            }
            markSuccess(now);
            log.info("自动汇率拉取成功：provider={} base={} 条数={} by={}",
                    providerCode, baseCurrency, written, actor);
        } catch (RuntimeException failure) {
            String message = failure.getMessage() == null ? failure.getClass().getSimpleName()
                    : failure.getMessage();
            markAttempt(providerCode, now, message);
            log.warn("自动汇率拉取失败，保留上次/手工汇率：provider={} base={} 原因={}",
                    providerCode, baseCurrency, message);
        }
        return status();
    }

    private String resolveProviderCode() {
        String configured = integrations.enabledProvider("exchange_rate");
        if (configured != null && !configured.isBlank()) {
            return configured.trim().toLowerCase(java.util.Locale.ROOT);
        }
        // 演示模式显式使用 mock；生产/开发未配置则不做自动拉取
        return isDemoMock() ? "mock" : null;
    }

    private boolean isDemoMock() {
        return "demo".equals(mode) || "dev".equals(mode);
    }

    private ExchangeRateProvider provider(String code) {
        return switch (code) {
            case "mock" -> new MockExchangeRateProvider();
            case "frankfurter" -> new FrankfurterExchangeRateProvider(
                    integrations.resolveAll("exchange_rate").get("base-url"));
            default -> throw new IllegalStateException("未实现的汇率供应商：" + code);
        };
    }

    private void ensureStateRow() {
        jdbc.update("INSERT INTO exchange_rate_fetch_state (id) VALUES (true) ON CONFLICT (id) DO NOTHING");
    }

    private void markAttempt(String provider, Instant at, String error) {
        ensureStateRow();
        jdbc.update("""
                UPDATE exchange_rate_fetch_state
                   SET provider = ?, last_attempt_at = ?, last_error = ?, updated_at = now()
                 WHERE id = true
                """, provider, Timestamp.from(at), truncate(error));
    }

    private void markSuccess(Instant at) {
        jdbc.update("""
                UPDATE exchange_rate_fetch_state
                   SET last_success_at = ?, last_error = NULL, updated_at = now()
                 WHERE id = true
                """, Timestamp.from(at));
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}