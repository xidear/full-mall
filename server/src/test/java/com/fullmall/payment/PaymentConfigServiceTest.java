package com.fullmall.payment;

import com.fullmall.payment.app.PaymentConfigService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class PaymentConfigServiceTest {

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
    PaymentConfigService service;

    @Autowired
    JdbcTemplate jdbc;

    private static PaymentConfigService.ConfigInput subMerchant(String market, String subMchId) {
        return new PaymentConfigService.ConfigInput("WECHAT", "SUB_MERCHANT", market, "微信服务商",
                "某支付服务商", "1900000109", "wxspp", subMchId, "wxsub", null, null, "https://cb.example/x");
    }

    @Test
    void subMerchantModeRequiresOurSubMerchantId() {
        var missingSub = new PaymentConfigService.ConfigInput("ALIPAY", "SUB_MERCHANT", "SG", "支付宝",
                "SP", "2088000000000001", "app", null, null, null, null, null);

        assertThatThrownBy(() -> service.create(missingSub, "admin"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("子商户号");
    }

    @Test
    void directModeRejectsServiceProviderFields() {
        var bad = new PaymentConfigService.ConfigInput("WECHAT", "DIRECT", "CN", "直连",
                null, "1900000109", null, null, null, "1600000001", "wxapp", null);

        assertThatThrownBy(() -> service.create(bad, "admin"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("直连模式");
    }

    @Test
    void onlyOneActiveConfigPerChannelAndMarket() {
        service.create(subMerchant("SG", "1600000100"), "admin");

        assertThatThrownBy(() -> service.create(subMerchant("SG", "1600000101"), "admin"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("已有生效配置");
    }

    @Test
    void editingCreatesNewImmutableVersionAndMasksIds() {
        UUID id = service.create(subMerchant("MY", "1600000001"), "admin");
        int version = service.update(id, subMerchant("MY", "1600000002"), "admin");

        assertThat(version).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_provider_config_version WHERE config_id = ?", Integer.class, id))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT settings->>'subMchId' FROM payment_provider_config_version
                 WHERE config_id = ? AND version_no = 1
                """, String.class, id)).isEqualTo("1600000001");

        var view = service.list().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
        assertThat(view.subMchIdMasked()).contains("*").doesNotContain("1600000002");
    }
}
