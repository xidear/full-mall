package com.fullmall.analytics.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 能力装配：只有配置了 `app.ai.base-url/api-key/text-model` 才创建客户端；
 * 未配置时不注册，接口返回 503（现有行为），**不会假装生成内容**。
 */
@Configuration
public class AiCapabilityConfig {

    @Bean
    public OpenAiCompatClient openAiCompatClient(
            @Value("${app.ai.base-url:}") String baseUrl,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.text-model:}") String textModel,
            @Value("${app.ai.image-model:}") String imageModel) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        return new OpenAiCompatClient(baseUrl, apiKey, textModel, imageModel);
    }
}
