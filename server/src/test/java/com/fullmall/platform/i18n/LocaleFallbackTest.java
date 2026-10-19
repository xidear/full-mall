package com.fullmall.platform.i18n;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocaleFallbackTest {

    @Test
    void requestedLocaleWinsWhenPresent() {
        assertThat(LocaleFallback.preferred("es-ES", "zh-CN")).isEqualTo("es-ES");
    }

    @Test
    void blankRequestFallsBackToConfiguredDefault() {
        assertThat(LocaleFallback.preferred(null, "zh-CN")).isEqualTo("zh-CN");
        assertThat(LocaleFallback.preferred("  ", "zh-CN")).isEqualTo("zh-CN");
    }

    @Test
    void defaultLocaleIsConfigurableNotHardcodedEnglish() {
        assertThat(LocaleFallback.preferred(null, "en")).isEqualTo("en");
        assertThat(LocaleFallback.preferred("fr", "en")).isEqualTo("fr");
    }

    @Test
    void malformedDefaultFailsLoudly() {
        // 只有请求语言缺失时才会用到站点主语言，因此这里传 null 请求
        assertThatThrownBy(() -> LocaleFallback.preferred(null, "chinese"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("主语言");
        assertThatThrownBy(() -> LocaleFallback.preferred(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedRequestedLocaleIsIgnoredInFavourOfDefault() {
        assertThat(LocaleFallback.preferred("not a locale!", "zh-CN")).isEqualTo("zh-CN");
    }

    @Test
    void normalizesCase() {
        assertThat(LocaleFallback.preferred("ZH-cn", "en")).isEqualTo("zh-CN");
        assertThat(LocaleFallback.preferred("ES-es", "en")).isEqualTo("es-ES");
    }

    @Test
    void localizedContentIsPreferred() {
        var resolution = LocaleFallback.resolve("es", true, true);
        assertThat(resolution.locale()).isEqualTo("es");
        assertThat(resolution.source()).isEqualTo(LocaleFallback.Source.LOCALIZED);
    }

    @Test
    void missingLocalizedContentFallsBackToEnglishDefault() {
        var resolution = LocaleFallback.resolve("es", false, true);
        assertThat(resolution.locale()).isEqualTo("en");
        assertThat(resolution.source()).isEqualTo(LocaleFallback.Source.DEFAULT);
    }

    @Test
    void chineseIsALocalizedLanguageNotTheDefault() {
        var resolution = LocaleFallback.resolve("zh-CN", false, true);
        assertThat(resolution.locale()).isEqualTo("en");
        assertThat(resolution.source()).isEqualTo(LocaleFallback.Source.DEFAULT);
    }

    @Test
    void missingEnglishDefaultIsADataIntegrityErrorNotAPlaceholder() {
        assertThatThrownBy(() -> LocaleFallback.resolve("es", false, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("英文默认");
    }

    @Test
    void publishingRequiresEnglishDefaultContent() {
        LocaleFallback.requirePublishable(true, "sku:1");
        assertThatThrownBy(() -> LocaleFallback.requirePublishable(false, "sku:1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不能发布");
    }
}