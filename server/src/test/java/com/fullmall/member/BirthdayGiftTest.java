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

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 生日礼：当天发放一次，重复执行不重复入账。 */
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

    private UUID member(String tier, LocalDate birthday) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency, birthday)
                VALUES (?, ?, '生日会员', ?, ?, 0, 0, 'CNY', ?)
                """, id, "M-BD-" + id.toString().substring(0, 8),
                "137" + id.toString().replaceAll("[^0-9]", "3").substring(0, 8), tier,
                java.sql.Date.valueOf(birthday));
        return id;
    }

    @Test
    void grantsOnceOnBirthday() {
        LocalDate today = LocalDate.now(BirthdayGiftService.BUSINESS_OFFSET);
        UUID birthdayMember = member("GOLD", today.minusYears(20));
        UUID otherDay = member("GOLD", today.minusYears(20).plusDays(1));

        var granted = gifts.grantForDate(today);

        assertThat(granted).anySatisfy(item -> assertThat(item.memberId()).isEqualTo(birthdayMember));
        assertThat(granted).noneSatisfy(item -> assertThat(item.memberId()).isEqualTo(otherDay));
        long balance = jdbc.queryForObject(
                "SELECT points_balance FROM member WHERE id = ?", Long.class, birthdayMember);
        assertThat(balance).isPositive();

        // 再跑一次：不重复发放
        assertThat(gifts.grantForDate(today)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT points_balance FROM member WHERE id = ?",
                Long.class, birthdayMember)).isEqualTo(balance);
    }
}
