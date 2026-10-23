package com.fullmall.platform.asset;

import com.fullmall.platform.asset.cos.CosAssetStorage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** COS 适配器：缺配置启动即失败、provider 固定、空内容/空 key 拒绝（不触网）。 */
class CosAssetStorageTest {

    @Test
    void missingConfigFailsFast() {
        assertThatThrownBy(() -> new CosAssetStorage("", "sk", "ap-shanghai", "b", "https://cdn"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("必须配置");
    }

    @Test
    void providerAndValidation() {
        AssetStorage storage = new CosAssetStorage("id", "sk", "ap-shanghai", "bucket", "https://cdn.test/");
        assertThat(storage.provider()).isEqualTo("cos");
        assertThatThrownBy(() -> storage.put("k", new byte[0], "image/png"))
                .hasMessageContaining("上传内容为空");
        assertThatThrownBy(() -> storage.put("", new byte[]{1}, "image/png"))
                .hasMessageContaining("key 不能为空");
    }
}
