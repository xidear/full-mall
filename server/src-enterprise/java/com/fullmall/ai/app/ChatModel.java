package com.fullmall.ai.app;

/** 对话模型端口（OpenAI 兼容实现由 infra 提供）。 */
public interface ChatModel {

    /**
     * @param system 系统约束（含"只能依据资料回答"等硬性规则）
     * @param user   已拼接好资料与问题的用户消息
     * @return 模型输出原文
     */
    String complete(String system, String user);
}
