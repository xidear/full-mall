package com.fullmall.catalog;

import com.fullmall.catalog.app.AdminProductDetailService;
import com.fullmall.catalog.app.SkuMatrixTemplateService;
import com.fullmall.catalog.api.ProductDetailView;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 商品详情聚合 + 规格矩阵模板。
 *
 * <p>覆盖两个设计缺陷：
 * <ul>
 *   <li>"没有译文"用 present=false 表达，而不是让前端把 404 当业务状态；</li>
 *   <li>矩阵模板只描述维度/取值/禁用组合，价格只在应用模板时给出。</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ProductDetailAndMatrixTemplateTest {

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
    AdminProductDetailService detailService;

    @Autowired
    SkuMatrixTemplateService matrixTemplates;

    @Autowired
    JdbcTemplate jdbc;

    private UUID product(String title) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, ?, 'ACTIVE')", id, title);
        return id;
    }

    @Test
    void detailListsEnabledLocalesEvenWhenTranslationMissing() {
        UUID productId = product("有机鲜牛奶");

        ProductDetailView detail = detailService.detail(productId);

        assertThat(detail.title()).isEqualTo("有机鲜牛奶");
        assertThat(detail.enabledLocales()).first().isEqualTo(detail.defaultLocale());
        assertThat(detail.translations()).isNotEmpty();
        assertThat(detail.translations()).allMatch(entry -> !entry.present())
                .allMatch(entry -> entry.title() == null);
        assertThat(detail.translations()).extracting(ProductDetailView.TranslationEntry::locale)
                .doesNotContain(detail.defaultLocale());
    }

    @Test
    void matrixTemplateHoldsDimensionsWithoutPriceAndAppliesToProduct() {
        LinkedHashMap<String, List<String>> dimensions = new LinkedHashMap<>();
        dimensions.put("颜色", List.of("黑", "白"));
        dimensions.put("容量", List.of("64G", "128G"));

        SkuMatrixTemplateService.TemplateView template = matrixTemplates.create(
                new SkuMatrixTemplateService.TemplateInput("PHONE-COLOR-MEM", "手机颜色×容量",
                        "可复用到同类商品", dimensions,
                        List.of(Map.of("颜色", "白", "容量", "128G"))));

        assertThat(template.combinationCount()).isEqualTo(4);
        assertThat(template.enabledCombinationCount()).isEqualTo(3);
        assertThat(template.dimensions().get("颜色")).containsExactly("黑", "白");

        UUID productId = product("测试手机");
        var applied = matrixTemplates.applyToProduct(productId, template.id(), "PHONE", 199900L, "CNY");

        assertThat(applied.created()).isEqualTo(3);
        assertThat(applied.disabled()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sku WHERE product_id = ?",
                Integer.class, productId)).isEqualTo(3);
        assertThat(jdbc.queryForObject(
                "SELECT sku_matrix_template_id FROM product WHERE id = ?", UUID.class, productId))
                .isEqualTo(template.id());

        assertThatThrownBy(() -> matrixTemplates.delete(template.id()))
                .hasMessageContaining("仍被 1 个商品使用");
    }

    @Test
    void templateRejectsUnknownDimensionValueInDisabledCombination() {
        LinkedHashMap<String, List<String>> dimensions = new LinkedHashMap<>();
        dimensions.put("颜色", List.of("黑"));

        assertThatThrownBy(() -> matrixTemplates.create(new SkuMatrixTemplateService.TemplateInput(
                "BAD-COMBO", "非法组合", null, dimensions, List.of(Map.of("颜色", "红")))))
                .hasMessageContaining("不存在的取值");
    }
}
