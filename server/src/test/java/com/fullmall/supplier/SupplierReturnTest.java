package com.fullmall.supplier;

import com.fullmall.supplier.app.SupplierReturnService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 供应商售后状态机：REQUESTED → ACCEPTED/REJECTED，ACCEPTED → SETTLED，其它流转一律拒绝。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SupplierReturnTest {

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
    SupplierReturnService returns;

    @Autowired
    JdbcTemplate jdbc;

    private UUID order() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 5000, 0, 0, 5000, 'p', '13900007777', 'SHIP')
                """, id, "SR-" + id.toString().substring(0, 8));
        return id;
    }

    @Test
    void allowsExpectedTransitionsAndClosesOnTerminalStates() {
        UUID supplierId = UUID.randomUUID();
        UUID id = returns.create(supplierId, order(), "质量问题", 2000, "CNY", "staff:ops");

        returns.transition(id, "ACCEPTED", "同意折让");
        returns.transition(id, "SETTLED", "已从结算扣减");

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, closed_at FROM supplier_return WHERE id = ?", id);
        assertThat(row.get("status")).isEqualTo("SETTLED");
        assertThat(row.get("closed_at")).isNotNull();
    }

    @Test
    void rejectsInvalidTransitionsAndBadInput() {
        UUID id = returns.create(UUID.randomUUID(), order(), "少件", 1000, "CNY", "staff:ops");

        // REQUESTED 不能直接结算
        assertThatThrownBy(() -> returns.transition(id, "SETTLED", null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不允许从 REQUESTED 变为 SETTLED");

        // 未知状态
        assertThatThrownBy(() -> returns.transition(id, "DONE", null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("状态只能是");

        // 缺原因 / 金额为负
        assertThatThrownBy(() -> returns.create(UUID.randomUUID(), order(), " ", 100, "CNY", "staff:ops"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("退货原因");
        assertThatThrownBy(() -> returns.create(UUID.randomUUID(), order(), "x", -1, "CNY", "staff:ops"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("金额不能为负");
    }
}
