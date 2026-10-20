package com.fullmall.setting;

import com.fullmall.setting.app.SystemSettingService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 后台表单能保存全部经销配置键（白名单必须收录），保存后立即可读。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class DistributionSettingsKeysTest {

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

    @Test
    void savesAllDistributionKeysAndReadsThemBack() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("distribution.level1-mode", "FIXED");
        values.put("distribution.level1-value", "800");
        values.put("distribution.level2-enabled", "false");
        values.put("distribution.level2-mode", "GROSS");
        values.put("distribution.level2-value", "2");
        values.put("distribution.bind-mode", "ONCE");
        values.put("distribution.bind-timing", "REGISTER");
        values.put("distribution.freeze-policy", "FIXED_DAYS");
        values.put("distribution.after-sale-window-days", "10");
        values.put("distribution.min-withdrawal", "20000");
        values.put("distribution.payout-methods", "ALIPAY,WECHAT");
        values.put("distribution.staff-payout", "PAYROLL");
        values.put("distribution.staff-level2-enabled", "false");

        settings.save(values);

        Map<String, String> stored = settings.adminSettings();
        assertThat(stored).containsAllEntriesOf(values);

        var config = new com.fullmall.distribution.app.DistributionConfig(settings).load();
        assertThat(config.level1().mode()).isEqualTo(com.fullmall.distribution.app.DistributionConfig.RateMode.FIXED);
        assertThat(config.minWithdrawal()).isEqualTo(20_000);
        assertThat(config.payoutMethods()).containsExactly("ALIPAY", "WECHAT");
        assertThat(config.bindMode()).isEqualTo("ONCE");
    }
}
