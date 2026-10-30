package com.fullmall.platform.export;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.secret.IntegrationConfigService;
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

    private final IntegrationConfigService integrations;
    private final Path fixedDirectory;

    @org.springframework.beans.factory.annotation.Autowired
    public LocalExportStorage(IntegrationConfigService integrations) {
        this.integrations = integrations;
        this.fixedDirectory = null;
    }

    /** 仅供单元测试：显式给定目录，不读集成配置，也没有任何默认值。 */
    LocalExportStorage(Path fixedDirectory) {
        if (fixedDirectory == null) {
            throw new IllegalArgumentException("测试用目录不能为空");
        }
        this.integrations = null;
        this.fixedDirectory = fixedDirectory.toAbsolutePath().normalize();
    }

    /**
     * 导出目录只认「第三方服务 → 导出文件存储」里配置的值。
     *
     * <p>以前未配置时落到系统临时目录：api 与 worker 在不同容器里，临时目录互不可见，
     * 于是任务显示 SUCCEEDED、下载却报 EXPORT_FILE_MISSING。现在未配置直接失败。
     * 另外目录不允许放在公开静态资源目录下——导出文件含顾客手机号。
     */
    /** 供调用方"创建即校验"用：配置不完整时立刻抛出可读错误。 */
    public void assertConfigured() {
        baseDir();
    }

    private Path baseDir() {
        if (fixedDirectory != null) {
            return fixedDirectory;
        }
        if (integrations.enabledProvider("export") == null) {
            throw ApiException.serviceUnavailable("EXPORT_STORAGE_NOT_CONFIGURED",
                    "尚未启用导出文件存储：请到「第三方服务 → 导出文件存储」配置目录并启用");
        }
        String directory = integrations.resolveAll("export").get("directory");
        if (directory == null || directory.isBlank()) {
            throw ApiException.serviceUnavailable("EXPORT_STORAGE_DIRECTORY_NOT_CONFIGURED",
                    "导出文件存储未配置目录：请到「第三方服务 → 导出文件存储」填写后启用");
        }
        return Path.of(directory.trim()).toAbsolutePath().normalize();
    }

    @Override
    public String store(String key, byte[] content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(baseDir());
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
        if (!Files.isDirectory(baseDir())) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - java.time.Duration.ofDays(days).toMillis();
        int removed = 0;
        try (var stream = Files.walk(baseDir())) {
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
            throw new IllegalStateException("清理导出文件失败: " + baseDir(), error);
        }
        return removed;
    }

    private Path resolve(String key) {
        if (key == null || key.isBlank() || key.contains("..") || key.startsWith("/")) {
            throw new IllegalArgumentException("非法导出文件 key: " + key);
        }
        Path resolved = baseDir().resolve(key).normalize();
        if (!resolved.startsWith(baseDir())) {
            throw new IllegalArgumentException("导出文件 key 越界: " + key);
        }
        return resolved;
    }
}
