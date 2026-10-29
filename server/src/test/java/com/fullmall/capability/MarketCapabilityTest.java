package com.fullmall.capability;

import com.fullmall.capability.app.MarketCapabilityService;
import com.fullmall.platform.error.ApiException;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 市场能力门禁：市场 × 目的地 能不能做生意。
 *
 * <p>核心断言：**"没配"不等于"全球都能送"**。跨境必须显式开通目的地 + 配运费规则，
 * 否则在下单入口就被拦下，而不是等算完价再炸。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class MarketCapabilityTest {

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
    MarketCapabilityService capabilities;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM market_destination");
        jdbc.update("DELETE FROM shipping_rate");
        jdbc.update("""
                INSERT INTO market (code, name, currency, country_code, tax_jurisdiction, price_includes_tax,
                                    timezone, active)
                VALUES ('CN', '中国', 'CNY', 'CN', 'CN-VAT', TRUE, 'Asia/Shanghai', TRUE)
                ON CONFLICT (code) DO UPDATE SET active = TRUE
                """);
    }

    @Test
    void domesticIsDeliverableByDefault() {
        var result = capabilities.evaluate("CN", "CN");
        assertThat(result.deliverable()).isTrue();
        assertThat(result.addressFormat()).isTrue();
        assertThat(result.shippingRule()).isTrue();
        assertThat(result.canCheckout()).isTrue();
    }

    @Test
    void crossBorderIsNotDeliverableWithoutExplicitRow() {
        // 没配 market_destination 不等于"全球都能送"
        var result = capabilities.evaluate("CN", "SG");
        assertThat(result.deliverable()).isFalse();
        assertThat(result.blockers()).contains("DESTINATION_NOT_DELIVERABLE");
        assertThatThrownBy(() -> capabilities.requireCheckoutCapable("CN", "SG"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("暂不支持配送到该国家");
    }

    @Test
    void crossBorderStillNeedsShippingRuleAfterEnablingDestination() {
        jdbc.update("""
                INSERT INTO market_destination (market_code, destination_country, deliverable)
                VALUES ('CN', 'SG', TRUE) ON CONFLICT DO NOTHING
                """);
        var result = capabilities.evaluate("CN", "SG");
        assertThat(result.deliverable()).isTrue();
        // 运费规则还没有：依然不能下单
        assertThat(result.blockers()).contains("SHIPPING_RULE_MISSING");
        assertThatThrownBy(() -> capabilities.requireCheckoutCapable("CN", "SG"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("没有配置运费规则");
    }

    @Test
    void crossBorderBecomesOrderableOnceEverythingIsConfigured() {
        jdbc.update("""
                INSERT INTO market_destination (market_code, destination_country, deliverable)
                VALUES ('CN', 'SG', TRUE) ON CONFLICT DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO shipping_rate (id, market_code, destination_country, min_weight_grams,
                                          max_weight_grams, fee_amount, currency)
                VALUES (?, 'CN', 'SG', 0, NULL, 5000, 'CNY')
                """, UUID.randomUUID());
        var result = capabilities.requireCheckoutCapable("CN", "SG");
        assertThat(result.canCheckout()).isTrue();
        assertThat(result.blockers()).isEmpty();
    }

    @Test
    void inactiveMarketBlocksCheckout() {
        jdbc.update("UPDATE market SET active = FALSE WHERE code = 'CN'");
        var result = capabilities.evaluate("CN", "CN");
        assertThat(result.marketActive()).isFalse();
        assertThat(result.blockers()).contains("MARKET_INACTIVE");
        assertThatThrownBy(() -> capabilities.requireCheckoutCapable("CN", "CN"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("该市场已停用");
    }

    @Test
    void missingDestinationIsItsOwnBlocker() {
        var result = capabilities.evaluate("CN", null);
        assertThat(result.blockers()).containsExactly("DESTINATION_REQUIRED");
    }
}
