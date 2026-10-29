package com.fullmall.market;

import com.fullmall.capability.app.MarketCapabilityService;
import com.fullmall.market.app.MarketIntegrationService;
import com.fullmall.setting.app.SystemSettingService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 收口验证（批次 G）：市场级设置覆盖全局、通道按市场启用。
 *
 * <p>两条断言核心：
 * <ul>
 *   <li>香港站的 ICP/客服电话必须是香港自己的，不能显示大陆的备案号；</li>
 *   <li>同一市场同一能力同时只能启用一个服务商（否则"用哪家"不确定）。</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class MarketScopedConsolidationTest {

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
    SystemSettingService settings;

    @Autowired
    MarketIntegrationService integrations;

    @Autowired
    com.fullmall.market.app.MarketService marketService;

    @Autowired
    MarketCapabilityService capabilities;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM market_integration");
        jdbc.update("DELETE FROM market_setting");
        jdbc.update("""
                INSERT INTO market (code, name, currency, country_code, tax_jurisdiction, price_includes_tax,
                                    timezone, active)
                VALUES ('CN', '中国', 'CNY', 'CN', 'CN-VAT', TRUE, 'Asia/Shanghai', TRUE)
                ON CONFLICT (code) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO market (code, name, currency, country_code, tax_jurisdiction, price_includes_tax,
                                    timezone, active)
                VALUES ('HK', '香港', 'HKD', 'HK', 'HK', TRUE, 'Asia/Hong_Kong', TRUE)
                ON CONFLICT (code) DO NOTHING
                """);
        // 全局值（站点级）
        jdbc.update("""
                INSERT INTO system_setting (setting_key, setting_value) VALUES ('icp_number', 'GLOBAL-ICP')
                ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value
                """);
        jdbc.update("""
                INSERT INTO system_setting (setting_key, setting_value) VALUES ('logistics_provider', 'global-carrier')
                ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value
                """);
    }

    @Test
    void marketSettingOverridesGlobalForThatMarketOnly() {
        marketService.putSettings("HK", Map.of("icp_number", "HK-ICP", "service_phone", "+852 0000 0000"));

        assertThat(settings.publicSettings("HK").get("icp_number")).isEqualTo("HK-ICP");
        assertThat(settings.publicSettings("HK").get("service_phone")).isEqualTo("+852 0000 0000");
        // 其它市场仍是全局值：不能因为给香港配了，就把大陆也改了
        assertThat(settings.publicSettings("CN").get("icp_number")).isEqualTo("GLOBAL-ICP");
        // 无市场上下文时只给全局值，不替调用方挑一个市场
        assertThat(settings.publicSettings(null).get("icp_number")).isEqualTo("GLOBAL-ICP");
    }

    @Test
    void marketValueFallsBackToGlobalThenEmpty() {
        assertThat(settings.marketValue("HK", "logistics_provider")).isEqualTo("global-carrier");
        marketService.putSettings("HK", Map.of("logistics_provider", "kuaidi100"));
        assertThat(settings.marketValue("HK", "logistics_provider")).isEqualTo("kuaidi100");
        assertThat(settings.marketValue("CN", "logistics_provider")).isEqualTo("global-carrier");
    }

    @Test
    void enablingOneProviderDisablesTheOtherForSameCapability() {
        integrations.setEnabled("HK", "logistics", "kuaidi100", true);
        assertThat(integrations.enabledProvider("HK", "logistics")).contains("kuaidi100");

        integrations.setEnabled("HK", "logistics", "dhl", true);
        assertThat(integrations.enabledProvider("HK", "logistics")).contains("dhl");
        // 旧的一个必须被关掉，否则"用哪家"不确定
        assertThat(integrations.isEnabled("HK", "logistics", "kuaidi100")).isFalse();
        // 另一个市场不受影响
        assertThat(integrations.enabledProvider("CN", "logistics")).isEmpty();
    }

    @Test
    void capabilityViewReflectsMarketIntegration() {
        assertThat(capabilities.evaluate("HK", "HK").paymentEnabled()).isFalse();
        integrations.setEnabled("HK", "payment", "stripe", true);
        assertThat(capabilities.evaluate("HK", "HK").paymentEnabled()).isTrue();
        assertThat(capabilities.evaluate("CN", "CN").paymentEnabled()).isFalse();
    }

    /**
     * 审计发现（B）：原来用 isBlank 判断市场级值，导致"显式清空"被回退成全局值——
     * 香港想表示"没有 ICP 备案号"时，页脚会显示大陆的备案号。
     * 现在的语义：market_setting 有这一行就用它（哪怕是空串）；没有这一行才回退全局。
     */
    @Test
    void explicitlyEmptyMarketValueDoesNotFallBackToGlobal() {
        marketService.putSettings("HK", Map.of("icp_number", ""));
        assertThat(settings.publicSettings("HK").get("icp_number")).isEmpty();
        // 其它市场不受影响
        assertThat(settings.publicSettings("CN").get("icp_number")).isEqualTo("GLOBAL-ICP");
        assertThat(settings.marketValue("HK", "icp_number")).isEmpty();
    }

    /**
     * 审计发现（A）：读路径原来不校验能力白名单，拼错的能力会"静默返回没有启用项"，
     * 表现成"明明配了却没生效"。读写都必须在入口拒绝。
     */
    @Test
    void unknownCapabilityIsRejectedOnReadToo() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> integrations.enabledProvider("HK", "paymnet"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不支持的能力");
    }

    @Test
    void unknownCapabilityIsRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> integrations.setEnabled("HK", "blockchain", "x", true))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不支持的能力");
    }
}
