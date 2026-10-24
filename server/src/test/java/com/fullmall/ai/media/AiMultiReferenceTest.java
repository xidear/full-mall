package com.fullmall.ai.media;

import com.sun.net.httpserver.HttpServer;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多参考图（多角度商品图 / 角色脸部定型）：服务端解析成 [{url,role}]，
 * 火山方舟协议按官方形态 `content[] + role` 传参。
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
class AiMultiReferenceTest {

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
    AiMediaService media;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    com.fullmall.platform.secret.SecretCipher cipher;

    private HttpServer server;
    private final AtomicReference<String> imageBody = new AtomicReference<>();
    private final AtomicReference<String> videoBody = new AtomicReference<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/images/generations", exchange -> {
            imageBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = "{\"data\":[{\"url\":\"https://cdn.example.com/out.png\"}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.createContext("/contents/generations/tasks", exchange -> {
            videoBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = "{\"id\":\"t1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.createContext("/contents/generations/tasks/t1", exchange -> {
            byte[] out = "{\"status\":\"succeeded\",\"content\":{\"video_url\":\"https://cdn.example.com/v.mp4\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void provider(String code, MediaKind kind, String protocol, String base) {
        jdbc.update("""
                INSERT INTO ai_provider (id, kind, code, name, protocol, endpoint, api_key, model,
                                         extra, enabled, priority)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'seed-model', '{}'::jsonb, TRUE, 10)
                """, UUID.randomUUID(), kind.name(), code, code, protocol, base, cipher.encrypt("k"));
    }

    private UUID asset(String url) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO asset (id, name, url, kind, owner_username, purpose)
                VALUES (?, '参考图', ?, 'IMAGE', 'admin', 'PRODUCT')
                """, id, url);
        return id;
    }

    @Test
    void arkImageSendsAllReferenceImagesWithRole() throws Exception {
        String base = start();
        provider("ark-img", MediaKind.IMAGE, "ark-image", base);
        UUID front = asset("https://cdn.example.com/front.png");
        UUID side = asset("https://cdn.example.com/side.png");
        UUID face = asset("https://cdn.example.com/face.png");

        UUID jobId = media.enqueue(new AiMediaService.CreateJob(
                MediaKind.IMAGE, "ark-img", null, null, "DETAIL", "多角度合成详情图", null,
                Map.of("size", "1024x1024"), List.of(),
                List.of(new AiMediaService.ImageRef(front, "REFERENCE"),
                        new AiMediaService.ImageRef(side, "REFERENCE"),
                        new AiMediaService.ImageRef(face, "SUBJECT"))), "admin");

        assertThat(media.runOne()).isTrue();
        assertThat(media.job(jobId)).containsEntry("status", "SUCCEEDED");
        assertThat(imageBody.get())
                .contains("front.png").contains("side.png").contains("face.png")
                .contains("\"role\":\"reference_image\"");
    }

    @Test
    void arkVideoSendsFirstFrameAndReferenceImages() throws Exception {
        String base = start();
        provider("ark-vid", MediaKind.VIDEO, "ark-video", base);
        UUID frame = asset("https://cdn.example.com/first.png");
        UUID ref = asset("https://cdn.example.com/ref.png");

        UUID jobId = media.enqueue(new AiMediaService.CreateJob(
                MediaKind.VIDEO, "ark-vid", null, null, "VIDEO", "镜头：正面推近", null,
                Map.of("duration", 5), List.of(),
                List.of(new AiMediaService.ImageRef(frame, "FIRST_FRAME"),
                        new AiMediaService.ImageRef(ref, "REFERENCE"))), "admin");

        assertThat(media.runOne()).isTrue();
        assertThat(media.job(jobId)).containsEntry("status", "SUCCEEDED");
        assertThat(videoBody.get())
                .contains("\"role\":\"first_frame\"")
                .contains("\"role\":\"reference_image\"");
    }
}
