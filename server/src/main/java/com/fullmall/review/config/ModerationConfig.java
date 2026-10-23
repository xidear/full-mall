package com.fullmall.review.config;

import com.fullmall.review.moderation.ContentModeration;
import com.fullmall.review.moderation.http.HttpModerationAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 审核通道装配：provider=http 时启用；未配置则不创建（策略层走全人工）。 */
@Configuration
public class ModerationConfig {

    @Bean
    public ContentModeration contentModeration(
            @Value("${app.review.moderation.provider:}") String provider,
            @Value("${app.review.moderation.endpoint:}") String endpoint,
            @Value("${app.review.moderation.api-key:}") String apiKey) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(java.util.Locale.ROOT);
        if (!"http".equals(normalized)) {
            return null;
        }
        return new HttpModerationAdapter(endpoint, apiKey);
    }
}
