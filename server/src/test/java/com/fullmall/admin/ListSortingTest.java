package com.fullmall.admin;

import com.fullmall.aftersale.app.AfterSaleService;
import com.fullmall.customer.app.CustomerService;
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

/** 客户/售后列表排序：白名单生效、未知字段安全回落。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ListSortingTest {

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
    CustomerService customers;

    @Autowired
    AfterSaleService afterSales;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    com.fullmall.staff.infra.StaffAuthRepository staff;

    @Autowired
    com.fullmall.supplier.infra.SupplierRepository suppliers;

    private void order(String phone, long total) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', ?, 0, 0, ?, 'p', ?, 'SHIP')
                """, id, "SORT-" + id.toString().substring(0, 8), total, total, phone);
    }

    @Test
    void customersSortByWhitelistedColumnsAndIgnoreUnknown() {
        order("13977000001", 1_000);
        order("13977000002", 9_000);

        var desc = customers.list(0, 10, "totalSpent", "desc");
        var asc = customers.list(0, 10, "totalSpent", "asc");

        assertThat((java.util.List<?>) desc.get("items")).isNotEmpty();
        assertThat((java.util.List<?>) asc.get("items")).isNotEmpty();
        assertThat((Long) desc.get("total")).isGreaterThanOrEqualTo(2L);

        // 未知字段与恶意片段都回落默认，不抛错也不破坏数据
        var fallback = customers.list(0, 10, "total_spent; DROP TABLE orders", "desc");
        assertThat(fallback).containsKey("items");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isPositive();
    }

    @Test
    void staffAndSupplierSortUseWhitelist() {
        // 员工：白名单字段排序可用，未知字段回落
        UUID staffId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO staff_user (id, username, display_name, password_hash, role, status)
                VALUES (?, ?, '排序员工', 'x', 'OPERATOR', 'ACTIVE')
                """, staffId, "sort_staff_" + staffId.toString().substring(0, 6));

        var staffAsc = staff.findAll("displayName", "asc");
        assertThat(staffAsc).isNotEmpty();
        var staffFallback = staff.findAll("display_name; DROP TABLE staff_user", "desc");
        assertThat(staffFallback).isNotNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_user", Integer.class)).isPositive();

        // 供应商：同上
        assertThat(suppliers.listSuppliers("code", "asc")).isNotNull();
        assertThat(suppliers.listSuppliers("name; DROP", "asc")).isNotNull();
    }

    @Test
    void afterSalesSortAcceptsWhitelist() {
        Map<String, Object> page = afterSales.list(null, 0, 10, "refundAmount", "asc");
        assertThat(page).containsKeys("items", "total");
        Map<String, Object> fallback = afterSales.list(null, 0, 10, "created_at; DROP", "desc");
        assertThat(fallback).containsKey("items");
    }
}
