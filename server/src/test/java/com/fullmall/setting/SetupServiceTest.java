package com.fullmall.setting;

import com.fullmall.setting.app.SetupService;
import com.fullmall.setting.app.SystemSettingService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 首次配置检查：缺项要能指出，标记完成后状态变化，但不阻断使用。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SetupServiceTest {

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
    SetupService setup;

    @Autowired
    SystemSettingService settings;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void reportsMissingItemsThenCompleted() {
        jdbc.update("DELETE FROM system_setting WHERE setting_key IN"
                + " ('setup_completed','payment_provider','site_name')");

        var before = setup.status();
        assertThat(before.completed()).isFalse();
        assertThat(before.ready()).isFalse();
        assertThat(before.items()).anySatisfy(item -> {
            assertThat(item.key()).isEqualTo("payment_provider");
            assertThat(item.configured()).isFalse();
            assertThat(item.hint()).contains("未配置真实支付");
        });

        // mock 不算已配置（演示可用，但上线必须换真实服务商）
        jdbc.update("INSERT INTO system_setting (setting_key, setting_value)"
                + " VALUES ('payment_provider','mock') ON CONFLICT (setting_key)"
                + " DO UPDATE SET setting_value = 'mock'");
        assertThat(setup.status().items())
                .anySatisfy(item -> assertThat(item.configured()).isFalse());

        setup.markCompleted();
        assertThat(setup.status().completed()).isTrue();
    }

    @Test
    void configuredProviderCountsAsReady() {
        jdbc.update("DELETE FROM system_setting WHERE setting_key = 'payment_provider'");
        settings.save(Map.of("payment_provider", "wechat"));

        assertThat(setup.status().items())
                .anySatisfy(item -> {
                    if (item.key().equals("payment_provider")) {
                        assertThat(item.configured()).isTrue();
                    }
                });
    }
}
