package com.fullmall.member;

import com.fullmall.member.app.MemberPointsService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 积分按笔 FIFO 扣减：先扣最早的，扣不完继续下一笔。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class PointsFifoTest {

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
    MemberPointsService points;

    @Autowired
    JdbcTemplate jdbc;

    private static final String PHONE = "13700001111";
    private UUID memberId;

    private void earn(long amount, String key) {
        jdbc.update("""
                INSERT INTO points_ledger (id, member_id, sale_id, entry_type, points,
                                           balance_after, reason, business_key, remaining_points, created_at)
                VALUES (?, ?, NULL, 'EARN', ?, ?, '测试获得', ?, ?, now() + (? || ' seconds')::interval)
                """, UUID.randomUUID(), memberId, amount, amount, key, amount, nextOffset());
        jdbc.update("UPDATE member SET points_balance = points_balance + ? WHERE id = ?", amount, memberId);
    }

    private int offset = 0;
    private int nextOffset() { return ++offset; }

    private void member(long balance) {
        memberId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency)
                VALUES (?, ?, '积分用户', ?, 'STANDARD', ?, 0, 'CNY')
                """, memberId, "M-FIFO-" + memberId.toString().substring(0, 8), PHONE, balance);
    }

    @Test
    void consumesOldestEntriesFirst() {
        member(0);
        earn(100, "fifo:a");
        earn(50, "fifo:b");

        // 抵扣 120：先扣最早的 100，再从第二笔扣 20
        points.redeem(PHONE, UUID.randomUUID(), 120, 100_000);

        List<Long> remaining = jdbc.queryForList("""
                SELECT remaining_points FROM points_ledger
                 WHERE member_id = ? AND entry_type = 'EARN' ORDER BY created_at
                """, Long.class, memberId);
        assertThat(remaining).containsExactly(0L, 30L);
        assertThat(jdbc.queryForObject("SELECT points_balance FROM member WHERE id = ?",
                Long.class, memberId)).isEqualTo(30L);
    }
}
