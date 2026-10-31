package com.fullmall.ai.media;

import com.fullmall.platform.secret.SecretCipher;
import com.sun.net.httpserver.HttpServer;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fullmall.platform.secret.IntegrationConfigService;
import com.fullmall.platform.secret.TestIntegrationConfig;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AI 素材生产全链路：供应商（密钥加密）→ 入队 → 异步执行 → 入素材库 → 提示词留痕。stub 供应商，无需真模型。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.scheduling.enabled=false",
                "app.asset.storage=local",
                "app.asset.local.directory=/tmp/fullmall-ai-assets"
        })
@ActiveProfiles("test")
class AiMediaPipelineTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

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

    private HttpServer server;
    private final java.util.concurrent.atomic.AtomicReference<String> lastImageRequest =
            new java.util.concurrent.atomic.AtomicReference<>();

    @BeforeEach
    void enableStorage() {
        TestIntegrationConfig.enableLocal(integrations, "storage", "/tmp/fullmall-ai-assets");
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Autowired
    AiMediaService media;

    @Autowired
    SecretCipher cipher;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    IntegrationConfigService integrations;

    /** 直接写库（控制器上有 @PreAuthorize，测试无安全上下文）；密钥仍走加密，验证落库不是明文。 */
    private void upsertProvider(String code, MediaKind kind, String protocol, String base) {
        jdbc.update("""
                INSERT INTO ai_provider (id, kind, code, name, protocol, endpoint, api_key, model,
                                         extra, enabled, priority)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, '{}'::jsonb, TRUE, 10)
                ON CONFLICT (code) DO UPDATE
                   SET kind = EXCLUDED.kind, protocol = EXCLUDED.protocol,
                       endpoint = EXCLUDED.endpoint, api_key = EXCLUDED.api_key,
                       model = EXCLUDED.model, enabled = TRUE
                """, UUID.randomUUID(), kind.name(), code, code, protocol, base,
                cipher.encrypt("sk-secret"), "stub-model");
    }

    private String baseUrl() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/images/generations", exchange -> {
            lastImageRequest.set(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            String body = "{\"data\":[{\"b64_json\":\""
                    + Base64.getEncoder().encodeToString(PNG) + "\"}]}";
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.createContext("/images/edits", exchange -> {
            lastImageRequest.set(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.ISO_8859_1));
            String body = "{\"data\":[{\"b64_json\":\""
                    + Base64.getEncoder().encodeToString(PNG) + "\"}]}";
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.createContext("/ref", exchange -> {
            exchange.sendResponseHeaders(200, PNG.length);
            exchange.getResponseBody().write(PNG);
            exchange.close();
        });
        server.createContext("/chat/completions", exchange -> {
            String body = "{\"choices\":[{\"message\":{\"content\":\"白底、45° 侧光、浅灰渐变背景\"}}]}";
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void imageJobRunsAndLandsInAssetLibrary() throws Exception {
        String base = baseUrl();
        upsertProvider("stub-image", MediaKind.IMAGE, "openai-images", base);
        upsertProvider("stub-chat", MediaKind.TEXT, "openai-chat", base);

        String storedKey = jdbc.queryForObject(
                "SELECT api_key FROM ai_provider WHERE code = 'stub-image'", String.class);
        assertThat(storedKey).as("API Key 必须加密落库").doesNotContain("sk-secret");

        UUID jobId = media.enqueue(new AiMediaService.CreateJob(
                MediaKind.IMAGE, "stub-image", null, null, "MAIN",
                "白底商品主图，陶瓷马克杯", "logo,水印", Map.of("size", "1024x1024"),
                List.of()), "admin");

        assertThat(media.runOne()).isTrue();
        var job = media.job(jobId);
        assertThat(job).containsEntry("status", "SUCCEEDED");
        assertThat(job.get("assetId")).isNotNull();

        UUID assetId = UUID.fromString(String.valueOf(job.get("assetId")));
        var asset = jdbc.queryForMap("SELECT name, url, kind FROM asset WHERE id = ?", assetId);
        assertThat(asset.get("kind")).isEqualTo("IMAGE");
        assertThat(String.valueOf(asset.get("url"))).contains("/ai/stub-image/");

        var prompts = media.promptHistory(null, null, 5);
        assertThat(prompts).anySatisfy(row ->
                assertThat(String.valueOf(row.get("prompt"))).contains("陶瓷马克杯"));
    }

    @Test
    void optimizePromptAndMissingProvider() throws Exception {
        String base = baseUrl();
        upsertProvider("stub-chat", MediaKind.TEXT, "openai-chat", base);

        assertThat(media.optimizePrompt("马克杯主图", "stub-chat")).contains("白底");

        assertThatThrownBy(() -> media.optimizePrompt("x", "no-such-provider"))
                // 现在按协议挑供应商：错误信息明确说需要 openai-chat，而不是笼统的“供应商不可用”
                .hasMessageContaining("没有可用的对话模型");
    }

    @Test
    void referenceImageIsSentToProvider() throws Exception {
        String base = baseUrl();
        upsertProvider("stub-image-ref", MediaKind.IMAGE, "openai-images", base);
        UUID refAsset = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO asset (id, name, url, kind, owner_username, purpose)
                VALUES (?, '实拍参考图', ?, 'IMAGE', 'admin', 'PRODUCT')
                """, refAsset, base + "/ref/ref.png");

        UUID jobId = media.enqueue(new AiMediaService.CreateJob(
                MediaKind.IMAGE, "stub-image-ref", null, null, "DETAIL",
                "按参考图生成详情图", null, Map.of("size", "1024x1024"),
                List.of(refAsset)), "admin");
        media.runOne();

        assertThat(media.job(jobId)).containsEntry("status", "SUCCEEDED");
        assertThat(lastImageRequest.get())
                .as("参考图必须真实发给供应商（gpt-image-1 走 multipart image[]）")
                .contains("name=\"image[]\"").contains("ref1.png");
    }
}
