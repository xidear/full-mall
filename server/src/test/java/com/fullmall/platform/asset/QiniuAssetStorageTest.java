package com.fullmall.platform.asset;

import com.fullmall.platform.asset.qiniu.QiniuAssetStorage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 七牛适配器：缺配置启动即失败、provider 固定、空内容拒绝（不发网络请求）。 */
class QiniuAssetStorageTest {

    @Test
    void missingConfigFailsFast() {
        assertThatThrownBy(() -> new QiniuAssetStorage("", "sk", "bucket", "https://cdn.example.com"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new QiniuAssetStorage("ak", "sk", "", "https://cdn.example.com"))
                .hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new QiniuAssetStorage("ak", "sk", "bucket", ""))
                .hasMessageContaining("必须配置");
    }

    @Test
    void providerAndValidationAreCorrect() {
        AssetStorage storage = new QiniuAssetStorage("ak", "sk", "bucket", "https://cdn.example.com/");

        assertThat(storage.provider()).isEqualTo("qiniu");
        assertThat(storage.getClass().getPackageName())
                .as("适配器必须独立成包，可整体删除")
                .isEqualTo("com.fullmall.platform.asset.qiniu");
        assertThatThrownBy(() -> storage.put("k", new byte[0], "image/png"))
                .hasMessageContaining("上传内容为空");
        assertThatThrownBy(() -> storage.put("", new byte[]{1}, "image/png"))
                .hasMessageContaining("key 不能为空");
    }
}
