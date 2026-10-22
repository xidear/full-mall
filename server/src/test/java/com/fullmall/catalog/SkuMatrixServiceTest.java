package com.fullmall.catalog;

import com.fullmall.catalog.app.SkuMatrixService;
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

/** 规格矩阵：生成编码、重复生成幂等、非法输入拒绝。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SkuMatrixServiceTest {

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
    SkuMatrixService matrix;

    @Autowired
    JdbcTemplate jdbc;

    private UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '矩阵商品', 'ACTIVE')", id);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, UUID.randomUUID(), id, "BASE-" + id.toString().substring(0, 8));
        return id;
    }

    @Test
    void generatesCodesAndIsIdempotent() {
        UUID productId = product();

        var first = matrix.generate(productId, "MUG", "颜色",
                List.of("米白", " 墨绿 "), 3_900, null);
        assertThat(first.created()).isEqualTo(2);
        assertThat(first.codes()).containsExactly("MUG-米白", "MUG-墨绿");

        var second = matrix.generate(productId, "MUG", "颜色", List.of("米白"), 5_000, null);
        assertThat(second.created()).isZero();
        assertThat(second.skipped()).isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "SELECT price_amount FROM sku WHERE sku_code = 'MUG-米白'", Long.class))
                .as("已存在编码不覆盖价格").isEqualTo(3_900L);
    }

    @Test
    void invalidInputsAreRejected() {
        UUID productId = product();
        assertThatThrownBy(() -> matrix.generate(productId, "", "颜色", List.of("米白"), 1, null))
                .hasMessageContaining("前缀不能为空");
        assertThatThrownBy(() -> matrix.generate(productId, "MUG", "颜色", List.of(), 1, null))
                .hasMessageContaining("至少需要一个规格值");
        assertThatThrownBy(() -> matrix.generate(productId, "MUG", "颜色", List.of("米白"), 0, null))
                .hasMessageContaining("价格必须大于 0");
        assertThatThrownBy(() -> matrix.generate(productId, "MUG", "颜色",
                List.of("米白", "米白"), 1, null))
                .hasMessageContaining("编码重复");
    }
}
