package com.fullmall.ai.media;

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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 按各厂官方形态校验请求体（官方 mock）：
 * OpenAI images/edits multipart 多文件、qwen-image-edit messages 多图、MiniMax subject_reference。
 */
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
class AiProviderPayloadTest {

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

    @Autowired
    AiMediaService media;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    IntegrationConfigService integrations;

    @Autowired
    com.fullmall.platform.secret.SecretCipher cipher;

    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastContentType = new AtomicReference<>();

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

    private String base() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ref", exchange -> {
            exchange.sendResponseHeaders(200, PNG.length);
            exchange.getResponseBody().write(PNG);
            exchange.close();
        });
        server.createContext("/images/edits", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] out = ("{\"data\":[{\"b64_json\":\"" + Base64.getEncoder().encodeToString(PNG)
                    + "\"}]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.createContext("/multimodal", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = ("{\"output\":{\"choices\":[{\"message\":{\"content\":[{\"image\":"
                    + "\"https://cdn.example.com/edited.png\"}]}}]}}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.createContext("/video_generation", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = "{\"task_id\":\"mm1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.createContext("/query/video_generation", exchange -> {
            byte[] out = "{\"status\":\"Success\",\"file_url\":\"https://cdn.example.com/mm.mp4\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void provider(String code, MediaKind kind, String protocol, String endpoint) {
        jdbc.update("""
                INSERT INTO ai_provider (id, kind, code, name, protocol, endpoint, api_key, model,
                                         extra, enabled, priority)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'm1', '{}'::jsonb, TRUE, 10)
                """, UUID.randomUUID(), kind.name(), code, code, protocol, endpoint,
                cipher.encrypt("k"));
    }

    private UUID asset(String url) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO asset (id, name, url, kind, owner_username, purpose)
                VALUES (?, 'ref', ?, 'IMAGE', 'admin', 'PRODUCT')
                """, id, url);
        return id;
    }

    @Test
    void openAiEditsUsesMultipartWithAllImages() throws Exception {
        String base = base();
        provider("oai-img", MediaKind.IMAGE, "openai-images", base);
        UUID a = asset(base + "/ref/a.png");
        UUID b = asset(base + "/ref/b.png");

        UUID jobId = media.enqueue(new AiMediaService.CreateJob(
                MediaKind.IMAGE, "oai-img", null, null, "MAIN", "合成主图", null,
                Map.of("size", "1024x1024"), List.of(),
                List.of(new AiMediaService.ImageRef(a, "REFERENCE"),
                        new AiMediaService.ImageRef(b, "SUBJECT"))), "admin");
        assertThat(media.runOne()).isTrue();

        assertThat(media.job(jobId)).containsEntry("status", "SUCCEEDED");
        assertThat(lastContentType.get()).startsWith("multipart/form-data");
        assertThat(lastBody.get())
                .contains("name=\"image[]\"; filename=\"ref1.png\"")
                .contains("name=\"image[]\"; filename=\"ref2.png\"")
                .contains("name=\"model\"");
    }

    @Test
    void qwenImageEditSendsAllImagesInMessages() throws Exception {
        String base = base();
        provider("qwen-edit", MediaKind.IMAGE, "dashscope-multimodal", base + "/multimodal");
        UUID a = asset(base + "/ref/a.png");
        UUID b = asset(base + "/ref/b.png");

        UUID jobId = media.enqueue(new AiMediaService.CreateJob(
                MediaKind.IMAGE, "qwen-edit", null, null, "DETAIL", "两张图合成白底详情图", null,
                Map.of(), List.of(),
                List.of(new AiMediaService.ImageRef(a, "REFERENCE"),
                        new AiMediaService.ImageRef(b, "REFERENCE"))), "admin");
        assertThat(media.runOne()).isTrue();

        assertThat(media.job(jobId)).containsEntry("status", "SUCCEEDED");
        String body = lastBody.get();
        assertThat(body).contains("\"messages\"").contains("\"image\":\"" + base + "/ref/a.png\"")
                .contains("\"image\":\"" + base + "/ref/b.png\"").contains("\"text\"");
    }

    @Test
    void miniMaxUsesFirstFrameAndCharacterSubject() throws Exception {
        String base = base();
        provider("mm-video", MediaKind.VIDEO, "minimax-video", base);
        UUID frame = asset(base + "/ref/f.png");
        UUID face = asset(base + "/ref/face.png");

        UUID jobId = media.enqueue(new AiMediaService.CreateJob(
                MediaKind.VIDEO, "mm-video", null, null, "VIDEO", "人物走动展示商品", null,
                Map.of("duration", 5), List.of(),
                List.of(new AiMediaService.ImageRef(frame, "FIRST_FRAME"),
                        new AiMediaService.ImageRef(face, "SUBJECT"))), "admin");
        assertThat(media.runOne()).isTrue();

        assertThat(media.job(jobId)).containsEntry("status", "SUCCEEDED");
        String body = lastBody.get();
        assertThat(body).contains("\"first_frame_image\":\"" + base + "/ref/f.png\"")
                .contains("\"subject_reference\"")
                .contains("\"type\":\"character\"")
                .contains(base + "/ref/face.png");
    }
}
