package com.fullmall.platform.asset;

import com.fullmall.platform.asset.local.LocalAssetStorage;
import com.fullmall.setting.app.SystemSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 素材存储装配：**服务商由数据库 system_setting.storage_provider 决定**，
 * 环境变量 app.asset.storage 仅作为未配置时的启动兜底。
 *
 * <p>切换服务商不需要重启：下一次上传会按新的 DB 配置路由；缺密钥时明确失败，不静默回落 local。
 */
@Configuration
public class AssetStorageConfig {

    // 只装配委托器；真实服务商由 ConfigurableAssetStorage.delegate 在每次上传时查 system_setting.storage_provider，
    // 所以后台切换不必重启。代价是密钥缺失要到上传时才抛错——有意为之，不能静默降级回 local。
    @Bean
    public AssetStorage assetStorage(
            // 本机磁盘访问基址（部署参数）：key 以 assets/ 开头，URL 形如 /assets/assets/202610/x.jpg，
            // 绝对化交给 MediaUrlResolver；生产应配 CDN 域名。
            // 存储目录不在这里读：那是运营在「第三方服务」里配的（见 ConfigurableAssetStorage）。
            @Value("${app.asset.local.public-base-url:/assets}") String localBaseUrl,
            @Value("${app.asset.qiniu.access-key:}") String qiniuAccessKey,
            @Value("${app.asset.qiniu.secret-key:}") String qiniuSecretKey,
            @Value("${app.asset.qiniu.bucket:}") String qiniuBucket,
            @Value("${app.asset.qiniu.domain:}") String qiniuDomain,
            @Value("${app.asset.cos.secret-id:}") String cosSecretId,
            @Value("${app.asset.cos.secret-key:}") String cosSecretKey,
            @Value("${app.asset.cos.region:}") String cosRegion,
            @Value("${app.asset.cos.bucket:}") String cosBucket,
            @Value("${app.asset.cos.domain:}") String cosDomain,
            com.fullmall.platform.secret.IntegrationConfigService integrations) {
        return new ConfigurableAssetStorage(integrations, localBaseUrl,
                qiniuAccessKey, qiniuSecretKey, qiniuBucket, qiniuDomain,
                cosSecretId, cosSecretKey, cosRegion, cosBucket, cosDomain);
    }
}
