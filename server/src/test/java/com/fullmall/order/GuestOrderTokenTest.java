package com.fullmall.order;

import com.fullmall.platform.identity.OrderOwnershipGuard;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
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

/** 游客订单：仅凭订单号不能看履约；令牌或联系人手机号才放行。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class GuestOrderTokenTest {

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

    private UUID guestOrder(String token) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone,
                                    fulfillment_mode, access_token_hash)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '游客', '13900005555', 'PICKUP', ?)
                """, id, "GUEST-" + id.toString().substring(0, 8),
                token == null ? null : OrderOwnershipGuard.hashToken(token));
        return id;
    }

    @Test
    void orderIdAloneIsNotEnough() {
        UUID orderId = guestOrder("secret-token");

        assertThatThrownBy(() -> guard.requireAccess(orderId, null, null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("手机号");
    }

    @Test
    void matchingTokenGrantsAccess() {
        UUID orderId = guestOrder("secret-token");

        assertThatCode(() -> guard.requireAccess(orderId, null, null, "secret-token"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.requireAccess(orderId, null, null, "wrong-token"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class);
    }

    @Test
    void contactPhoneStillWorksForGuestOrders() {
        UUID orderId = guestOrder("secret-token");

        assertThatCode(() -> guard.requireAccess(orderId, null, "13900005555"))
                .doesNotThrowAnyException();
    }
}
