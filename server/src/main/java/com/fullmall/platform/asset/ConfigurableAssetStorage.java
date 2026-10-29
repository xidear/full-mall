package com.fullmall.platform.asset;

import com.fullmall.platform.asset.cos.CosAssetStorage;
import com.fullmall.platform.asset.local.LocalAssetStorage;
import com.fullmall.platform.asset.qiniu.QiniuAssetStorage;
import com.fullmall.setting.app.SystemSettingService;

import java.util.Locale;

/**
 * 素材存储路由：**服务商以数据库 system_setting.storage_provider 为准**，
 * 环境变量只作为未配置时的启动兜底。
 *
 * <p>这样运营在后台切换 local / qiniu / cos 不需要改容器环境变量或重启；
 * 密钥仍走环境变量/密钥管理，切换后下一次上传即生效。
 */
public class ConfigurableAssetStorage implements AssetStorage {

    private final LocalAssetStorage local;
    private final SystemSettingService settings;
    private final String fallbackProvider;
    private final String qiniuAccessKey;
    private final String qiniuSecretKey;
    private final String qiniuBucket;
    private final String qiniuDomain;
    private final String cosSecretId;
    private final String cosSecretKey;
    private final String cosRegion;
    private final String cosBucket;
    private final String cosDomain;

    public ConfigurableAssetStorage(LocalAssetStorage local, SystemSettingService settings,
                                   String fallbackProvider,
                                   String qiniuAccessKey, String qiniuSecretKey,
                                   String qiniuBucket, String qiniuDomain,
                                   String cosSecretId, String cosSecretKey,
                                   String cosRegion, String cosBucket, String cosDomain) {
        this.local = local;
        this.settings = settings;
        this.fallbackProvider = fallbackProvider == null ? "local" : fallbackProvider.trim();
        this.qiniuAccessKey = qiniuAccessKey;
        this.qiniuSecretKey = qiniuSecretKey;
        this.qiniuBucket = qiniuBucket;
        this.qiniuDomain = qiniuDomain;
        this.cosSecretId = cosSecretId;
        this.cosSecretKey = cosSecretKey;
        this.cosRegion = cosRegion;
        this.cosBucket = cosBucket;
        this.cosDomain = cosDomain;
    }

    @Override
    public String provider() {
        return delegate().provider();
    }

    @Override
    public String put(String key, byte[] content, String contentType) {
        return delegate().put(key, content, contentType);
    }

    @Override
    public boolean delete(String key) {
        return delegate().delete(key);
    }

    private AssetStorage delegate() {
        String configured = settings.rawValue("storage_provider");
        String provider = configured == null || configured.isBlank()
                ? fallbackProvider
                : configured.trim().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "", "local", "mock" -> local;
            case "qiniu" -> new QiniuAssetStorage(qiniuAccessKey, qiniuSecretKey, qiniuBucket, qiniuDomain);
            case "cos" -> new CosAssetStorage(cosSecretId, cosSecretKey, cosRegion, cosBucket, cosDomain);
            default -> throw new IllegalStateException(
                    "storage_provider=" + provider + " 尚未接入（可选 local / qiniu / cos）");
        };
    }
}
