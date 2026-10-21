package com.fullmall.ai.app;

import java.util.UUID;

/**
 * 知识片段：AI 回答的**唯一事实来源**。
 *
 * @param sourceType PRODUCT / DOCUMENT / POLICY / FAQ
 * @param sourceId   商品、文档或政策 id（答复里要回引，客服可核对）
 * @param title      人类可读标题（如"某商品 · 规格与价格"）
 * @param content    已切分的正文，包含价格、单位、有效期等原文数字
 * @param version    知识版本（商品更新时间/文档版本），用于判断是否过期
 */
public record KnowledgeChunk(
        UUID id,
        String sourceType,
        String sourceId,
        String title,
        String content,
        String version) {
}
