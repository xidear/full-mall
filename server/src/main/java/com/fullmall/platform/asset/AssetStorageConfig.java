package com.fullmall.platform.asset;

import com.fullmall.platform.asset.local.LocalAssetStorage;
import com.fullmall.platform.asset.qiniu.QiniuAssetStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 素材存储装配：`app.asset.storage=local|qiniu`。
 *
 * <p>未指定时默认 local（开发可用）；指定 qiniu 但缺配置 → **启动失败**，不静默回落到 local。
 */
@Configuration
public class AssetStorageConfig {

    @Bean
    public AssetStorage assetStorage(
            @Value("${app.asset.storage:local}") String provider,
            @Value("${app.asset.local.directory:./data/assets}") String localDirectory,
            @Value("${app.asset.local.public-base-url:/assets}") String localBaseUrl,
            @Value("${app.asset.qiniu.access-key:}") String qiniuAccessKey,
            @Value("${app.asset.qiniu.secret-key:}") String qiniuSecretKey,
            @Value("${app.asset.qiniu.bucket:}") String qiniuBucket,
            @Value("${app.asset.qiniu.domain:}") String qiniuDomain) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase();
        return switch (normalized) {
            case "qiniu" -> new QiniuAssetStorage(qiniuAccessKey, qiniuSecretKey, qiniuBucket, qiniuDomain);
            case "local", "" -> new LocalAssetStorage(localDirectory, localBaseUrl);
            default -> throw new IllegalStateException(
                    "不支持的 app.asset.storage=" + provider + "（可选 local / qiniu）");
        };
    }
}
