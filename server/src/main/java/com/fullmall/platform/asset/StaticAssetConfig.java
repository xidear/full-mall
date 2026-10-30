package com.fullmall.platform.asset;

import com.fullmall.platform.secret.IntegrationConfigService;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.nio.file.Path;
import java.util.Map;

/**
 * 静态资源映射。
 *
 * <ul>
 *   <li>/assets/**：映射到「第三方服务 → 对象存储」里配置的本机磁盘目录。</li>
 *   <li>/images/**、/media/**：随 jar 打包的演示图片与历史路径兼容。</li>
 * </ul>
 *
 * <p>目录必须**按请求解析**而不是启动时定死：运营在后台改存储目录后不应要求重启；
 * 更重要的是——启动时读一个"默认目录"会让"没配置也能上传"的假象重新出现。
 * 目录未配置时这里直接 404（上传侧会以 ASSET_STORAGE_DIRECTORY_NOT_CONFIGURED 明确失败）。
 */
@Configuration
public class StaticAssetConfig implements WebMvcConfigurer {

    private final IntegrationConfigService integrations;

    public StaticAssetConfig(IntegrationConfigService integrations) {
        this.integrations = integrations;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 3600 秒缓存：换素材要用新 key，同名覆盖会让客户端一小时内仍看到旧图。
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("file:/")
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) {
                        Path root = configuredRoot();
                        if (root == null) {
                            return null;
                        }
                        FileSystemResource candidate = new FileSystemResource(root.resolve(resourcePath).normalize());
                        if (!candidate.exists()) {
                            return null;
                        }
                        // 防目录穿越：解析后的路径必须仍在配置目录之下
                        return candidate.getFile().toPath().normalize().startsWith(root) ? candidate : null;
                    }
                });
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600);
        // 兼容历史数据：早期 seeds/配置把图片地址写成 /media/images/...
        registry.addResourceHandler("/media/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }

    /**
     * 当前生效的素材目录；未启用或未配置目录时返回 null（调用方据此 404）。
     *
     * <p>这里必须先看 enabled：直接调 resolveAll() 在"服务商存在但未启用"时会抛
     * INTEGRATION_NOT_CONFIGURED（409 Conflict），而一个静态图片取不到应当是 404，
     * 不是业务冲突。语义要对得上，否则排查时会被错误码带偏。
     */
    private Path configuredRoot() {
        if (integrations.enabledProvider("storage") == null) {
            return null;
        }
        Map<String, String> config = integrations.resolveAll("storage");
        String directory = config.get("directory");
        if (directory == null || directory.isBlank()) {
            return null;
        }
        return Path.of(directory.trim()).toAbsolutePath().normalize();
    }
}
