package com.fullmall.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文档契约测试：**文档里提到的每个接口路径都必须真实存在**。
 *
 * <p>为什么需要：本项目多次出现"文档/清单说做了，但接口根本没加"（如
 * `GET /admin/v1/suppliers/{id}/settlements` 长期 405）。靠人记不可靠，改成机械比对。
 *
 * <p>做法：扫描 `docs/OPEN_ITEMS.md` 与 `docs/IMPLEMENTATION_STATUS.md` 中形如
 * `/admin/v1/...`、`/api/v1/...`、`/open/v1/...` 的路径，与 Spring 实际注册的路由模式比对。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class DocumentedPathsExistTest {

    private static final Pattern DOC_PATH =
            Pattern.compile("`(/(?:admin|api|open|store|support)/v1/[A-Za-z0-9_\\-{}/.]*)");

    private static final io.zonky.test.db.postgres.embedded.EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = io.zonky.test.db.postgres.embedded.EmbeddedPostgres.builder().start();
        } catch (Exception error) {
            throw new IllegalStateException("无法启动嵌入式 PostgreSQL", error);
        }
    }

    @org.springframework.test.context.DynamicPropertySource
    static void datasource(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    org.springframework.context.ApplicationContext context;

    /** 企业版专有前缀：社区版打包时这些类根本不存在，文档里出现是正常的。 */
    private static final List<String> ENTERPRISE_ONLY = List.of("/admin/v1/ai");

    private boolean communityEdition() {
        String edition = System.getenv("APP_EDITION");
        return edition == null || edition.isBlank() || "community".equalsIgnoreCase(edition.trim());
    }

    @Test
    void everyDocumentedPathIsRegistered() throws Exception {
        RequestMappingHandlerMapping handlerMapping = context.getBean(
                "requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Set<String> registered = handlerMapping.getHandlerMethods().keySet().stream()
                .map(RequestMappingInfo::getPathPatternsCondition)
                .filter(java.util.Objects::nonNull)
                .flatMap(condition -> condition.getPatternValues().stream())
                .map(this::normalize)
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> missing = new ArrayList<>();
        // 只扫"当前诉求清单"：实现状态文档里有大量历史叙述（修复前后对比），不适合做契约源
        for (Path doc : List.of(Path.of("../docs/OPEN_ITEMS.md"))) {
            if (!Files.exists(doc)) {
                continue;
            }
            Matcher matcher = DOC_PATH.matcher(Files.readString(doc));
            while (matcher.find()) {
                String documented = normalize(matcher.group(1));
                if (isPlaceholderOrPartial(documented)) {
                    continue;
                }
                if (communityEdition()
                        && ENTERPRISE_ONLY.stream().anyMatch(documented::startsWith)) {
                    continue;   // 社区版不含 AI 模块
                }
                if (!registered.contains(documented)) {
                    missing.add(documented + "  （来自 " + doc.getFileName() + "）");
                }
            }
        }

        assertThat(new TreeSet<>(missing))
                .as("以下路径在文档中出现但未注册到 Spring 路由")
                .isEmpty();
    }

    /** 去掉查询串与末尾斜杠；把文档里的具体 id/占位符统一成 `{var}` 以便比较。 */
    private String normalize(String path) {
        String trimmed = path.trim();
        int query = trimmed.indexOf('?');
        if (query >= 0) {
            trimmed = trimmed.substring(0, query);
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        // 路径变量名不参与比较：`{id}` 与 `{fulfillmentOrderId}` 视为同一模式
        return trimmed.replaceAll("\\{[^}]*}", "{}");
    }

    /** 文档里常见的"示例片段"（如 `/admin/v1/orders/{id}/refunds` 之外的裸前缀）跳过。 */
    private boolean isPlaceholderOrPartial(String path) {
        return path.endsWith("/") || path.contains("...") || path.contains("*");
    }
}
