package com.fullmall.platform.asset.qiniu;

import com.fullmall.platform.asset.AssetStorage;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;

/**
 * 七牛云对象存储适配器（官方 SDK 7.16.x）。
 *
 * <p>可剥离：本包 + `qiniu-java-sdk` 依赖 + `app.asset.*` 配置即可整体移除。
 * 缺配置构造即失败，不静默降级为本地存储。
 */
public class QiniuAssetStorage implements AssetStorage {

    private final Auth auth;
    private final String bucket;
    private final String domain;
    private final UploadManager uploadManager;
    private final BucketManager bucketManager;

    public QiniuAssetStorage(String accessKey, String secretKey, String bucket, String domain) {
        if (isBlank(accessKey) || isBlank(secretKey) || isBlank(bucket) || isBlank(domain)) {
            throw new IllegalStateException(
                    "app.asset.storage=qiniu 时必须配置 access-key/secret-key/bucket/domain");
        }
        this.auth = Auth.create(accessKey, secretKey);
        this.bucket = bucket;
        this.domain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        Configuration configuration = new Configuration(Region.autoRegion());
        this.uploadManager = new UploadManager(configuration);
        this.bucketManager = new BucketManager(auth, configuration);
    }

    @Override
    public String provider() {
        return "qiniu";
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
            uploadManager.put(content, key, auth.uploadToken(bucket));
            return url(key);
        } catch (QiniuException error) {
            throw new IllegalStateException("七牛上传失败：" + key + "（" + error.getMessage() + "）", error);
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            bucketManager.delete(bucket, key);
            return true;
        } catch (QiniuException error) {
            if (error.code() == 612) {
                return false;
            }
            throw new IllegalStateException("七牛删除失败：" + key + "（" + error.getMessage() + "）", error);
        }
    }

    private String url(String key) {
        return domain + "/" + key;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
