package com.fullmall.bargain.api;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 参数越界必须 400 带说明，而不是 500（DB 约束违规被当内部错误）。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class BargainValidationTest {

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
    JdbcTemplate jdbc;

    private AdminBargainController.CreateRequest request(int minHelpers, int expiresHours) {
        return new AdminBargainController.CreateRequest(UUID.randomUUID(), 100, 5000, minHelpers,
                50, 100, expiresHours, true, "测试", Instant.now(), Instant.now().plusSeconds(3600));
    }

    @Test
    void rejectsOutOfRangeWithBadRequest() {
        var controller = new AdminBargainController(jdbc);

        assertThatThrownBy(() -> controller.create(request(2, 24)))
                .hasMessageContaining("助力人数必须在 3–30");
        assertThatThrownBy(() -> controller.create(request(3, 5)))
                .hasMessageContaining("有效期必须在 12–72");
    }
}
