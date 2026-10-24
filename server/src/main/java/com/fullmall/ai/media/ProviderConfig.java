package com.fullmall.ai.media;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * AI 供应商配置（数据库行）。
 *
 * @param protocol 调用协议：`openai-images` / `dashscope-image` / `dashscope-video` / `openai-chat`
 * @param extra    供应商私有参数（尺寸默认值、超时、水印等），不参与通用逻辑
 */
public record ProviderConfig(UUID id, MediaKind kind, String code, String name, String protocol,
                             String endpoint, String apiKey, String model, JsonNode extra,
                             boolean enabled, int priority) {
}
