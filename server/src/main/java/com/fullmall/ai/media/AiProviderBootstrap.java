package com.fullmall.ai.media;

import com.fullmall.platform.secret.SecretCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

/**
 * 启动时确保"本机 Ollama"作为默认文本/embedding 供应商存在。
 *
 * <p>只做插入（已存在则不动），不会覆盖后台手工配置；地址可用
 * `app.ai.ollama.base-url` 覆盖（容器内访问宿主机时改成 host 地址）。
 */
@Configuration
// H2 建库测试（spring.flyway.enabled=false）没有业务表，不能跑种子
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class AiProviderBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AiProviderBootstrap.class);

    @Bean
    public ApplicationRunner aiProviderBootstrapRunner(
            JdbcTemplate jdbc, SecretCipher cipher,
            @Value("${app.ai.ollama.base-url:http://localhost:11434/v1}") String baseUrl,
            @Value("${app.ai.ollama.text-model:qwen2.5:0.5b}") String textModel,
            @Value("${app.ai.ollama.embedding-model:nomic-embed-text}") String embeddingModel,
            @Value("${app.ai.mock.base-url:}") String mockBaseUrl) {
        return args -> {
            insertIfAbsent(jdbc, cipher, "ollama-chat", MediaKind.TEXT, "openai-chat",
                    baseUrl, textModel, 500);
            insertIfAbsent(jdbc, cipher, "ollama-embedding", MediaKind.TEXT, "openai-embedding",
                    baseUrl, embeddingModel, 510);
            if (mockBaseUrl != null && !mockBaseUrl.isBlank()) {
                // 本地官方 mock（wiremock）：无真实 Key 也能跑通生成链路
                insertIfAbsent(jdbc, cipher, "mock-image", MediaKind.IMAGE, "openai-images",
                        mockBaseUrl + "/v1", "mock-image-1", 900);
                insertIfAbsent(jdbc, cipher, "mock-image-qwen", MediaKind.IMAGE, "dashscope-multimodal",
                        mockBaseUrl + "/v1/multimodal", "qwen-image-edit", 910);
                insertIfAbsent(jdbc, cipher, "mock-video", MediaKind.VIDEO, "ark-video",
                        mockBaseUrl + "/ark", "seedance-mock", 900);
            }
            log.info("AI 默认供应商就绪：ollama-chat({}) / ollama-embedding({}) / mock={}",
                    textModel, embeddingModel, mockBaseUrl);
        };
    }

    private void insertIfAbsent(JdbcTemplate jdbc, SecretCipher cipher, String code, MediaKind kind,
                                String protocol, String baseUrl, String model, int priority) {
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ai_provider WHERE code = ?", Integer.class, code);
        if (exists != null && exists > 0) {
            return;
        }
        jdbc.update("""
                INSERT INTO ai_provider (id, kind, code, name, protocol, endpoint, api_key, model,
                                         extra, enabled, priority)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, '{}'::jsonb, TRUE, ?)
                """, UUID.randomUUID(), kind.name(), code,
                "ollama".equals(code) ? "本机 Ollama" : ("本机 Ollama " + model),
                protocol, baseUrl, cipher.encrypt("ollama"), model, priority);
    }
}
