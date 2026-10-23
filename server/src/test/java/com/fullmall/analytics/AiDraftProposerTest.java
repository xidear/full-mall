package com.fullmall.analytics;

import com.fullmall.analytics.action.AiActionDraftService;
import com.fullmall.analytics.action.AiDraftProposer;
import com.fullmall.analytics.ai.OpenAiCompatClient;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** N3：模型只产出草稿；无模型明确报错；非法动作拒绝；双签后才执行。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AiDraftProposerTest {

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
    AiActionDraftService drafts;

    @Autowired
    JdbcTemplate jdbc;

    private AiDraftProposer proposerReturning(String assistantJson) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            String body = "{\"choices\":[{\"message\":{\"content\":"
                    + com.fullmall.platform.json.Json.write(assistantJson) + "}}]}";
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        var client = new OpenAiCompatClient(
                "http://127.0.0.1:" + server.getAddress().getPort(), "sk-test", "model", "");
        return new AiDraftProposer(client, drafts);
    }

    @Test
    void proposalBecomesDraftAndStillNeedsDualApproval() throws Exception {
        UUID productId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, 'N3 商品', 'ACTIVE')", productId);
        var proposer = proposerReturning(
                "{\"actionType\":\"product.status.set\",\"payload\":{\"productId\":\""
                        + productId + "\",\"status\":\"ARCHIVED\"}}");

        var draft = proposer.propose("把 N3 商品下架", "admin");
        assertThat(draft.status()).isEqualTo("PENDING_APPROVAL");

        drafts.approve(draft.id(), "a1");
        drafts.approve(draft.id(), "a2");
        drafts.execute(draft.id(), "a2");

        assertThat(jdbc.queryForObject("SELECT status FROM product WHERE id = ?", String.class, productId))
                .isEqualTo("ARCHIVED");
    }

    @Test
    void unsupportedActionAndMissingModelAreRejected() throws Exception {
        var proposer = proposerReturning("{\"actionType\":\"order.delete\",\"payload\":{}}");
        assertThatThrownBy(() -> proposer.propose("删掉所有订单", "admin"))
                .hasMessageContaining("不可执行");

        var noModel = new AiDraftProposer(null, drafts);
        assertThat(noModel.available()).isFalse();
        assertThatThrownBy(() -> noModel.propose("改价", "admin"))
                .hasMessageContaining("未配置模型");
    }
}
