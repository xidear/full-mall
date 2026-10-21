package com.fullmall.member;

import com.fullmall.member.auth.MemberAuthService;
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

/** 会员资料返回时区：已设置返回会员的，未设置返回系统默认。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class MemberProfileTimezoneTest {

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
    MemberAuthService auth;

    @Autowired
    JdbcTemplate jdbc;

    private UUID member(String timezone) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency, timezone)
                VALUES (?, ?, '时区会员', ?, 'STANDARD', 0, 0, 'CNY', ?)
                """, id, "M-TZ-" + id.toString().substring(0, 8),
                "138" + id.toString().replaceAll("[^0-9]", "6").substring(0, 8), timezone);
        return id;
    }

    @Test
    void returnsOwnTimezoneOrSystemDefault() {
        UUID tokyo = member("Asia/Tokyo");
        assertThat(auth.profile(tokyo).timezone()).isEqualTo("Asia/Tokyo");

        UUID noZone = member(null);
        String fallback = auth.profile(noZone).timezone();
        assertThat(fallback).isNotBlank();
        // 未设置时返回系统默认（服务器时区或 app.timezone），必须能解析
        assertThat(java.time.ZoneId.of(fallback)).isNotNull();
    }
}
