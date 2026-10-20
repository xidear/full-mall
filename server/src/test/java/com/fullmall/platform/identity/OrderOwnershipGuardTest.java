package com.fullmall.platform.identity;

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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 订单归属校验：会员订单只允许本人；游客订单必须带联系人手机号。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class OrderOwnershipGuardTest {

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
    OrderOwnershipGuard guard;

    @Autowired
    JdbcTemplate jdbc;

    private UUID guestOrder;
    private UUID memberOrder;
    private UUID memberId;
    private String phone = "13977770001";

    @BeforeEach
    void seed() {
        memberId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, ?, '归属用户', ?, 'STANDARD', 0, 0, 'CNY')
                """, memberId, "M-" + memberId.toString().substring(0, 8), "138" + memberId.toString().substring(0, 8));

        guestOrder = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '游客', ?, 'SHIP')
                """, guestOrder, "GUARD-" + guestOrder, phone);

        memberOrder = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode,
                                    member_id)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '会员', '13900000000', 'SHIP', ?)
                """, memberOrder, "GUARD-" + memberOrder, memberId);
    }

    @Test
    void guestOrderRequiresMatchingPhone() {
        assertThatCode(() -> guard.requireAccess(guestOrder, null, phone)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.requireAccess(guestOrder, null, "13900000000"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("需要订单联系人手机号");
        assertThatThrownBy(() -> guard.requireAccess(guestOrder, null, null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class);
    }

    @Test
    void memberOrderAllowsOnlyOwner() {
        assertThatCode(() -> guard.requireAccess(memberOrder, memberId, null)).doesNotThrowAnyException();
        // 其他会员 → 404（不暴露存在性）
        assertThatThrownBy(() -> guard.requireAccess(memberOrder, UUID.randomUUID(), null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("订单不存在");
        // 游客身份访问会员订单 → 404
        assertThatThrownBy(() -> guard.requireAccess(memberOrder, null, "13900000000"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    void unknownOrderIsNotFound() {
        assertThatThrownBy(() -> guard.requireAccess(UUID.randomUUID(), memberId, phone))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("订单不存在");
    }
}
