package com.fullmall.ai.app;

import com.fullmall.ai.config.AiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 生产模式装配：真实对话模型 + PG 知识库召回。
 *
 * <p>召回方式由 `app.ai.retrieval` **显式选择**：
 * <ul>
 *   <li>`vector`：查询先算嵌入再按余弦重排（需要嵌入服务；未配置会在调用时明确报错）；</li>
 *   <li>`keyword`：只做关键词召回（无嵌入服务时使用，须在部署文档里写明）。</li>
 * </ul>
 * 不做"嵌入失败自动降级成关键词"——那会让召回质量悄悄变差且无人察觉。
 */
@Configuration
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "prod")
public class ProdAiBeans {

    @Bean
    ChatModel chatModel(AiProperties properties) {
        return new HttpChatModel(properties);
    }

    @Bean
    KnowledgeRetriever knowledgeRetriever(PgKnowledgeStore store, AiProperties properties) {
        if (properties.isVectorRetrieval()) {
            return (query, topK) -> {
                throw com.fullmall.platform.error.ApiException.conflict("AI_EMBEDDING_REQUIRED",
                        "当前为 vector 召回，但未接入嵌入服务；请配置 app.ai.embedding-* 或显式设置 app.ai.retrieval=keyword");
            };
        }
        return (query, topK) -> store.search(query, topK);
    }
}
