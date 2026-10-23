package com.fullmall.platform.asset.cos;

import com.fullmall.platform.asset.AssetStorage;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;

import java.io.ByteArrayInputStream;

/** 腾讯云 COS 适配器（官方 SDK）。缺配置构造即失败，不降级。 */
public class CosAssetStorage implements AssetStorage {

    private final COSClient client;
    private final String bucket;
    private final String domain;

    public CosAssetStorage(String secretId, String secretKey, String region,
                           String bucket, String domain) {
        if (isBlank(secretId) || isBlank(secretKey) || isBlank(region)
                || isBlank(bucket) || isBlank(domain)) {
            throw new IllegalStateException(
                    "app.asset.storage=cos 时必须配置 secret-id/secret-key/region/bucket/domain");
        }
        this.client = new COSClient(new BasicCOSCredentials(secretId, secretKey),
                new ClientConfig(new Region(region)));
        this.bucket = bucket;
        this.domain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
    }

    @Override
    public String provider() {
        return "cos";
    }

    @Override
    public String put(String key, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("上传内容为空");
        }
        if (isBlank(key)) {
            throw new IllegalArgumentException("存储 key 不能为空");
        }
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.length);
            if (contentType != null && !contentType.isBlank()) {
                metadata.setContentType(contentType);
            }
            client.putObject(new PutObjectRequest(bucket, key,
                    new ByteArrayInputStream(content), metadata));
            return domain + "/" + key;
        } catch (Exception error) {
            throw new IllegalStateException("COS 上传失败：" + key + "（" + error.getMessage() + "）", error);
        }
    }

    @Override
    public boolean delete(String key) {
        if (isBlank(key)) {
            throw new IllegalArgumentException("存储 key 不能为空");
        }
        try {
            client.deleteObject(bucket, key);
            return true;
        } catch (Exception error) {
            throw new IllegalStateException("COS 删除失败：" + key + "（" + error.getMessage() + "）", error);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
