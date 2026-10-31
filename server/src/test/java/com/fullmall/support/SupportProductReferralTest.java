package com.fullmall.support;

import com.fullmall.support.api.SupportDtos;
import com.fullmall.support.app.SupportService;
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

/**
 * B10：客服发商品**不产生分销绑定**。
 * 之前链接里带 `ref=<客服ID>&mode=<请求自填>`，等于绕过分销绑定策略；现在显式拒绝。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SupportProductReferralTest {

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
    SupportService support;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void productMessageHasNoReferralAndRejectsMode() {
        UUID agentId = agent();
        UUID conversationId = conversation(agentId);
        UUID productId = product();

        var message = support.sendProduct(conversationId,
                new SupportDtos.SendProductRequest(agentId, productId, null));

        assertThat(message.metadataJson()).contains("referralMode").contains("NONE");
        assertThat(message.metadataJson()).doesNotContain("ref=");

        assertThatThrownBy(() -> support.sendProduct(conversationId,
                new SupportDtos.SendProductRequest(agentId, productId, "PERMANENT")))
                .hasMessageContaining("客服佣金走工资");
    }

    private UUID conversation(UUID assignedAgent) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO support_conversation (id, subject, status, locale, agent_id)
                VALUES (?, '咨询', 'OPEN', 'zh-CN', ?)
                """, id, assignedAgent);
        return id;
    }

    private UUID agent() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO staff_user (id, username, display_name, role, status, password_hash)
                VALUES (?, ?, '客服', 'CS', 'ACTIVE', 'x')
                """, id, "cs-" + id.toString().substring(0, 6));
        return id;
    }

    private UUID product() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '客服推荐商品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1990, 'CNY')
                """, skuId, productId, "CS-" + skuId.toString().substring(0, 6));
        return productId;
    }

    @Test
    void conversationAssignmentRequired() {
        UUID owner = agent();
        UUID conversationId = conversation(owner);
        UUID otherAgent = agent();
        assertThatThrownBy(() -> support.sendProduct(conversationId,
                new SupportDtos.SendProductRequest(otherAgent, product(), null)))
                .hasMessageContaining("会话");
    }
}
