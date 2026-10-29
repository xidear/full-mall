package com.fullmall.catalog;

import com.fullmall.catalog.app.BrandService;
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

/** 品牌管理：编码唯一、被商品引用时禁止删除。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class BrandServiceTest {

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
    BrandService brands;

    @Autowired
    JdbcTemplate jdbc;

    // 守住"编码重复要变成可读冲突，而不是 500"：唯一约束在 DB，DuplicateKeyException 必须翻译；
    // productCount 必须是 0 而不是 null，后台表格直接渲染这个数字。
    @Test
    void createListUpdateAndDuplicateCode() {
        UUID id = brands.create("BR-TEST-1", "测试品牌", 5);

        assertThat(brands.list()).anySatisfy(brand -> {
            assertThat(brand.id()).isEqualTo(id);
            assertThat(brand.name()).isEqualTo("测试品牌");
            assertThat(brand.productCount()).isZero();
        });
        assertThatThrownBy(() -> brands.create("BR-TEST-1", "重复编码", 0))
                .hasMessageContaining("编码已存在");

        brands.update(id, "改名品牌", 9);
        assertThat(brands.list()).anySatisfy(brand ->
                assertThat(brand.name()).isEqualTo("改名品牌"));
    }

    // 重名判断忽略大小写与首尾空格，但改名成自己当前的名字不算冲突——
    // 少做前半句，保存未改名的表单就会误报；少做后半句，真正的重复会漏过。
    @Test
    void duplicateNameIsRejectedIgnoringCaseAndSpaces() {
        UUID id = brands.create("BR-NAME-1", "同名品牌", 0);

        assertThatThrownBy(() -> brands.create("BR-NAME-2", "  同名品牌  ", 0))
                .hasMessageContaining("同名品牌已存在");
        assertThatThrownBy(() -> brands.create("BR-NAME-3", "同名品牌", 0))
                .hasMessageContaining("同名品牌已存在");

        // 改名到"自己现在的名字"不应报冲突
        brands.update(id, "同名品牌", 1);

        // 改名撞上别人已有的名字要被拒绝
        brands.update(id, "另一个名字", 1);
        UUID other = brands.create("BR-NAME-4", "第三个名字", 0);
        assertThatThrownBy(() -> brands.update(other, "另一个名字", 0))
                .hasMessageContaining("同名品牌已存在");
    }

    // 删除前先查引用并给出原因，不靠外键异常冒泡成 500；解除引用后必须删得掉，否则品牌永久卡死。
    // 断言用 noneSatisfy 而不是集合大小：库是共享的，其它用例也在造品牌。
    @Test
    void deleteIsRejectedWhenBrandInUse() {
        UUID brandId = brands.create("BR-TEST-2", "被引用品牌", 0);
        UUID productId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product (id, title, status, brand_id)
                VALUES (?, '关联商品', 'ACTIVE', ?)
                """, productId, brandId);

        assertThatThrownBy(() -> brands.delete(brandId))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不能删除");

        jdbc.update("DELETE FROM product WHERE id = ?", productId);
        brands.delete(brandId);
        assertThat(brands.list()).noneSatisfy(brand ->
                assertThat(brand.code()).isEqualTo("BR-TEST-2"));
    }
}
