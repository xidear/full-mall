package com.fullmall.ai.media;

import com.fullmall.ai.media.adapter.DashScopeImageAdapter;
import com.fullmall.ai.media.adapter.DashScopeVideoAdapter;
import com.fullmall.ai.media.adapter.MediaGenerationAdapter;
import com.fullmall.ai.media.adapter.OpenAiChatAdapter;
import com.fullmall.ai.media.adapter.OpenAiEmbeddingAdapter;
import com.fullmall.ai.media.adapter.OpenAiImagesAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 生成适配器装配：新增协议只加一个 Bean，供应商数据全部来自数据库。 */
@Configuration
public class AiMediaConfig {

    @Bean
    public MediaGenerationAdapter openAiImagesAdapter() {
        return new OpenAiImagesAdapter();
    }

    @Bean
    public MediaGenerationAdapter dashScopeImageAdapter() {
        return new DashScopeImageAdapter();
    }

    @Bean
    public MediaGenerationAdapter dashScopeVideoAdapter() {
        return new DashScopeVideoAdapter();
    }

    @Bean
    public MediaGenerationAdapter openAiChatAdapter() {
        return new OpenAiChatAdapter();
    }

    @Bean
    public MediaGenerationAdapter openAiEmbeddingAdapter() {
        return new OpenAiEmbeddingAdapter();
    }
}
