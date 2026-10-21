package com.fullmall.ai.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 未配置模型时的占位实现：**明确抛 503**，绝不返回空字符串或规则模板冒充回答。
 *
 * <p>为什么需要它：`AnswerService` 依赖两个端口，若不提供实现，Spring 启动会失败；
 * 而 AI 未配置恰恰是常态（演示环境可能先不接模型）。启动不能因此挂掉，
 * 但调用时必须给出可读的失败原因。
 */
@Configuration
public class UnconfiguredAiBeans {

    /**
     * 演示模式：用内存关键词检索；生产模式才需要 pgvector + 嵌入模型。
     * 用 `@ConditionalOnProperty(app.ai.mode=demo)` 显式声明，不靠默认值猜。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "app.ai.mode", havingValue = "demo")
    KnowledgeRetriever demoKnowledgeRetriever(InMemoryKnowledgeStore store,
                                              com.fullmall.ai.config.AiProperties properties) {
        return (query, topK) -> store.search(query, topK > 0 ? topK : properties.getTopK());
    }

    @Bean
    KnowledgeRetriever knowledgeRetriever() {
        return new KnowledgeRetriever() {
            @Override
            public List<KnowledgeChunk> retrieve(String query, int topK) {
                throw ApiException.conflict("AI_NOT_CONFIGURED",
                        "AI 未配置向量检索（app.ai.embedding-* 缺失或索引未建立），无法检索知识库");
            }
        };
    }

    @Bean
    ChatModel chatModel() {
        return new ChatModel() {
            @Override
            public String complete(String system, String user) {
                throw ApiException.conflict("AI_NOT_CONFIGURED",
                        "AI 未配置对话模型（app.ai.chat-* 缺失），无法生成回答");
            }
        };
    }
}
