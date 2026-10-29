package com.fullmall.tax;

import com.fullmall.platform.money.Money;
import com.fullmall.tax.app.TaxService;
import com.fullmall.test.TestHttp;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 批次 E：计税口径与报关单导出。
 *
 * <p>断言重点是两条：
 * <ul>
 *   <li>价内税是**反算**（总额不变），价外税是**另加**（总额增加）；</li>
 *   <li>报关单缺 HS/原产地/净重时必须逐行告警，不能静默补 0。</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class TaxAndCustomsTest {

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

    @org.springframework.boot.test.context.TestConfiguration
    static class PermitAll {
        /** 必须最高优先级：否则 SecurityConfig 的链会先匹配 /admin/** 并返回 401。 */
        @org.springframework.context.annotation.Bean
        @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
        org.springframework.security.web.SecurityFilterChain chain(
                org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
            return http.csrf(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    TaxService taxes;

    /** 直接调控制器方法：这条断言验的是"导出内容"，不是"谁能访问"（鉴权另有测试），
     *  走 HTTP 会被 admin 安全链拦成 401，测不到真正要验的东西。 */
    @Autowired
    com.fullmall.order.api.CustomsInvoiceController customsInvoice;

    @Autowired
    JdbcTemplate jdbc;

    TestHttp http;

    @BeforeEach
    void setup() {
        http = new TestHttp(port);
        jdbc.update("DELETE FROM tax_rule");
        jdbc.update("""
                INSERT INTO market (code, name, currency, country_code, tax_jurisdiction, price_includes_tax,
                                    timezone, active)
                VALUES ('CN', '中国', 'CNY', 'CN', 'CN-VAT', TRUE, 'Asia/Shanghai', TRUE)
                ON CONFLICT (code) DO UPDATE SET tax_jurisdiction = 'CN-VAT'
                """);
    }

    @Test
    void inclusiveTaxIsExtractedNotAdded() {
        taxes.upsert(UUID.randomUUID().toString(), "CN-VAT", "增值税 13%", 1300, true, true);
        var line = taxes.calculate("CN", "CN", Money.of(11300, "CNY"));
        assertThat(line).isPresent();
        // 113.00 含 13% 税 → 税基 100.00，税额 13.00
        assertThat(line.get().taxableAmount()).isEqualTo(10000);
        assertThat(line.get().taxAmount()).isEqualTo(1300);
        assertThat(line.get().priceIncludesTax()).isTrue();
    }

    @Test
    void exclusiveTaxIsAddedOnTop() {
        taxes.upsert(UUID.randomUUID().toString(), "US-CA", "CA sales tax 8%", 800, false, true);
        var line = taxes.calculate("US-MARKET", "US-CA", Money.of(10000, "USD"));
        assertThat(line).isPresent();
        assertThat(line.get().taxAmount()).isEqualTo(800);
        assertThat(line.get().priceIncludesTax()).isFalse();
    }

    @Test
    void destinationRuleWinsOverMarketJurisdiction() {
        taxes.upsert(UUID.randomUUID().toString(), "CN-VAT", "增值税 13%", 1300, true, true);
        taxes.upsert(UUID.randomUUID().toString(), "SG", "GST 9%", 900, true, true);
        var line = taxes.calculate("CN", "SG", Money.of(10900, "CNY"));
        assertThat(line).isPresent();
        assertThat(line.get().jurisdiction()).isEqualTo("SG");
    }

    @Test
    void noRuleMeansNoTaxLine() {
        assertThat(taxes.calculate("CN", "ZZ", Money.of(10000, "CNY"))).isEmpty();
    }

    @Test
    void customsInvoiceFlagsMissingFields() {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone,
                                    fulfillment_mode, address_json)
                VALUES (?, ?, 'PAID', 'CNY', 10000, 0, 0, 10000, '报关测试', '13800000001', 'SHIP',
                        '{"countryCode":"SG"}'::jsonb)
                """, orderId, "CI-" + orderId);
        // 故意不填 HS 编码与原产地：导出必须告警
        jdbc.update("""
                INSERT INTO product (id, title, status)
                VALUES ('f0000000-0000-0000-0000-000000000009', '报关测试品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """);
        UUID skuId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, 'f0000000-0000-0000-0000-000000000009', ?, 10000, 'CNY')
                ON CONFLICT DO NOTHING
                """, skuId, "CI-" + skuId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot, quantity,
                                        unit_price_amount, currency, fulfillment_type, line_type)
                VALUES (?, ?, ?, '报关测试商品', ?, 2, 5000, 'CNY', 'SHIP', 'NORMAL')
                """, UUID.randomUUID(), orderId, skuId, "CI-SKU-1");

        var invoice = customsInvoice.invoice(orderId);
        assertThat(invoice.lines()).hasSize(1);
        assertThat(invoice.lines().get(0).skuCode()).isEqualTo("CI-SKU-1");
        // 缺字段必须逐行告警，不能静默补 0
        assertThat(invoice.warnings()).anyMatch(w -> w.contains("缺少 HS 编码"));
        assertThat(invoice.warnings()).anyMatch(w -> w.contains("缺少原产地"));
        assertThat(invoice.warnings()).anyMatch(w -> w.contains("没有税行"));
        assertThat(invoice.destinationCountry()).isEqualTo("SG");
    }
}
