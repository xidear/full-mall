package com.fullmall.platform.asset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 图片校验：白名单、大小、key 规则（不使用原始文件名）。 */
class ImageUploadValidatorTest {

    @Test
    void acceptsImagesAndBuildsSafeKey() {
        var validator = new ImageUploadValidator(1024);

        String key = validator.requireValidKey("image/PNG", new byte[]{1, 2});

        assertThat(key).startsWith("assets/").endsWith(".png");
        assertThat(key).doesNotContain("..");
    }

    @Test
    void rejectsNonImageOversizeAndEmpty() {
        var validator = new ImageUploadValidator(4);

        assertThatThrownBy(() -> validator.requireValidKey("application/pdf", new byte[]{1}))
                .hasMessageContaining("只允许上传图片");
        assertThatThrownBy(() -> validator.requireValidKey("image/png", new byte[5]))
                .hasMessageContaining("超过上限");
        assertThatThrownBy(() -> validator.requireValidKey("image/png", new byte[0]))
                .hasMessageContaining("上传内容为空");
    }
}
