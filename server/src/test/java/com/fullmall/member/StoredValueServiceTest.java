package com.fullmall.member;

import com.fullmall.member.app.StoredValueService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 储值充值：幂等键防重复、金额校验、流水与余额一致。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.stored-value.enabled=true"
        })
@ActiveProfiles("test")
class StoredValueServiceTest {

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
    StoredValueService storedValue;

    @Autowired
    JdbcTemplate jdbc;

    private UUID member() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency)
                VALUES (?, ?, '储值用户', ?, 'STANDARD', 0, 0, 'CNY')
                """, id, "M-SV-" + id.toString().substring(0, 8),
                "137" + id.toString().replaceAll("[^0-9]", "3").substring(0, 8));
        return id;
    }

    @Test
    void topUpIsIdempotentAndWritesLedger() {
        UUID memberId = member();

        var first = storedValue.topUp(memberId, 10_000, "topup-1", "门店充值");
        assertThat(first.balance()).isEqualTo(10_000);
        assertThat(first.duplicated()).isFalse();

        // 同一幂等键重复提交：余额不变，返回 duplicated
        var second = storedValue.topUp(memberId, 10_000, "topup-1", "重复点击");
        assertThat(second.balance()).isEqualTo(10_000);
        assertThat(second.duplicated()).isTrue();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM stored_value_ledger WHERE member_id = ?", Integer.class, memberId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT stored_value_balance FROM member WHERE id = ?", Long.class, memberId))
                .isEqualTo(10_000L);
    }

    @Test
    void invalidAmountIsRejected() {
        UUID memberId = member();
        assertThatThrownBy(() -> storedValue.topUp(memberId, 0, "k", null))
                .hasMessageContaining("必须大于 0");
        assertThatThrownBy(() -> storedValue.topUp(memberId, 100, "", null))
                .hasMessageContaining("幂等键");
    }

    /** 开关默认关闭：未取得预付卡资质时不得开放充值。 */
    @Test
    void topUpIsRejectedWhenSwitchIsOff() {
        var off = new StoredValueService(jdbc, false, "储值功能需资质，默认关闭");
        UUID memberId = member();

        assertThatThrownBy(() -> off.topUp(memberId, 1_000, "k-off", null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("需资质");
        assertThat(jdbc.queryForObject(
                "SELECT stored_value_balance FROM member WHERE id = ?", Long.class, memberId))
                .isZero();
    }
}
