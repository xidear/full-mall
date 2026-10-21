package com.fullmall.platform.schedule;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 定时任务注册表：cron 存库、可启停、可手动触发，非法 cron 明确拒绝。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.scheduling.enabled=true"
        })
@ActiveProfiles("test")
class ScheduledTaskServiceTest {

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
    ScheduledTaskService tasks;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void registersDefaultCronAndCanBeUpdated() {
        var before = tasks.find("maintenance.retention");
        assertThat(before.cron()).isEqualTo("0 30 4 * * *");
        assertThat(before.enabled()).isFalse();
        assertThat(before.nextRunAt()).isNotNull();

        var updated = tasks.update("maintenance.retention", "0 0 5 * * *", true);

        assertThat(updated.cron()).isEqualTo("0 0 5 * * *");
        assertThat(updated.enabled()).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT cron FROM scheduled_task WHERE code = 'maintenance.retention'", String.class))
                .isEqualTo("0 0 5 * * *");

        // 复位，避免影响其它用例
        tasks.update("maintenance.retention", "0 30 4 * * *", false);
    }

    @Test
    void rejectsInvalidCron() {
        assertThatThrownBy(() -> tasks.update("maintenance.retention", "不是cron", true))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("cron 非法");
        assertThatThrownBy(() -> tasks.update("maintenance.retention", " ", true))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void unknownTaskIsRejected() {
        assertThatThrownBy(() -> tasks.runNow("no.such.task"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("未注册的任务");
    }

    @Test
    void allRetentionTasksAreRegistered() {
        var codes = tasks.list().stream().map(ScheduledTaskService.TaskView::code).toList();

        assertThat(codes).contains(
                "maintenance.retention",
                "notification.retention",
                "export.retention",
                "distribution.commission-release",
                "export.worker",
                "order.expiry",
                "inventory.reservation-release",
                "promotion.group-buy-expiry",
                "audit-outbox-recovery",
                "member.birthday-gift",
                "member.points-expiry");
        // ai.knowledge-sync 只在企业版存在（社区版不含 AI 类），因此按 edition 区分条数
        String edition = System.getenv("APP_EDITION");
        boolean enterprise = "enterprise".equalsIgnoreCase(edition == null ? "" : edition.trim());
        assertThat(codes).hasSize(enterprise ? 12 : 11);
    }

    @Test
    void businessCriticalTasksEnabledByDefault() {
        // 订单超时/库存释放/拼团过期/佣金解冻：不跑会导致库存被永久占用或佣金永远冻结
        for (String code : new String[]{"order.expiry", "inventory.reservation-release",
                "promotion.group-buy-expiry", "distribution.commission-release"}) {
            assertThat(tasks.find(code).enabled()).as(code).isTrue();
        }
        // 破坏性/重型任务默认关闭（导出处理已移到上面的必开列表）
        for (String code : new String[]{"maintenance.retention", "notification.retention",
                "export.retention"}) {
            assertThat(tasks.find(code).enabled()).as(code).isFalse();
        }
    }

    @Test
    void migratedTasksRunAndReportSuccess() {
        var notification = tasks.runNow("notification.retention");
        var export = tasks.runNow("export.retention");

        assertThat(notification.lastStatus()).isEqualTo("SUCCESS");
        assertThat(export.lastStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void runNowRecordsStatus() {
        var result = tasks.runNow("maintenance.retention");

        assertThat(result.lastRunAt()).isNotNull();
        assertThat(result.lastStatus()).isEqualTo("SUCCESS");
        assertThat(result.lastDurationMs()).isNotNull();
    }
}
