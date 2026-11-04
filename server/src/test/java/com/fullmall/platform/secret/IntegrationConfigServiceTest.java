package com.fullmall.platform.secret;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 第三方配置：敏感字段加密、读取不回显、启用切换互斥、未配置时 resolve 明确报错。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class IntegrationConfigServiceTest {

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
    IntegrationConfigService configs;

    @Autowired
    JdbcTemplate jdbc;

    /** 每个用例都从空表开始：同一能力可能有多个供应商，用例之间不能互相看见对方的行。 */
    @BeforeEach
    void clearIntegrations() {
        jdbc.update("DELETE FROM integration_route");
        jdbc.update("DELETE FROM integration_config");
    }

    @Test
    void savesEncryptedAndNeverEchoesSecrets() {
        var saved = configs.save("sms", "aliyun",
                Map.of("access-key-id", "AK123", "access-key-secret", "SK456",
                        "sign-name", "示例签名"), true, "admin");

        assertThat(saved.secretFields()).containsExactlyInAnyOrder("access-key-id", "access-key-secret");
        assertThat(saved.visibleFields()).containsExactly("sign-name");

        String raw = new String(jdbc.queryForObject(
                "SELECT config_enc FROM integration_config WHERE capability = 'sms'", byte[].class),
                java.nio.charset.StandardCharsets.UTF_8);
        assertThat(raw).doesNotContain("AK123").doesNotContain("SK456");

        var view = configs.list("sms").get(0);
        assertThat(view.secretsConfigured()).isTrue();
        assertThat(view.hint()).containsEntry("sign-name", "示例签名");
        assertThat(view.hint()).doesNotContainKeys("access-key-id", "access-key-secret");

        // sms 是多供应商能力：resolve() 会拒绝猜「哪一个」（ADR-0018），必须指名供应商
        var resolved = configs.resolveProvider("sms", "aliyun");
        assertThat(resolved).containsEntry("access-key-secret", "SK456");
    }

    @Test
    void ambiguousResolveIsRejectedForMultiProviderCapability() {
        configs.save("sms", "aliyun", Map.of("access-key-id", "AK", "access-key-secret", "SK",
                "sign-name", "签名"), true, "admin");
        configs.save("sms", "tencent", Map.of("secret-id", "ID", "secret-key", "KEY",
                "sdk-app-id", "1400", "sign-name", "签名"), true, "admin");

        assertThatThrownBy(() -> configs.resolve("sms"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("resolveProvider");
        assertThat(configs.enabledProviders("sms")).containsExactlyInAnyOrder("aliyun", "tencent");
    }

    @Test
    void enablingAnotherProviderDisablesPrevious() {
        configs.save("storage", "qiniu", Map.of("access-key", "a", "secret-key", "b",
                "bucket", "demo-bucket", "domain", "https://cdn.example.com"), true, "admin");
        configs.save("storage", "local", Map.of("directory", "/data/assets"), true, "admin");

        var views = configs.list("storage");
        assertThat(views).filteredOn(v -> "qiniu".equals(v.provider())).allSatisfy(v ->
                assertThat(v.enabled()).isFalse());
        assertThat(views).filteredOn(v -> "local".equals(v.provider())).allSatisfy(v ->
                assertThat(v.enabled()).isTrue());
    }

    @Test
    void providerWithoutSecretsIsAllowedAndUnconfiguredResolveFails() {
        // local 存储没有密钥：不应被"必须填敏感字段"卡住
        var saved = configs.save("storage", "local",
                Map.of("directory", "/data/assets"), false, "admin");
        assertThat(saved.secretFields()).isEmpty();

        assertThatThrownBy(() -> configs.resolve("payment"))
                .hasMessageContaining("尚未在后台启用配置");
    }

    /** 能力拼错必须拒绝：否则会写入永不生效的死配置。 */
    @Test
    void unsupportedCapabilityIsRejected() {
        assertThatThrownBy(() -> configs.save("smsx", "aliyun",
                Map.of("access-key-id", "a", "access-key-secret", "b"), true, "admin"))
                .hasMessageContaining("不支持的能力");
        assertThatThrownBy(() -> configs.save("", "aliyun",
                Map.of("access-key", "a"), true, "admin"))
                .hasMessageContaining("能力与供应商不能为空");
    }

    /** 无字段/全可选字段供应商（mock、Frankfurter）允许空配置；否则演示汇率源无法启用。 */
    @Test
    void providerWithoutRequiredFieldsAcceptsEmptyConfig() {
        var saved = configs.save("exchange_rate", "mock", Map.of(), true, "admin");
        assertThat(saved.enabled()).isTrue();
        assertThat(configs.enabledProvider("exchange_rate")).isEqualTo("mock");

        var frankfurter = configs.save("exchange_rate", "frankfurter", Map.of(), true, "admin");
        assertThat(frankfurter.enabled()).isTrue();
        assertThat(configs.enabledProvider("exchange_rate")).isEqualTo("frankfurter");
    }
}
