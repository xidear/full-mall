package com.fullmall.setting;

import com.fullmall.setting.app.SetupService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
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
 * 首次配置里的"示例数据 / 完全空白"选择。
 *
 * <p>规则：首次配置阶段允许清空；一旦已有订单/会员且已标记完成，就不允许（清空等于灾难）。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SetupDataModeTest {

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
    JdbcTemplate jdbc;

    /** 每个用例都从"尚未完成配置、没有会员"开始：用例之间不互相污染。 */
    @org.junit.jupiter.api.BeforeEach
    void resetOnboardingState() {
        jdbc.update("DELETE FROM member_address");
        jdbc.update("DELETE FROM member");
        jdbc.update("UPDATE system_setting SET setting_value = 'false' WHERE setting_key = 'setup_completed'");
        jdbc.update("DELETE FROM system_setting WHERE setting_key = 'onboarding.data-mode'");
    }

    private void product(String title) {
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, ?, 'ACTIVE')",
                UUID.randomUUID(), title);
    }

    @Test
    void blankWipesCommercialDataDuringOnboarding() {
        product("示例商品A");
        product("示例商品B");
        // 同一上下文里其它用例可能already插过商品：只断言"清空前有、清空后为零"
        assertThat(setup.dataStatus().products()).isGreaterThanOrEqualTo(2);

        var status = setup.applyDataMode("BLANK");

        assertThat(status.mode()).isEqualTo("BLANK");
        assertThat(status.products()).isZero();
        // 员工与设置保留（否则后台进不去）
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM system_setting", Long.class)).isPositive();
    }

    @Test
    void blankIsRejectedAfterBusinessStarted() {
        setup.markCompleted();
        product("已开卖商品");
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance,
                                    currency, growth_value)
                VALUES (?, ?, '张三', '13900000001', 'STANDARD', 0, 0, 'CNY', 0)
                """, UUID.randomUUID(), "M-TEST-1");

        assertThatThrownBy(() -> setup.applyDataMode("BLANK"))
                .hasMessageContaining("不能清空为空白商城");
    }

    @Test
    void demoModeRequiresSeederInThisEnvironment() {
        // test profile 没有 dev/demo 的 DemoDataSeeder：必须明确报错，而不是假装安装成功
        assertThatThrownBy(() -> setup.applyDataMode("DEMO"))
                .hasMessageContaining("未启用示例数据种子");
    }

    @Test
    void invalidModeIsRejected() {
        assertThatThrownBy(() -> setup.applyDataMode("WHATEVER"))
                .hasMessageContaining("只能是 DEMO 或 BLANK");
        assertThat(setup.dataStatus()).isNotNull();
    }
}
