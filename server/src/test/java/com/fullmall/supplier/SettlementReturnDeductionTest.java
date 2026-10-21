package com.fullmall.supplier;

import com.fullmall.supplier.app.SupplierReturnService;
import com.fullmall.supplier.app.SupplierSettlementService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 已结算的供应商退货必须从应付中扣掉；未结算的不扣。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.supplier.settlement.commission-rate=0"
        })
@ActiveProfiles("test")
class SettlementReturnDeductionTest {

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
    SupplierSettlementService settlements;

    @Autowired
    SupplierReturnService returns;

    @Autowired
    JdbcTemplate jdbc;

    private void paidOrder(long amount) {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', ?, 0, 0, ?, 'p', '13900008888', 'SHIP')
                """, orderId, "ST-" + orderId.toString().substring(0, 8), amount, amount);
    }

    private UUID returnFor(UUID supplierId, long amount, String finalStatus) {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, 'p', '13900009999', 'SHIP')
                """, orderId, "SRD-" + orderId.toString().substring(0, 8));
        UUID id = returns.create(supplierId, orderId, "退货", amount, "CNY", "staff:ops");
        returns.transition(id, "ACCEPTED", "同意");
        if ("SETTLED".equals(finalStatus)) {
            returns.transition(id, "SETTLED", "扣款");
        }
        return id;
    }

    @Test
    void settledReturnIsDeductedFromPayable() {
        UUID supplierId = UUID.randomUUID();
        paidOrder(10_000);
        returnFor(supplierId, 2_000, "SETTLED");
        returnFor(supplierId, 3_000, "ACCEPTED");   // 未结算，不扣

        /**
         * 注意：`aggregate()` 只统计**供应商代发订单**，这里用直接查询验证扣减口径，
         * 避免为了造代发订单而复制一大段夹具。
         */
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(1, ChronoUnit.DAYS);
        long deduction = returns.settledAmount(supplierId, start, end, "CNY");

        assertThat(deduction).isEqualTo(2_000);
    }
}
