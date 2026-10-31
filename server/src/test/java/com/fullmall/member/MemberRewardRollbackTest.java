package com.fullmall.member;

import com.fullmall.member.app.MemberRewardService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class MemberRewardRollbackTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final String PHONE = "13922220000";

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
    MemberRewardService rewards;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM points_ledger WHERE member_id IN (SELECT id FROM member WHERE phone = ?)", PHONE);
        jdbc.update("DELETE FROM growth_ledger WHERE member_id IN (SELECT id FROM member WHERE phone = ?)", PHONE);
        jdbc.update("DELETE FROM member WHERE phone = ?", PHONE);
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M-RB-1', '回退用户', ?, 'GOLD', 0, 0, 'CNY')
                """, UUID.randomUUID(), PHONE);
    }

    @Test
    void refundReversesEarnedPointsAndGrowthOnce() {
        UUID orderId = UUID.randomUUID();
        var earned = rewards.accrue(orderId, PHONE, 1_900, "CNY");
        assertThat(earned.points()).isEqualTo(38);
        assertThat(earned.growth()).isEqualTo(19);

        var reversed = rewards.rollback(orderId, PHONE);

        assertThat(reversed.points()).isEqualTo(38);
        assertThat(reversed.growth()).isEqualTo(19);
        assertThat(jdbc.queryForObject(
                "SELECT points_balance FROM member WHERE phone = ?", Long.class, PHONE)).isZero();
        // 幂等：重复回退不再产生流水
        assertThat(rewards.rollback(orderId, PHONE).points()).isZero();
    }

    @Test
    void proportionalRollbackReversesShareOfEarnedPoints() {
        UUID orderId = UUID.randomUUID();
        // GOLD 2 倍：2000 分实付 → 40 积分 / 20 成长值
        rewards.accrue(orderId, PHONE, 2_000, "CNY");

        // 退一半（1000/2000）→ 积分 20、成长值 10
        var reversed = rewards.rollbackProportional(orderId, PHONE, 1_000, 2_000);

        assertThat(reversed.points()).isEqualTo(20);
        assertThat(reversed.growth()).isEqualTo(10);
        assertThat(jdbc.queryForObject(
                "SELECT points_balance FROM member WHERE phone = ?", Long.class, PHONE))
                .isEqualTo(20);
    }

    @Test
    void proportionalRollbackRoundsDown() {
        UUID orderId = UUID.randomUUID();
        // GOLD 2 倍：1900 分实付 → 38 积分 / 19 成长值
        rewards.accrue(orderId, PHONE, 1_900, "CNY");

        // 退 1/3：38*633/1900 = 12.66 → 向下取整 12（不多退）
        var reversed = rewards.rollbackProportional(orderId, PHONE, 1_900 / 3, 1_900);

        assertThat(reversed.points()).isEqualTo(12);
    }

    @Test
    void rollbackNeverDrivesBalanceNegative() {
        UUID orderId = UUID.randomUUID();
        rewards.accrue(orderId, PHONE, 1_900, "CNY");
        // 用户已把积分花掉
        jdbc.update("UPDATE member SET points_balance = 0 WHERE phone = ?", PHONE);

        var reversed = rewards.rollback(orderId, PHONE);

        assertThat(reversed.points()).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT points_balance FROM member WHERE phone = ?", Long.class, PHONE)).isZero();
    }
}
