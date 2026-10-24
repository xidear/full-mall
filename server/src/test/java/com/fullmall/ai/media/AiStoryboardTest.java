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

import static org.assertj.core.api.Assertions.assertThat;

/** 分镜：镜头逐个入队生成，完成后回填到镜头。 */
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
class AiStoryboardTest {

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

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Autowired
    AiStoryboardService storyboards;

    @Autowired
    AiMediaService media;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    com.fullmall.platform.secret.SecretCipher cipher;

    private String startVideoStub() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/services/aigc/video-generation/video-synthesis", exchange -> {
            byte[] out = "{\"output\":{\"task_id\":\"t1\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.createContext("/api/v1/tasks/t1", exchange -> {
            byte[] out = "{\"output\":{\"task_status\":\"SUCCEEDED\",\"video_url\":\"https://cdn.example.com/shot.mp4\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void generatesEachShotAndBackfillsAsset() throws Exception {
        String base = startVideoStub();
        jdbc.update("""
                INSERT INTO ai_provider (id, kind, code, name, protocol, endpoint, api_key, model,
                                         extra, enabled, priority)
                VALUES (?, 'VIDEO', 'stub-video', 'Stub 视频', 'dashscope-video',
                        ?, ?, 'vid-model', '{}'::jsonb, TRUE, 10)
                """, UUID.randomUUID(),
                base + "/api/v1/services/aigc/video-generation/video-synthesis",
                cipher.encrypt("sk"));

        UUID productId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '分镜商品', 'ACTIVE')", productId);

        UUID storyboardId = storyboards.create("主图视频", productId, null, List.of(
                new AiStoryboardService.ShotInput("镜头1：正面推近", null, 5),
                new AiStoryboardService.ShotInput("镜头2：侧面旋转", null, 5)), "admin");

        var jobIds = storyboards.generate(storyboardId, "stub-video", "admin");
        assertThat(jobIds).hasSize(2);

        assertThat(media.runOne()).isTrue();
        assertThat(media.runOne()).isTrue();

        assertThat(storyboards.syncAssets(storyboardId)).isEqualTo(2);
        var detail = storyboards.get(storyboardId);
        assertThat(detail.shots()).allSatisfy(shot -> assertThat(shot.assetId()).isNotNull());
        assertThat(detail.shots().get(0).prompt()).contains("正面推近");
    }
}
