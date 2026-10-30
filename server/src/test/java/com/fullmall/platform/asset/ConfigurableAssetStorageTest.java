package com.fullmall.platform.asset;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.secret.IntegrationConfigService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 素材存储的三态：未配置 / 配置不全 / 配置正确。
 *
 * <p>这三条是"什么都不配也能上传"那次事故的回归测试：
 * 少了任何一条，就又会有人把默认值加回来而不被发现。
 */
class ConfigurableAssetStorageTest {

    private static IntegrationConfigService configs(String provider, Map<String, String> values) {
        IntegrationConfigService mock = mock(IntegrationConfigService.class);
        when(mock.enabledProvider(anyString())).thenReturn(provider);
        when(mock.resolveAll(anyString())).thenReturn(values);
        return mock;
    }

    private static ConfigurableAssetStorage storage(IntegrationConfigService configs) {
        return new ConfigurableAssetStorage(configs, "/assets",
                "", "", "", "", "", "", "", "", "");
    }

    @Test
    void unconfiguredStorageIsRejected() {
        var target = storage(configs(null, Map.of()));

        assertThatThrownBy(() -> target.put("assets/x.png", new byte[]{1, 2}, "image/png"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("尚未启用素材存储");
    }

    @Test
    void localWithoutDirectoryIsRejected() {
        var target = storage(configs("local", Map.of()));

        assertThatThrownBy(() -> target.put("assets/x.png", new byte[]{1, 2}, "image/png"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("未配置存储目录");
    }

    @Test
    void localWithDirectoryWorks(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        var target = storage(configs("local", Map.of("directory", dir.toString())));

        String url = target.put("assets/x.png", new byte[]{1, 2}, "image/png");

        assertThat(url).contains("assets/x.png");
        assertThat(target.provider()).isEqualTo("local");
    }

    @Test
    void unknownProviderIsRejectedInsteadOfFallingBack() {
        var target = storage(configs("some-new-vendor", Map.of()));

        assertThatThrownBy(() -> target.put("assets/x.png", new byte[]{1, 2}, "image/png"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("尚未接入");
    }
}
