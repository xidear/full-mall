package com.fullmall.member;

import com.fullmall.member.app.PointsExpiryService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 积分过期：默认永不过期；开启后按逐笔余量扣，余额不为负，非法配置报错。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class PointsExpiryTest {

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
    PointsExpiryService expiry;

    @Autowired
    JdbcTemplate jdbc;

    private UUID member(long balance) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency)
                VALUES (?, ?, '过期用户', ?, 'STANDARD', ?, 0, 'CNY')
                """, id, "M-EXP-" + id.toString().substring(0, 8),
                "135" + id.toString().replaceAll("[^0-9]", "7").substring(0, 8), balance);
        return id;
    }

    private UUID earn(UUID memberId, long points, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO points_ledger (id, member_id, sale_id, entry_type, points,
                                           balance_after, reason, business_key,
                                           remaining_points, created_at)
                VALUES (?, ?, NULL, 'EARN', ?, ?, '测试', ?, ?, ?)
                """, id, memberId, points, points, "earn:" + id, points,
                java.sql.Timestamp.from(createdAt));
        return id;
    }

    private void setting(String key, String value) {
        jdbc.update("INSERT INTO system_setting (setting_key, setting_value) VALUES (?, ?)"
                + " ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value",
                key, value);
    }

    @Test
    void neverModeExpiresNothing() {
        setting(PointsExpiryService.MODE_KEY, "NEVER");
        UUID memberId = member(100);
        UUID ledgerId = earn(memberId, 100, Instant.now().minus(400, ChronoUnit.DAYS));

        assertThat(expiry.expireNow(Instant.now())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT remaining_points FROM points_ledger WHERE id = ?",
                Long.class, ledgerId)).isEqualTo(100);
    }

    @Test
    void rollingDaysExpiresOnlyOldEntries() {
        setting(PointsExpiryService.MODE_KEY, "ROLLING_DAYS");
        setting(PointsExpiryService.DAYS_KEY, "30");
        UUID memberId = member(150);
        UUID oldLedger = earn(memberId, 100, Instant.now().minus(40, ChronoUnit.DAYS));
        UUID newLedger = earn(memberId, 50, Instant.now().minus(2, ChronoUnit.DAYS));

        var expired = expiry.expireNow(Instant.now());

        assertThat(expired).anySatisfy(item -> assertThat(item.memberId()).isEqualTo(memberId));
        assertThat(jdbc.queryForObject("SELECT remaining_points FROM points_ledger WHERE id = ?",
                Long.class, oldLedger)).isZero();
        assertThat(jdbc.queryForObject("SELECT remaining_points FROM points_ledger WHERE id = ?",
                Long.class, newLedger)).isEqualTo(50);
        assertThat(jdbc.queryForObject("SELECT points_balance FROM member WHERE id = ?",
                Long.class, memberId)).isEqualTo(50);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM points_ledger WHERE member_id = ? AND entry_type = 'EXPIRE'",
                Integer.class, memberId)).isEqualTo(1);
    }

    @Test
    void invalidConfigurationFailsLoudly() {
        setting(PointsExpiryService.MODE_KEY, "SOMETIMES");
        assertThatThrownBy(() -> expiry.settings())
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("NEVER / ROLLING_DAYS / FIFO");

        setting(PointsExpiryService.MODE_KEY, "ROLLING_DAYS");
        setting(PointsExpiryService.DAYS_KEY, "0");
        assertThatThrownBy(() -> expiry.settings())
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("必须大于 0");

        setting(PointsExpiryService.MODE_KEY, "NEVER");
    }
}
