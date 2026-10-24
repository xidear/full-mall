package com.fullmall.ai.knowledge;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 语义检索：桩 embedding 建索引→余弦排序正确；空类重建明确报错。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.scheduling.enabled=false",
                "app.ai.semantic.enabled=true"
        })
@ActiveProfiles("test")
class SemanticIndexTest {

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
    SemanticIndexService index;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    com.fullmall.platform.secret.SecretCipher cipher;

    private void startEmbeddingStub() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/embeddings", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            double[] v = body.contains("发票") ? new double[]{1, 0, 0}
                    : body.contains("退货") ? new double[]{0, 1, 0} : new double[]{0, 0, 1};
            String json = "{\"data\":[{\"embedding\":[" + v[0] + "," + v[1] + "," + v[2] + "]}]}";
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        jdbc.update("""
                INSERT INTO ai_provider (id, kind, code, name, protocol, endpoint, api_key, model,
                                         extra, enabled, priority)
                VALUES (?, 'TEXT', 'stub-emb', 'Stub', 'openai-embedding', ?, ?, 'e1',
                        '{}'::jsonb, TRUE, 1)
                ON CONFLICT (code) DO UPDATE SET endpoint = EXCLUDED.endpoint
                """, UUID.randomUUID(),
                "http://127.0.0.1:" + server.getAddress().getPort(), cipher.encrypt("k"));
    }

    @Test
    void ranksSemanticallyClosestFirst() throws Exception {
        startEmbeddingStub();
        index.index("FAQ", "f1", "怎么开发票", "stub-emb");
        index.index("FAQ", "f2", "如何退货退款", "stub-emb");

        List<Map<String, Object>> results = index.search("我要开发票", "FAQ", 2, "stub-emb");

        assertThat(results).isNotEmpty();
        assertThat(String.valueOf(results.get(0).get("sourceId"))).isEqualTo("f1");
        assertThat((double) results.get(0).get("score")).isGreaterThan(0.9);
    }

    @Test
    void reindexFaqAndEmptyKindFails() throws Exception {
        startEmbeddingStub();
        jdbc.update("""
                INSERT INTO faq_entry (id, question, answer) VALUES (?, '怎么修改地址', '订单详情里改')
                """, UUID.randomUUID());

        assertThat(index.reindexKind("FAQ", "stub-emb")).isGreaterThanOrEqualTo(1);
        assertThatThrownBy(() -> index.reindexKind("UNKNOWN", "stub-emb"))
                .hasMessageContaining("没有可索引的数据");
    }
}
