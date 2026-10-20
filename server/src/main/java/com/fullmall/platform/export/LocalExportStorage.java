package com.fullmall.platform.export;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 本地磁盘实现（开发/单机）。生产应替换为对象存储实现，接口不变。
 *
 * <p>key 会做路径穿越校验：不允许 `..` 或绝对路径，避免任务表被污染后读到任意文件。
 */
@Component
public class LocalExportStorage implements ExportStorage {

    private final Path baseDir;

    public LocalExportStorage(@Value("${app.export.storage-dir:}") String storageDir) {
        this.baseDir = Path.of(storageDir == null || storageDir.isBlank()
                ? System.getProperty("java.io.tmpdir") + "/fullmall-exports"
                : storageDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(String key, byte[] content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(baseDir);
            Files.write(target, content);
            return key;
        } catch (IOException error) {
            throw new IllegalStateException("导出文件写入失败: " + key, error);
        }
    }

    @Override
    public byte[] read(String key) {
        Path target = resolve(key);
        if (!Files.isReadable(target)) {
            throw new IllegalStateException("导出文件不存在或不可读: " + key);
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException error) {
            throw new IllegalStateException("导出文件读取失败: " + key, error);
        }
    }

    @Override
    public boolean exists(String key) {
        return key != null && Files.isReadable(resolve(key));
    }

    /** 删除最后修改时间早于保留期的文件；返回实际删除数量。 */
    @Override
    public int deleteOlderThan(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("导出文件保留天数必须大于 0，实际=" + days);
        }
        if (!Files.isDirectory(baseDir)) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - java.time.Duration.ofDays(days).toMillis();
        int removed = 0;
        try (var stream = Files.walk(baseDir)) {
            var candidates = stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < cutoff;
                        } catch (IOException error) {
                            throw new IllegalStateException("读取文件时间失败: " + path, error);
                        }
                    })
                    .toList();
            for (Path path : candidates) {
                Files.delete(path);
                removed++;
            }
        } catch (IOException error) {
            throw new IllegalStateException("清理导出文件失败: " + baseDir, error);
        }
        return removed;
    }

    private Path resolve(String key) {
        if (key == null || key.isBlank() || key.contains("..") || key.startsWith("/")) {
            throw new IllegalArgumentException("非法导出文件 key: " + key);
        }
        Path resolved = baseDir.resolve(key).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("导出文件 key 越界: " + key);
        }
        return resolved;
    }
}
