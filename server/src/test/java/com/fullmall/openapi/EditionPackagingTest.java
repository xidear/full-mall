package com.fullmall.openapi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 构建分版验收（AI-1）：社区版产物**不得包含** AI 模块类；企业版必须包含。
 *
 * <p>不用 Spring 上下文（打包形态与数据库无关），直接看类路径上有没有 `com.fullmall.ai`。
 * 构建形态来自 Maven profile 注入的 `APP_EDITION`（见 pom 的 surefire 配置）。
 */
class EditionPackagingTest {

    private String edition() {
        String value = System.getenv("APP_EDITION");
        return value == null || value.isBlank() ? "community" : value.trim();
    }

    private boolean aiClassPresent() {
        try {
            Class.forName("com.fullmall.ai.api.AdminAiController");
            return true;
        } catch (ClassNotFoundException expected) {
            return false;
        }
    }

    @Test
    void editionMatchesPackagedClasses() {
        String edition = edition();
        assertThat(edition).as("APP_EDITION 只能是 community 或 enterprise").isIn("community", "enterprise");

        if ("community".equals(edition)) {
            assertThat(aiClassPresent())
                    .as("社区版（GitHub 开源产物）不允许包含 AI 模块类")
                    .isFalse();
        } else {
            assertThat(aiClassPresent())
                    .as("企业版必须包含 AI 模块类")
                    .isTrue();
        }
    }
}
