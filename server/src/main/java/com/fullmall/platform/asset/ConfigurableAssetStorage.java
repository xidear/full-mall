package com.fullmall.platform.asset;

import com.fullmall.platform.asset.cos.CosAssetStorage;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.secret.IntegrationConfigService;
import com.fullmall.platform.asset.local.LocalAssetStorage;
import com.fullmall.platform.asset.qiniu.QiniuAssetStorage;
import com.fullmall.setting.app.SystemSettingService;

import java.util.Locale;
import java.util.Map;

/**
 * 素材存储路由：服务商与参数**只以「第三方服务」里保存的集成配置为准**，不做任何兜底。
 *
 * <p>未启用服务商、或选了本机磁盘却没填目录，一律抛带错误码的失败；
 * 不允许"默默存到某个默认目录"——那会让运营以为存到对象存储，实际落在容器临时盘，换机器即丢图。
 *
 * <p>切换服务商不需要重启：每次上传按当下配置路由。
 */
public class ConfigurableAssetStorage implements AssetStorage {

    private final IntegrationConfigService integrations;
    private final String localPublicBaseUrl;
    private final String qiniuAccessKey;
    private final String qiniuSecretKey;
    private final String qiniuBucket;
    private final String qiniuDomain;
    private final String cosSecretId;
    private final String cosSecretKey;
    private final String cosRegion;
    private final String cosBucket;
    private final String cosDomain;

    public ConfigurableAssetStorage(IntegrationConfigService integrations, String localPublicBaseUrl,
                                   String qiniuAccessKey, String qiniuSecretKey,
                                   String qiniuBucket, String qiniuDomain,
                                   String cosSecretId, String cosSecretKey,
                                   String cosRegion, String cosBucket, String cosDomain) {
        this.integrations = integrations;
        this.localPublicBaseUrl = localPublicBaseUrl;
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
        // 服务商与参数都以「第三方服务」里保存的集成为准。
        // 以前这里在未配置时静默回落到本机磁盘，导致"什么都没配也能上传成功"——
        // 那是把配置缺失隐藏起来，运营以为存到对象存储，实际落在容器临时盘上，换机器就丢。
        String provider = integrations.enabledProvider("storage");
        if (provider == null || provider.isBlank()) {
            throw ApiException.serviceUnavailable("ASSET_STORAGE_NOT_CONFIGURED",
                    "尚未启用素材存储服务商：请到「第三方服务 → 对象存储」选择服务商并保存后再上传");
        }
        Map<String, String> config = integrations.resolveAll("storage");
        return switch (provider.trim().toLowerCase(Locale.ROOT)) {
            case "local" -> {
                String directory = config.get("directory");
                if (directory == null || directory.isBlank()) {
                    throw ApiException.serviceUnavailable("ASSET_STORAGE_DIRECTORY_NOT_CONFIGURED",
                            "素材存储选择的是本机磁盘，但未配置存储目录：请到「第三方服务 → 对象存储」填写目录后保存");
                }
                yield new LocalAssetStorage(directory.trim(), localPublicBaseUrl);
            }
            case "qiniu" -> new QiniuAssetStorage(qiniuAccessKey, qiniuSecretKey, qiniuBucket, qiniuDomain);
            case "cos" -> new CosAssetStorage(cosSecretId, cosSecretKey, cosRegion, cosBucket, cosDomain);
            default -> throw ApiException.serviceUnavailable("ASSET_STORAGE_PROVIDER_UNSUPPORTED",
                    "素材存储服务商 " + provider + " 尚未接入（可选 local / qiniu / cos）");
        };
    }
}
