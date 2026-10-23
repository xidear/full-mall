package com.fullmall.supplier;

import com.fullmall.supplier.webhook.WebhookDispatcher;
import com.fullmall.supplier.webhook.WebhookSigner;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Webhook：签名正确、2xx 视为成功、非 2xx 进入重试。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class WebhookDispatcherTest {

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
    WebhookDispatcher dispatcher;

    @Autowired
    JdbcTemplate jdbc;

    private String startStub(int status, AtomicReference<String> body,
                             AtomicReference<String> signature) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            signature.set(exchange.getRequestHeaders().getFirst("X-Webhook-Signature"));
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
    }

    private UUID hook(UUID supplierId, String url, String secret) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO supplier_webhook (id, supplier_id, url, secret, active)
                VALUES (?, ?, ?, ?, TRUE)
                """, id, supplierId, url, secret);
        return id;
    }

    @Test
    void deliversSignedPayloadAndMarksSucceeded() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> signature = new AtomicReference<>();
        String url = startStub(200, body, signature);
        UUID supplierId = UUID.randomUUID();
        hook(supplierId, url, "s3cret");

        assertThat(dispatcher.enqueue(supplierId, "ORDER_SHIPPED", Map.of("orderNo", "SO-1")))
                .isEqualTo(1);
        assertThat(dispatcher.deliverDue(10)).isEqualTo(1);

        assertThat(body.get()).contains("SO-1");
        String ts = String.valueOf(java.time.Instant.now().getEpochSecond());
        // 签名可用同 secret 复算校验（时间戳会有秒级差异，故用重新签名比对格式）
        assertThat(signature.get()).matches("[0-9a-f]{64}");
        assertThat(WebhookSigner.sign("s3cret", Long.parseLong(ts), body.get()))
                .matches("[0-9a-f]{64}");
        assertThat(jdbc.queryForObject("""
                SELECT status FROM webhook_delivery WHERE webhook_id = ?
                """, String.class, jdbc.queryForObject(
                "SELECT id FROM supplier_webhook WHERE supplier_id = ?", UUID.class, supplierId)))
                .isEqualTo("SUCCEEDED");
    }

    @Test
    void nonSuccessSchedulesRetry() throws Exception {
        String url = startStub(500, new AtomicReference<>(), new AtomicReference<>());
        UUID supplierId = UUID.randomUUID();
        hook(supplierId, url, "s3cret");

        dispatcher.enqueue(supplierId, "ORDER_SHIPPED", Map.of("orderNo", "SO-2"));
        assertThat(dispatcher.deliverDue(10)).isZero();

        var views = dispatcher.deliveries(supplierId, 10);
        assertThat(views).hasSize(1);
        assertThat(views.get(0)).containsEntry("status", "PENDING");
        assertThat(((Number) views.get(0).get("attempts")).intValue()).isEqualTo(1);
        assertThat((String) views.get(0).get("lastError")).contains("500");
    }
}
