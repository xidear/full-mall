package com.fullmall.export;

import com.fullmall.platform.export.OssExportStorage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OSS 存储：配置缺失必须**启动即失败**，不能拖到导出时才 500。
 * （真实上传需要凭据，故此处只验证"配置契约"这一确定性行为。）
 */
class OssExportStorageConfigTest {

    @Test
    void missingConfigFailsFast() {
        assertThatThrownBy(() -> new OssExportStorage("", "bucket", "ak", "sk"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new OssExportStorage("oss-cn-hangzhou.aliyuncs.com", "", "ak", "sk"))
                .hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new OssExportStorage("oss-cn-hangzhou.aliyuncs.com", "b", "", "sk"))
                .hasMessageContaining("必须配置");
    }
}
