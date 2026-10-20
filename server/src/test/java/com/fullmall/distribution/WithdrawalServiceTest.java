package com.fullmall.distribution;

import com.fullmall.distribution.app.WithdrawalService;
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
class WithdrawalServiceTest {

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
    WithdrawalService withdrawals;

    @Autowired
    JdbcTemplate jdbc;

    private UUID promoter;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM commission_ledger");
        jdbc.update("DELETE FROM withdrawal");
        jdbc.update("DELETE FROM distribution_commission");
        promoter = UUID.randomUUID();
    }

    private UUID availableCommission(long amount) {
        UUID orderId = UUID.randomUUID();
        UUID commissionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '测试', '13800000000', 'SHIP')
                """, orderId, "WD-" + orderId);
        jdbc.update("""
                INSERT INTO distribution_commission (id, order_id, promoter_staff_id, order_amount,
                                                     commission_amount, currency, status, available_at)
                VALUES (?, ?, ?, 1000, ?, 'CNY', 'AVAILABLE', now())
                """, commissionId, orderId, promoter, amount);
        return commissionId;
    }

    @Test
    void cannotRequestMoreThanAvailable() {
        availableCommission(20_000);

        assertThatThrownBy(() -> withdrawals.request(promoter, 20_001, "CNY"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("余额不足");
    }

    @Test
    void pendingWithdrawalReducesWithdrawableBalance() {
        availableCommission(20_000);
        withdrawals.request(promoter, 15_000, "CNY");

        assertThat(withdrawals.availableForWithdrawal(promoter)).isEqualTo(5_000);
        assertThatThrownBy(() -> withdrawals.request(promoter, 20_000, "CNY"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class);
    }

    @Test
    void payingOutMarksCommissionWithdrawnAndWritesLedger() {
        UUID commissionId = availableCommission(20_000);
        UUID id = withdrawals.request(promoter, 20_000, "CNY");
        withdrawals.approve(id, "admin");
        withdrawals.markPaid(id, "WX-REF-1", "admin");

        assertThat(withdrawals.find(id).status()).isEqualTo("PAID");
        assertThat(jdbc.queryForObject(
                "SELECT status FROM distribution_commission WHERE id = ?", String.class, commissionId))
                .isEqualTo("WITHDRAWN");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM commission_ledger WHERE commission_id = ? AND to_status = 'WITHDRAWN'
                """, Integer.class, commissionId)).isEqualTo(1);
    }

    @Test
    void rejectionRequiresReason() {
        availableCommission(20_000);
        UUID id = withdrawals.request(promoter, 12_000, "CNY");

        assertThatThrownBy(() -> withdrawals.reject(id, " ", "admin"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("原因");
    }

    @Test
    void rejectsBelowMinimumAndUnsupportedMethod() {
        availableCommission(20_000);

        // 低于后台默认最低提现额（10000 分）
        assertThatThrownBy(() -> withdrawals.request(promoter, 9_999, "CNY"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("最低提现额");

        // 不在允许集合内的打款方式
        assertThatThrownBy(() -> withdrawals.request(promoter, 12_000, "CNY", "PAYPAL", null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不支持的打款方式");
    }

    @Test
    void recordsChosenPayoutMethod() {
        // 后台开启微信提现后，申请人才可选
        jdbc.update("INSERT INTO system_setting (setting_key, setting_value)"
                + " VALUES ('distribution.payout-methods', 'ALIPAY,WECHAT')"
                + " ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value");
        availableCommission(20_000);

        UUID id = withdrawals.request(promoter, 12_000, "CNY", "WECHAT", "wx-openid-1");

        var row = jdbc.queryForMap(
                "SELECT payout_method, payout_account FROM withdrawal WHERE id = ?", id);
        assertThat(row.get("payout_method")).isEqualTo("WECHAT");
        assertThat(row.get("payout_account")).isEqualTo("wx-openid-1");

        jdbc.update("DELETE FROM system_setting WHERE setting_key = 'distribution.payout-methods'");
    }

    @Test
    void consumesSingleLargeCommissionPartially() {
        // 只有一条 200.00 的大额佣金，申请 120.00：余额不凭空消失，剩余 80 仍可提现
        availableCommission(20_000);
        UUID id = withdrawals.request(promoter, 12_000, "CNY");
        withdrawals.approve(id, "reviewer");

        withdrawals.markPaid(id, "ref-partial", "payer");

        var row = jdbc.queryForMap("""
                SELECT status, commission_amount, consumed_amount
                  FROM distribution_commission WHERE promoter_staff_id = ?
                """, promoter);
        assertThat(((Number) row.get("consumed_amount")).longValue()).isEqualTo(12_000);
        assertThat(row.get("status")).isEqualTo("AVAILABLE");       // 未扣完，仍可用
        assertThat(withdrawals.availableForWithdrawal(promoter)).isEqualTo(8_000);
    }

    @Test
    void fullyConsumedCommissionBecomesWithdrawn() {
        availableCommission(20_000);
        UUID id = withdrawals.request(promoter, 20_000, "CNY");
        withdrawals.approve(id, "reviewer");

        withdrawals.markPaid(id, "ref-full", "payer");

        var row = jdbc.queryForMap("""
                SELECT status, consumed_amount FROM distribution_commission WHERE promoter_staff_id = ?
                """, promoter);
        assertThat(row.get("status")).isEqualTo("WITHDRAWN");
        assertThat(((Number) row.get("consumed_amount")).longValue()).isEqualTo(20_000);
        assertThat(withdrawals.availableForWithdrawal(promoter)).isZero();
    }
}
