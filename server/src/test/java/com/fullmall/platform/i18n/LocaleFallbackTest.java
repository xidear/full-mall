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
        assertThatThrownBy(() -> LocaleFallback.preferred("es", "chinese"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("默认语言");
        assertThatThrownBy(() -> LocaleFallback.preferred("es", null))
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
    void sourceIsSelfWhenLocalizedContentExists() {
        var resolution = LocaleFallback.resolve("es", "zh-CN", true, true);
        assertThat(resolution.locale()).isEqualTo("es");
        assertThat(resolution.source()).isEqualTo(LocaleFallback.Source.SELF);
    }

    @Test
    void sourceIsDefaultWhenOnlyDefaultContentExists() {
        var resolution = LocaleFallback.resolve("es", "zh-CN", false, true);
        assertThat(resolution.locale()).isEqualTo("zh-CN");
        assertThat(resolution.source()).isEqualTo(LocaleFallback.Source.DEFAULT);
    }

    @Test
    void sourceIsPlaceholderWhenNothingExists() {
        var resolution = LocaleFallback.resolve("es", "zh-CN", false, false);
        assertThat(resolution.source()).isEqualTo(LocaleFallback.Source.PLACEHOLDER);
    }

    @Test
    void requestedEqualToDefaultCountsAsSelf() {
        var resolution = LocaleFallback.resolve("zh-CN", "zh-CN", false, true);
        assertThat(resolution.source()).isEqualTo(LocaleFallback.Source.SELF);
    }
}