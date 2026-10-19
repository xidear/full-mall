package com.fullmall.catalog.app;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 翻译 Provider 抽象（I18N-003）。
 *
 * <p>演示/未配置时使用 {@link MockTranslationProvider}：**明确是 Mock**（返回带 `[mock]` 前缀的文本），
 * 生产接入 DeepL/Google/Azure 时替换实现，缺 key 应启动失败而不是静默返回原文。
 */
public interface TranslationProvider {

    String translate(String sourceText, String targetLocale);

    String name();

    @Component
    class MockTranslationProvider implements TranslationProvider {

        @Override
        public String translate(String sourceText, String targetLocale) {
            if (sourceText == null || targetLocale == null || targetLocale.isBlank()) {
                throw new IllegalArgumentException("源文与目标语言不能为空");
            }
            return "[mock:" + targetLocale.toLowerCase(Locale.ROOT) + "] " + sourceText;
        }

        @Override
        public String name() {
            return "mock";
        }
    }
}