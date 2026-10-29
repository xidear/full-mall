package com.fullmall.platform.asset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 静态资源映射。
 *
 * <ul>
 *   <li>/assets/**：LocalAssetStorage 返回 /assets/&lt;key&gt;，key 本身以 assets/ 开头，
 *       因此浏览器看到 /assets/assets/yyyyMM/xxx；映射到 app.asset.local.directory。</li>
 *   <li>/images/**：演示商品的本地图片（与 storefront 同一份），由 API 统一提供，
 *       避免"图片只在某个前端的 public 目录里"导致其它端裂图。</li>
 * </ul>
 */
@Configuration
public class StaticAssetConfig implements WebMvcConfigurer {

    private final String location;

    // 相对目录按进程工作目录解析：API 必须从仓库根启动，否则与 LocalAssetStorage 的写入目录不同，上传成功但 404。
    // URI 末尾必须带 "/"：缺斜杠时 Spring 会把最后一段当成文件名前缀。
    public StaticAssetConfig(@Value("${app.asset.local.directory:./data/assets}") String directory) {
        Path path = Path.of(directory).toAbsolutePath().normalize();
        String uri = path.toUri().toString();
        this.location = uri.endsWith("/") ? uri : uri + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 3600 秒缓存：换素材要用新 key，同名覆盖会让客户端一小时内仍看到旧图。
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600);
        // 兼容历史数据：早期 seeds/配置把图片地址写成 /media/images/...（app.media.public-base-url 的旧默认值带 /media）
        // 这里把 /media/** 也映射到同一份静态资源，避免"域名修好了但路径仍然 404"。
        registry.addResourceHandler("/media/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}
