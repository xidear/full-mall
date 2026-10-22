package com.fullmall.catalog;

import com.fullmall.catalog.app.CatalogService;
import com.fullmall.catalog.domain.ProductStatus;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 批量上下架：整批成功；含不存在商品则整批失败且不改任何状态。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class BatchStatusTest {

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
    CatalogService catalog;

    @Autowired
    JdbcTemplate jdbc;

    private UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '批量状态', 'ACTIVE')", id);
        return id;
    }

    @Test
    void updatesAllProducts() {
        UUID a = product();
        UUID b = product();

        int updated = catalog.batchUpdateStatus(List.of(a, b), ProductStatus.ARCHIVED);

        assertThat(updated).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT status FROM product WHERE id = ?", String.class, a))
                .isEqualTo("ARCHIVED");
        assertThat(jdbc.queryForObject("SELECT status FROM product WHERE id = ?", String.class, b))
                .isEqualTo("ARCHIVED");
    }

    @Test
    void missingProductFailsWholeBatch() {
        UUID existing = product();
        assertThatThrownBy(() -> catalog.batchUpdateStatus(
                List.of(existing, UUID.randomUUID()), ProductStatus.ARCHIVED))
                .hasMessageContaining("商品不存在");
        assertThat(jdbc.queryForObject("SELECT status FROM product WHERE id = ?", String.class, existing))
                .as("整批失败不得改动").isEqualTo("ACTIVE");
    }
}
