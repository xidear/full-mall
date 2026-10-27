package com.fullmall.platform.asset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 本地素材静态映射：LocalAssetStorage 返回 /assets/<key>，key 本身以 assets/ 开头，
 * 因此浏览器看到 /assets/assets/yyyyMM/xxx。这里把 /assets/** 映射到 app.asset.local.directory。
 */
@Configuration
public class StaticAssetConfig implements WebMvcConfigurer {

    private final String location;

    public StaticAssetConfig(@Value("${app.asset.local.directory:./data/assets}") String directory) {
        Path path = Path.of(directory).toAbsolutePath().normalize();
        String uri = path.toUri().toString();
        this.location = uri.endsWith("/") ? uri : uri + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
