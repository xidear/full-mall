package com.fullmall.member;

import com.fullmall.member.app.BirthdayGiftService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 生日礼按**会员所在时区**判定：同一时刻，UTC+14 已是次日、UTC-8 仍是当天。
 * 不顺延补发：非本地生日当天不发放。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class BirthdayGiftTest {

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
    BirthdayGiftService gifts;

    @Autowired
    JdbcTemplate jdbc;

    private UUID member(String tier, LocalDate birthday, String timezone) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency, birthday, timezone)
                VALUES (?, ?, '生日会员', ?, ?, 0, 0, 'CNY', ?, ?)
                """, id, "M-BD-" + id.toString().substring(0, 8),
                "137" + id.toString().replaceAll("[^0-9]", "3").substring(0, 8), tier,
                java.sql.Date.valueOf(birthday), timezone);
        return id;
    }

    @Test
    void usesMemberTimezoneNotServerTimezone() {
        // 2026-10-20T15:00Z：UTC+14 已是 10-21；UTC-8 仍是 10-20
        Instant now = Instant.parse("2026-10-20T15:00:00Z");
        UUID kiritimati = member("GOLD", LocalDate.of(1990, 10, 21), "Pacific/Kiritimati");
        UUID losAngeles = member("GOLD", LocalDate.of(1990, 10, 20), "America/Los_Angeles");
        UUID neither = member("GOLD", LocalDate.of(1990, 10, 22), "Asia/Shanghai");

        var granted = gifts.grantAt(now);

        assertThat(granted).anySatisfy(item -> assertThat(item.memberId()).isEqualTo(kiritimati));
        assertThat(granted).anySatisfy(item -> assertThat(item.memberId()).isEqualTo(losAngeles));
        assertThat(granted).noneSatisfy(item -> assertThat(item.memberId()).isEqualTo(neither));
    }

    @Test
    void doesNotPostponeOrDoubleGrant() {
        Instant now = Instant.parse("2026-10-20T15:00:00Z");
        UUID memberId = member("GOLD", LocalDate.of(1990, 10, 20), "Asia/Shanghai");

        assertThat(gifts.grantAt(now))
                .anySatisfy(item -> assertThat(item.memberId()).isEqualTo(memberId));
        long balance = jdbc.queryForObject(
                "SELECT points_balance FROM member WHERE id = ?", Long.class, memberId);

        // 同一秒再跑：不重复发放
        gifts.grantAt(now);
        assertThat(jdbc.queryForObject("SELECT points_balance FROM member WHERE id = ?",
                Long.class, memberId)).isEqualTo(balance);

        // 次日再跑：不顺延补发（本地日期已不是生日）
        assertThat(gifts.grantAt(now.plusSeconds(86400)))
                .noneSatisfy(item -> assertThat(item.memberId()).isEqualTo(memberId));
    }
}