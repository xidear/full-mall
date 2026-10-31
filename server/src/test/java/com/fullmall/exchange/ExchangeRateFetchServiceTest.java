package com.fullmall.exchange;

import com.fullmall.exchange.app.ExchangeRateFetchService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 演示模式（app.mode=demo）未配置供应商时使用 mock；
 * 生产语义（未配置不拉取）由 status.configured 表达，不影响启动。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.scheduling.enabled=false",
                "app.mode=demo"
        })
@ActiveProfiles("test")
class ExchangeRateFetchServiceTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (Exception error) {
            throw new IllegalStateException("无法启动嵌入式 PostgreSQL", error);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    ExchangeRateFetchService fetcher;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM exchange_rate");
        jdbc.update("DELETE FROM exchange_rate_fetch_state");
    }

    @Test
    void demoModeUsesMockAndWritesRates() {
        var before = fetcher.status();
        assertThat(before.configured()).isTrue();
        assertThat(before.provider()).isEqualTo("mock");
        assertThat(before.demoMock()).isTrue();

        var after = fetcher.refresh("test");

        assertThat(after.lastSuccessAt()).isNotNull();
        assertThat(after.lastError()).isNull();
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exchange_rate WHERE source = 'mock'", Integer.class);
        assertThat(count).isGreaterThan(0);
        // 1 CNY = 0.1389 USD（mock 直接汇率）
        assertThat(jdbc.queryForObject("""
                SELECT rate FROM exchange_rate
                 WHERE base_currency = 'CNY' AND quote_currency = 'USD'
                 ORDER BY effective_at DESC LIMIT 1
                """, java.math.BigDecimal.class)).isEqualByComparingTo("0.1389");
    }
}