package com.fullmall.platform.asset.local;

import com.fullmall.platform.asset.AssetStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 本地磁盘素材存储（开发/单机部署用）。
 *
 * <p>key 必须是相对路径且不允许 `..`——否则等于任意文件写入。
 * 返回 URL 由 `public-base-url` 拼接（生产应指向 CDN 或反向代理）。
 */
public class LocalAssetStorage implements AssetStorage {

    private final Path root;
    private final String publicBaseUrl;

    public LocalAssetStorage(String directory, String publicBaseUrl) {
        if (directory == null || directory.isBlank()) {
            throw new IllegalStateException("app.asset.local.directory 不能为空");
        }
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalStateException("app.asset.local.public-base-url 不能为空");
        }
        this.root = Path.of(directory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    @Override
    public String provider() {
        return "local";
    }

    @Override
    public String put(String key, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("上传内容为空");
        }
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException error) {
            throw new IllegalStateException("本地素材写入失败：" + key + "（" + error.getMessage() + "）", error);
        }
        return publicBaseUrl + "/" + key;
    }

    @Override
    public boolean delete(String key) {
        try {
            return Files.deleteIfExists(resolve(key));
        } catch (IOException error) {
            throw new IllegalStateException("本地素材删除失败：" + key + "（" + error.getMessage() + "）", error);
        }
    }

    private Path resolve(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("存储 key 不能为空");
        }
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法 key（越出存储目录）：" + key);
        }
        return target;
    }
}
