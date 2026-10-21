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

/**
 * 绑定策略（用户口径 2026-10-19）：
 * ONCE = 后到覆盖先前；PERMANENT = 已有上级则不覆盖。
 * **已结算的佣金永不追回**——覆盖只影响之后的订单。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class BindingPolicyTest {

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

    private void config(String mode, String timing) {
        jdbc.update("INSERT INTO system_setting (setting_key, setting_value) VALUES"
                + " ('distribution.bind-mode', ?) ON CONFLICT (setting_key)"
                + " DO UPDATE SET setting_value = EXCLUDED.setting_value", mode);
        jdbc.update("INSERT INTO system_setting (setting_key, setting_value) VALUES"
                + " ('distribution.bind-timing', ?) ON CONFLICT (setting_key)"
                + " DO UPDATE SET setting_value = EXCLUDED.setting_value", timing);
    }

    private UUID order(String phone) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 5000, 0, 0, 5000, 'p', ?, 'SHIP')
                """, id, "BP-" + id.toString().substring(0, 8), phone);
        return id;
    }

    private UUID promoter(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO staff_user (id, username, display_name, password_hash, role, status)
                VALUES (?, ?, ?, 'x', 'OPERATOR', 'ACTIVE')
                """, id, name + id.toString().substring(0, 6), name);
        return id;
    }

    private UUID activePromoter(String phone) {
        var rows = jdbc.query("SELECT promoter_staff_id FROM referral_binding WHERE customer_phone = ?"
                + " AND active = TRUE ORDER BY created_at DESC",
                (rs, i) -> rs.getObject("promoter_staff_id", UUID.class), phone);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Test
    void onceModeLetsLaterPromoterOverride() {
        config("ONCE", "CART");
        String phone = "13700000001";
        UUID first = promoter("甲");
        UUID second = promoter("乙");

        referrals.attachCheckout(order(phone), phone, first, null);
        assertThat(activePromoter(phone)).isEqualTo(first);

        referrals.attachCheckout(order(phone), phone, second, null);
        assertThat(activePromoter(phone))
                .as("ONCE：后到的推广员覆盖先前的")
                .isEqualTo(second);
    }

    @Test
    void permanentModeKeepsExistingPromoter() {
        config("PERMANENT", "CART");
        String phone = "13700000002";
        UUID first = promoter("丙");
        UUID second = promoter("丁");

        referrals.attachCheckout(order(phone), phone, first, null);
        referrals.attachCheckout(order(phone), phone, second, null);

        assertThat(activePromoter(phone))
                .as("PERMANENT：已有上级则不被覆盖")
                .isEqualTo(first);
    }

    @Test
    void overrideNeverTouchesSettledCommission() {
        config("ONCE", "CART");
        String phone = "13700000003";
        UUID first = promoter("戊");
        UUID second = promoter("己");
        UUID firstOrder = order(phone);
        referrals.attachCheckout(firstOrder, phone, first, null);

        // 第一单佣金已结算
        UUID commissionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO distribution_commission (id, order_id, promoter_staff_id, order_amount,
                                                     commission_amount, currency, status, payee_channel,
                                                     settled_at)
                VALUES (?, ?, ?, 5000, 500, 'CNY', 'WITHDRAWN', 'PAYROLL', now())
                """, commissionId, firstOrder, first);

        // 第二个推广员带来新订单并覆盖绑定
        referrals.attachCheckout(order(phone), phone, second, null);

        assertThat(jdbc.queryForObject("SELECT status FROM distribution_commission WHERE id = ?",
                String.class, commissionId))
                .as("已结算佣金不因绑定覆盖而被追回")
                .isEqualTo("WITHDRAWN");
        assertThat(jdbc.queryForObject(
                "SELECT commission_amount FROM distribution_commission WHERE id = ?",
                Long.class, commissionId)).isEqualTo(500L);
    }
}
