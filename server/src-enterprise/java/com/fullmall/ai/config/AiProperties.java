package com.fullmall.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模块配置。
 *
 * <p>关键原则：**未配置模型时不做任何静默兜底**——请求会收到 503 与明确原因，
 * 而不是空答案或规则模板伪装成 AI。
 */
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** 模块总开关；社区版没有这些类，企业版默认也关闭，需显式开启。 */
    private boolean enabled = false;
    /** `demo`（关键词检索 + 抽取式回答，无需 GPU）或 `prod`（向量检索 + 模型生成）。 */
    private String mode = "demo";
    /** OpenAI 兼容的对话服务地址，例如 http://192.168.1.15:8000/v1 */
    private String chatBaseUrl;
    private String chatModel;
    /** OpenAI 兼容的嵌入服务地址（可与对话相同） */
    private String embeddingBaseUrl;
    private String embeddingModel;
    /** 检索返回条数 */
    private int topK = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getChatBaseUrl() {
        return chatBaseUrl;
    }

    public void setChatBaseUrl(String chatBaseUrl) {
        this.chatBaseUrl = chatBaseUrl;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getEmbeddingBaseUrl() {
        return embeddingBaseUrl;
    }

    public void setEmbeddingBaseUrl(String embeddingBaseUrl) {
        this.embeddingBaseUrl = embeddingBaseUrl;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    /** 是否具备可用的对话/嵌入配置；缺任一项都算未就绪。 */
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isReady() {
        if (enabled && "demo".equalsIgnoreCase(mode == null ? "" : mode.trim())) {
            return true;   // 演示模式不依赖外部模型
        }
        return enabled
                && chatBaseUrl != null && !chatBaseUrl.isBlank()
                && chatModel != null && !chatModel.isBlank()
                && embeddingBaseUrl != null && !embeddingBaseUrl.isBlank()
                && embeddingModel != null && !embeddingModel.isBlank();
    }
}
