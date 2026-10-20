package com.fullmall.distribution;

import com.fullmall.distribution.app.ReferralService;
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

/** 绑定策略生效：一次性/终身、注册时/结算时，必须真的影响归属，而不是摆设配置。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ReferralBindingPolicyTest {

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
    ReferralService referrals;

    @Autowired
    JdbcTemplate jdbc;

    private void setting(String key, String value) {
        jdbc.update("INSERT INTO system_setting (setting_key, setting_value) VALUES (?, ?)"
                + " ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value", key, value);
    }

    private UUID order(String phone) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 10000, 0, 0, 10000, 'p', ?, 'SHIP')
                """, id, "BIND-" + id.toString().substring(0, 8), phone);
        return id;
    }

    private UUID orderReferrer(UUID orderId) {
        var rows = jdbc.queryForList(
                "SELECT referrer_staff_id FROM orders WHERE id = ?", UUID.class, orderId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Test
    void onceBindingKeepsFirstPromoter() {
        setting("distribution.bind-mode", "ONCE");
        setting("distribution.bind-timing", "CART");
        String phone = "139" + UUID.randomUUID().toString().substring(0, 8).replaceAll("[^0-9]", "1");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        UUID order1 = order(phone);
        referrals.attachCheckout(order1, phone, first, "ORDER");
        UUID order2 = order(phone);
        referrals.attachCheckout(order2, phone, second, "ORDER");

        assertThat(orderReferrer(order1)).isEqualTo(first);
        assertThat(orderReferrer(order2)).isEqualTo(first);   // 一次性绑定：后来者不抢
    }

    @Test
    void permanentBindingLetsNewPromoterTakeOver() {
        setting("distribution.bind-mode", "PERMANENT");
        setting("distribution.bind-timing", "CART");
        String phone = "137" + UUID.randomUUID().toString().substring(0, 8).replaceAll("[^0-9]", "1");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        UUID order1 = order(phone);
        referrals.attachCheckout(order1, phone, first, "PERMANENT");
        UUID order2 = order(phone);
        referrals.attachCheckout(order2, phone, second, "PERMANENT");

        assertThat(orderReferrer(order1)).isEqualTo(first);
        assertThat(orderReferrer(order2)).isEqualTo(second);  // 终身绑定：新推广员接管
    }

    @Test
    void registerTimingIgnoresCheckoutReferrer() {
        setting("distribution.bind-mode", "PERMANENT");
        setting("distribution.bind-timing", "REGISTER");
        String phone = "136" + UUID.randomUUID().toString().substring(0, 8).replaceAll("[^0-9]", "1");

        UUID orderId = order(phone);
        referrals.attachCheckout(orderId, phone, UUID.randomUUID(), "ORDER");

        assertThat(orderReferrer(orderId)).isNull();   // 只在注册时绑定，结算不建关系
    }

    @Test
    void secondLevelCommissionIsCreatedFromUpline() {
        setting("distribution.bind-mode", "PERMANENT");
        setting("distribution.bind-timing", "CART");
        setting("distribution.level1-value", "5");
        setting("distribution.level2-value", "2");
        String phone = "135" + UUID.randomUUID().toString().substring(0, 8).replaceAll("[^0-9]", "1");
        UUID promoter = UUID.randomUUID();
        UUID upline = UUID.randomUUID();

        UUID orderId = order(phone);
        referrals.attachCheckout(orderId, phone, promoter, "ORDER");
        referrals.linkUpline("STAFF", promoter, "STAFF", upline);
        referrals.settleOrder(orderId, 10_000, "CNY");

        var rows = jdbc.queryForList("""
                SELECT promoter_staff_id, level, commission_amount
                  FROM distribution_commission WHERE order_id = ? ORDER BY level
                """, orderId);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("promoter_staff_id")).isEqualTo(promoter);
        assertThat(((Number) rows.get(0).get("commission_amount")).longValue()).isEqualTo(500);
        assertThat(rows.get(1).get("promoter_staff_id")).isEqualTo(upline);
        assertThat(((Number) rows.get(1).get("commission_amount")).longValue()).isEqualTo(200);
    }
}
