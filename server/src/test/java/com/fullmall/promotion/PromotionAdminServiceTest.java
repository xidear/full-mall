package com.fullmall.promotion;

import com.fullmall.promotion.app.PromotionAdminService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;
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
class PromotionAdminServiceTest {

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
    PromotionAdminService service;

    @Autowired
    JdbcTemplate jdbc;

    private static PromotionAdminService.Command command(String code, String scope, List<UUID> ids) {
        return new PromotionAdminService.Command(code, "测试规则", "FULL_REDUCTION", 2_000, 300,
                scope, ids, "ONLINE",
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), 10);
    }

    @Test
    void createsDraftWithServerGeneratedRulesJson() {
        jdbc.update("DELETE FROM promotion WHERE code = 'ADMIN-RULE-1'");

        UUID id = service.create(command("ADMIN-RULE-1", "ALL", List.of()), "admin");

        assertThat(jdbc.queryForObject("SELECT status FROM promotion WHERE id = ?", String.class, id))
                .isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("SELECT rules->>'thresholdAmount' FROM promotion WHERE id = ?",
                String.class, id)).isEqualTo("2000");
        assertThat(jdbc.queryForObject("SELECT rules->>'channel' FROM promotion WHERE id = ?",
                String.class, id)).isEqualTo("ONLINE");
        jdbc.update("DELETE FROM promotion WHERE code = 'ADMIN-RULE-1'");
    }

    @Test
    void rejectsScopeIdThatDoesNotExist() {
        assertThatThrownBy(() -> service.create(
                command("ADMIN-RULE-2", "SKU", List.of(UUID.randomUUID())), "admin"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("范围 ID 不存在");
    }

    @Test
    void requiresScopeIdsForNonAllScope() {
        assertThatThrownBy(() -> service.create(command("ADMIN-RULE-3", "CATEGORY", List.of()), "admin"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("必须指定范围 ID");
    }

    @Test
    void activateOnlyFromDraft() {
        jdbc.update("DELETE FROM promotion WHERE code = 'ADMIN-RULE-4'");
        UUID id = service.create(command("ADMIN-RULE-4", "ALL", List.of()), "admin");

        service.activate(id, "admin");
        assertThatThrownBy(() -> service.activate(id, "admin"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("只有草稿");
        jdbc.update("DELETE FROM promotion WHERE code = 'ADMIN-RULE-4'");
    }

    @Test
    void rejectsInvalidDiscountPercentage() {
        var bad = new PromotionAdminService.Command("ADMIN-RULE-5", "测试", "FULL_DISCOUNT", 1_000, 100,
                "ALL", List.of(), "ONLINE", Instant.now(), Instant.now().plusSeconds(60), 1);

        assertThatThrownBy(() -> service.create(bad, "admin"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("1..99");
    }
}
