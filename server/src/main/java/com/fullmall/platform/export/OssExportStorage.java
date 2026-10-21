package com.fullmall.platform.export;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 阿里云 OSS 导出存储（官方 SDK）。
 *
 * <p>启用条件：`app.export.storage=oss` 且配置齐 endpoint/bucket/密钥。
 * **配置缺失时构造即失败**（启动报错），而不是运行到导出时才 500——这类错误越早暴露越好。
 *
 * <p>删除策略：不主动删除历史对象，交给 OSS 生命周期规则（`deleteOlderThan` 返回 0 并在日志说明），
 * 因此不会出现"本地删了、云端还在"的不一致。
 */
@Component
@ConditionalOnProperty(name = "app.export.storage", havingValue = "oss")
public class OssExportStorage implements ExportStorage {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(OssExportStorage.class);

    private final OSS client;
    private final String bucket;

    public OssExportStorage(
            @Value("${app.export.oss.endpoint:}") String endpoint,
            @Value("${app.export.oss.bucket:}") String bucket,
            @Value("${app.export.oss.access-key-id:}") String accessKeyId,
            @Value("${app.export.oss.access-key-secret:}") String accessKeySecret) {
        if (endpoint.isBlank() || bucket.isBlank()
                || accessKeyId.isBlank() || accessKeySecret.isBlank()) {
            throw new IllegalStateException(
                    "app.export.storage=oss 时必须配置 endpoint/bucket/access-key-id/access-key-secret");
        }
        this.bucket = bucket;
        this.client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    @Override
    public String store(String key, byte[] content) {
        client.putObject(bucket, key, new java.io.ByteArrayInputStream(content));
        return key;
    }

    @Override
    public byte[] read(String key) {
        try (OSSObject object = client.getObject(bucket, key);
             InputStream stream = object.getObjectContent()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            stream.transferTo(out);
            return out.toByteArray();
        } catch (java.io.IOException error) {
            throw new IllegalStateException("读取 OSS 对象失败：" + key, error);
        }
    }

    @Override
    public boolean exists(String key) {
        return client.doesObjectExist(bucket, key);
    }

    @Override
    public int deleteOlderThan(int days) {
        // 交给 OSS 生命周期规则；本地删除会与实际对象状态脱节
        log.info("OSS 导出清理由生命周期规则处理，跳过本地删除（保留 {} 天）", days);
        return 0;
    }
}
