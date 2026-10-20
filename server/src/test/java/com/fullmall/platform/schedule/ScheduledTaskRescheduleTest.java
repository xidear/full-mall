package com.fullmall.platform.schedule;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 改 cron 后**不能出现重复执行**。
 *
 * <p>这是迁移高危任务（订单超时、库存释放）前的保护性验证：
 * 若改 cron 时旧调度没被取消，重置后每秒会跑两次，业务上等同"重复释放库存"。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.scheduling.enabled=true"
        })
@ActiveProfiles("test")
class ScheduledTaskRescheduleTest {

    private static final EmbeddedPostgres POSTGRES;
    static final AtomicInteger RUNS = new AtomicInteger();

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

    @TestConfiguration
    static class CountingJobConfig {
        @Bean
        ScheduledJob countingJob() {
            return new ScheduledJob() {
                @Override
                public String code() {
                    return "test.counting";
                }

                @Override
                public String name() {
                    return "计数任务（测试用）";
                }

                @Override
                public String defaultCron() {
                    return "*/1 * * * * *";
                }

                @Override
                public boolean enabledByDefault() {
                    return false;
                }

                @Override
                public void run() {
                    RUNS.incrementAndGet();
                }
            };
        }
    }

    @Autowired
    ScheduledTaskService tasks;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void reschedulingDoesNotDuplicateExecution() throws Exception {
        tasks.update("test.counting", "*/1 * * * * *", false);
        RUNS.set(0);

        // 1) 启用后应周期性执行
        tasks.update("test.counting", "*/1 * * * * *", true);
        Thread.sleep(3500);
        int afterEnable = RUNS.get();
        assertThat(afterEnable).as("启用后应有执行").isGreaterThanOrEqualTo(2);

        // 2) 改成同一 cron（触发重新注册）——执行频率不应翻倍
        tasks.update("test.counting", "*/1 * * * * *", true);
        int beforeReschedule = RUNS.get();
        Thread.sleep(3500);
        int deltaAfterReschedule = RUNS.get() - beforeReschedule;
        assertThat(deltaAfterReschedule)
                .as("重新注册后不应翻倍执行（旧调度必须取消）")
                .isLessThanOrEqualTo(5);

        // 3) 关闭后应停止
        tasks.update("test.counting", "*/1 * * * * *", false);
        int beforeDisable = RUNS.get();
        Thread.sleep(2500);
        int deltaAfterDisable = RUNS.get() - beforeDisable;
        assertThat(deltaAfterDisable)
                .as("关闭后最多有 1 次在途执行")
                .isLessThanOrEqualTo(1);
    }
}
