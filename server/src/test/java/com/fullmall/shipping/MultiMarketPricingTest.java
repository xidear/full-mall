package com.fullmall.shipping;

import com.fullmall.catalog.app.SkuPricingService;
import com.fullmall.catalog.domain.Sku;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.Money;
import com.fullmall.shipping.app.ShippingRateService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * 批次 D：按市场定价 + 运费阶梯（ADR-0013 下游）。
 *
 * <p>两条红线：
 * <ul>
 *   <li>币种不同时**不许**拿基础价顶替（否则人民币价会被当港币收）；</li>
 *   <li>跨国没有运费规则时**不许**默认免运费（那等于白送）。</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class MultiMarketPricingTest {

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
    SkuPricingService pricing;

    @Autowired
    ShippingRateService shipping;

    @Autowired
    JdbcTemplate jdbc;

    private UUID skuId;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM sku_price");
        jdbc.update("DELETE FROM shipping_rate");
        jdbc.update("DELETE FROM sku WHERE sku_code LIKE 'PRICE-%'");
        jdbc.update("DELETE FROM product WHERE id = 'f0000000-0000-0000-0000-000000000001'");
        jdbc.update("""
                INSERT INTO market (code, name, currency, country_code, tax_jurisdiction, price_includes_tax,
                                    timezone, active)
                VALUES ('CN', '中国', 'CNY', 'CN', 'CN-VAT', TRUE, 'Asia/Shanghai', TRUE)
                ON CONFLICT (code) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO market (code, name, currency, country_code, tax_jurisdiction, price_includes_tax,
                                    timezone, active)
                VALUES ('HK', '香港', 'HKD', 'HK', 'HK', TRUE, 'Asia/Hong_Kong', TRUE)
                ON CONFLICT (code) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO product (id, title, status) VALUES ('f0000000-0000-0000-0000-000000000001', '定价测试品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """);
        skuId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency, net_weight_grams)
                VALUES (?, 'f0000000-0000-0000-0000-000000000001', 'PRICE-1', 10000, 'CNY', 500)
                """, skuId);
    }

    private Sku sku() {
        return new Sku(skuId, UUID.fromString("f0000000-0000-0000-0000-000000000001"),
                "PRICE-1", null, new Money(10000, "CNY"), null, Map.of(), null, null);
    }

    @Test
    void domesticUsesBasePriceWhenCurrencyMatches() {
        var price = pricing.priceFor(sku(), "CN");
        assertThat(price.price().amount()).isEqualTo(10000);
        assertThat(price.price().currency()).isEqualTo("CNY");
    }

    @Test
    void crossCurrencyWithoutOverrideIsRejected() {
        // 基础价是 CNY，香港市场是 HKD：没有覆盖价时必须报错，不能把 100 元当 100 港币收
        assertThatThrownBy(() -> pricing.priceFor(sku(), "HK"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("未定价");
    }

    @Test
    void overrideTakesEffectAndMustUseMarketCurrency() {
        pricing.upsert(skuId, "HK", "HKD", 12000, null);
        assertThat(pricing.priceFor(sku(), "HK").price().amount()).isEqualTo(12000);
        // 币种写错直接拒绝：否则会以错误币种收款
        assertThatThrownBy(() -> pricing.upsert(skuId, "HK", "CNY", 13000, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("币种");
    }

    @Test
    void removingOverrideFallsBackToBasePrice() {
        pricing.upsert(skuId, "CN", "CNY", 11000, null);
        assertThat(pricing.priceFor(sku(), "CN").price().amount()).isEqualTo(11000);
        pricing.remove(skuId, "CN");
        assertThat(pricing.priceFor(sku(), "CN").price().amount()).isEqualTo(10000);
    }

    @Test
    void crossBorderWithoutRateIsRejectedButDomesticDefaultsToFree() {
        // 香港市场寄香港：无规则 → 国内默认免运费（现状 FLAT=0 的等价物）
        assertThat(shipping.fee("HK", "HK", 500, Money.of(10000, "HKD")).amount().amount()).isZero();
        // 香港市场寄新加坡：无规则 → 必须报错
        assertThatThrownBy(() -> shipping.fee("HK", "SG", 500, Money.of(10000, "HKD")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("没有配置运费规则");
    }

    @Test
    void weightBandAndFreeOverAreApplied() {
        shipping.upsert(UUID.randomUUID().toString(), "HK", "SG", 0, 1000, 5000, null);
        shipping.upsert(UUID.randomUUID().toString(), "HK", "SG", 1000, null, 9000, 30000L);
        // 1kg 以下 50 港币
        assertThat(shipping.fee("HK", "SG", 800, Money.of(10000, "HKD")).amount().amount()).isEqualTo(5000);
        // 超过 1kg 走第二档 90 港币
        assertThat(shipping.fee("HK", "SG", 1500, Money.of(10000, "HKD")).amount().amount()).isEqualTo(9000);
        // 满 300 港币免运费
        assertThat(shipping.fee("HK", "SG", 1500, Money.of(30000, "HKD")).amount().amount()).isZero();
    }

    @Test
    void orderWeightIsSummedFromSkuNetWeight() {
        assertThat(shipping.totalWeightGrams(Map.of(skuId, 3))).isEqualTo(1500);
    }
}
