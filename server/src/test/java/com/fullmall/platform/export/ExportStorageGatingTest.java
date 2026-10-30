package com.fullmall.platform.export;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.secret.IntegrationConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 导出存储的三态。回归点：以前未配置时落到系统临时目录，
 * api 与 worker 在容器间互不可见 → 任务 SUCCEEDED 但下载 EXPORT_FILE_MISSING。
 */
class ExportStorageGatingTest {

    private static IntegrationConfigService configs(String provider, Map<String, String> values) {
        IntegrationConfigService mock = mock(IntegrationConfigService.class);
        when(mock.enabledProvider(anyString())).thenReturn(provider);
        when(mock.resolveAll(anyString())).thenReturn(values);
        return mock;
    }

    @Test
    void unconfiguredExportStorageIsRejected() {
        var storage = new LocalExportStorage(configs(null, Map.of()));

        assertThatThrownBy(() -> storage.store("job.csv", "a,b".getBytes()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("尚未启用导出文件存储");
    }

    @Test
    void missingDirectoryIsRejected() {
        var storage = new LocalExportStorage(configs("local", Map.of()));

        assertThatThrownBy(() -> storage.assertConfigured())
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("未配置目录");
    }

    @Test
    void configuredDirectoryStoresAndReads(@TempDir Path dir) {
        var storage = new LocalExportStorage(configs("local", Map.of("directory", dir.toString())));

        storage.assertConfigured();
        storage.store("job.csv", "a,b".getBytes());

        assertThat(storage.exists("job.csv")).isTrue();
        assertThat(new String(storage.read("job.csv"))).isEqualTo("a,b");
    }
}
