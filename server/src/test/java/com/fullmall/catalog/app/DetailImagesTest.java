package com.fullmall.catalog.app;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 详情图片按语言替换：守住"换对该换的、不动不该动的"。
 */
class DetailImagesTest {

    @Test
    void replacesOnlyMappedImages() {
        String html = "<p>说明</p><img src=\"/assets/cn.png\" alt=\"图\"><img src=\"/assets/plain.png\">";

        String localized = DetailImages.localize(html, Map.of("/assets/cn.png", "/assets/en.png"));

        assertThat(localized).contains("src=\"/assets/en.png\"");
        assertThat(localized).contains("src=\"/assets/plain.png\"");
        assertThat(localized).contains("alt=\"图\"");
        assertThat(localized).contains("说明");
    }

    @Test
    void keepsHtmlUntouchedWhenNoMapping() {
        String html = "<p><img src=\"/assets/cn.png\"></p>";
        assertThat(DetailImages.localize(html, Map.of())).isEqualTo(html);
        assertThat(DetailImages.localize(html, null)).isEqualTo(html);
        assertThat(DetailImages.localize("", Map.of("/a", "/b"))).isEmpty();
    }
}
