package com.fullmall.platform.secret;

import java.util.Map;

/**
 * 测试专用：把 storage/export 启用为本机目录。
 *
 * <p>这些能力在运行期只认 `integration_config`（不读环境变量/属性），
 * 所以测试必须通过同一个服务写入，不能再用 `app.asset.local.directory` 之类的旧属性。
 */
public final class TestIntegrationConfig {

    private TestIntegrationConfig() {
    }

    public static void enableLocal(IntegrationConfigService integrations, String capability,
                                   String directory) {
        integrations.save(capability, "local", Map.of("directory", directory), true, "test");
    }
}