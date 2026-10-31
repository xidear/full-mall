package com.fullmall.exchange;

import com.fullmall.exchange.app.ExchangeRateService;
import com.fullmall.platform.error.ApiException;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 汇率换算：按币种小数位、BigDecimal 精确、反向推导、找不到不兜底。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ExchangeRateServiceTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final Instant WHEN = Instant.parse("2026-10-31T00:00:00Z");

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
    ExchangeRateService service;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM exchange_rate");
    }

    @Test
    void convertsUsingCurrencyMinorUnits() {
        service.upsert("USD", "CNY", new BigDecimal("7.2"), "TEST", WHEN, "admin");

        var conversion = service.convert(10_000, "USD", "CNY", WHEN);

        // 100.00 USD * 7.2 = 720.00 CNY = 72000 分
        assertThat(conversion.convertedAmount()).isEqualTo(72_000);
        assertThat(conversion.rate()).isEqualByComparingTo("7.2");
        assertThat(conversion.inverse()).isFalse();
    }

    @Test
    void zeroDecimalTargetCurrencyIsNotScaledBy100() {
        service.upsert("USD", "JPY", new BigDecimal("150"), "TEST", WHEN, "admin");

        var conversion = service.convert(10_000, "USD", "JPY", WHEN);

        // 100 USD * 150 = 15000 日元（JPY 0 位，不是 1500000）
        assertThat(conversion.convertedAmount()).isEqualTo(15_000);
    }

    @Test
    void inverseRateIsDerivedWhenOnlyReverseExists() {
        service.upsert("USD", "CNY", new BigDecimal("7.2"), "TEST", WHEN, "admin");

        var conversion = service.convert(72_000, "CNY", "USD", WHEN);

        assertThat(conversion.inverse()).isTrue();
        assertThat(conversion.rate()).isBetween(new BigDecimal("0.1388"), new BigDecimal("0.1390"));
        assertThat(conversion.convertedAmount()).isEqualTo(10_000);
    }

    @Test
    void missingRateFailsExplicitly() {
        assertThatThrownBy(() -> service.convert(100, "USD", "EUR", WHEN))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("EXCHANGE_RATE_NOT_FOUND"));
    }

    @Test
    void duplicatePairAndEffectiveTimeIsConflict() {
        service.upsert("USD", "CNY", new BigDecimal("7.2"), "TEST", WHEN, "admin");

        assertThatThrownBy(() -> service.upsert("USD", "CNY", new BigDecimal("7.3"),
                "TEST", WHEN, "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("EXCHANGE_RATE_EXISTS"));
    }

    @Test
    void deletionRemovesRate() {
        var created = service.upsert("USD", "CNY", new BigDecimal("7.2"), "TEST", WHEN, "admin");

        service.delete(created.id(), "admin");

        assertThatThrownBy(() -> service.convert(100, "USD", "CNY", WHEN))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("没有可用汇率");
    }
}