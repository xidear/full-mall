package com.fullmall.member;

import com.fullmall.member.app.MemberPointsService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class MemberPointsServiceTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final String PHONE = "13911110000";

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
    MemberPointsService points;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM points_ledger WHERE member_id IN (SELECT id FROM member WHERE phone = ?)", PHONE);
        jdbc.update("DELETE FROM member WHERE phone = ?", PHONE);
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M-PTS-1', '积分用户', ?, 'GOLD', 500, 0, 'CNY')
                """, UUID.randomUUID(), PHONE);
    }

    @Test
    void redeemDeductsPointsAndRecordsLedger() {
        UUID orderId = UUID.randomUUID();
        var redemption = points.redeem(PHONE, orderId, 200, 1_000);

        assertThat(redemption.pointsUsed()).isEqualTo(200);
        assertThat(redemption.amount()).isEqualTo(200);
        assertThat(jdbc.queryForObject(
                "SELECT points_balance FROM member WHERE phone = ?", Long.class, PHONE)).isEqualTo(300);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM points_ledger WHERE business_key = ?", Integer.class,
                "order:" + orderId + ":redeem")).isEqualTo(1);
    }

    @Test
    void cannotExceedBalance() {
        assertThatThrownBy(() -> points.redeem(PHONE, UUID.randomUUID(), 501, 10_000))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("积分不足");
    }

    @Test
    void redemptionIsCappedByPayableAmount() {
        // 应付 50 分 → 最多抵 50 分，即使请求 200
        var redemption = points.redeem(PHONE, UUID.randomUUID(), 200, 50);

        assertThat(redemption.pointsUsed()).isEqualTo(50);
        assertThat(redemption.amount()).isEqualTo(50);
    }

    @Test
    void sameOrderCannotRedeemTwice() {
        UUID orderId = UUID.randomUUID();
        points.redeem(PHONE, orderId, 100, 1_000);

        assertThatThrownBy(() -> points.redeem(PHONE, orderId, 100, 1_000))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("已使用过积分");
    }

    @Test
    void rollbackReturnsPointsOnce() {
        UUID orderId = UUID.randomUUID();
        points.redeem(PHONE, orderId, 200, 1_000);

        assertThat(points.rollbackForOrder(orderId, PHONE)).isEqualTo(200);
        assertThat(points.rollbackForOrder(orderId, PHONE)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT points_balance FROM member WHERE phone = ?", Long.class, PHONE)).isEqualTo(500);
    }
}
