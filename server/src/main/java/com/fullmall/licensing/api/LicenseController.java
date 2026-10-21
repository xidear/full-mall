package com.fullmall.licensing.api;

import com.fullmall.licensing.app.LicenseService;
import com.fullmall.licensing.app.LicenseStatus;
import com.fullmall.licensing.config.LicenseProperties;
import com.fullmall.platform.error.ApiException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 许可管理：状态查询与**上传新许可证即生效**（无需重启）。
 *
 * <p>上传仅做"写入文件 + 立即重验"，验签失败会把原状态一起返回，避免运营以为上传成功。
 */
@RestController
@RequestMapping("/admin/v1/license")
public class LicenseController {

    private final LicenseService licenseService;
    private final LicenseProperties properties;

    public LicenseController(LicenseService licenseService, LicenseProperties properties) {
        this.licenseService = licenseService;
        this.properties = properties;
    }

    @GetMapping("/status")
    public LicenseStatus status() {
        return licenseService.status();
    }

    public record UploadRequest(String content) {
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestBody UploadRequest body) {
        if (body == null || body.content() == null || body.content().isBlank()) {
            throw ApiException.badRequest("LICENSE_CONTENT_EMPTY", "许可证内容不能为空");
        }
        if (properties.getFile() == null || properties.getFile().isBlank()) {
            throw ApiException.conflict("LICENSE_FILE_NOT_CONFIGURED",
                    "未配置 app.license.file，无法写入许可证");
        }
        Path target = Path.of(properties.getFile());
        try {
            Files.writeString(target, body.content());
        } catch (IOException error) {
            throw ApiException.conflict("LICENSE_WRITE_FAILED",
                    "写入许可证失败：" + error.getMessage());
        }
        LicenseStatus status = licenseService.refresh().status();
        // 上传成功但验签失败时，status 会是 INVALID —— 把真实结果返回，不假装成功
        return Map.of("status", status, "saved", true);
    }
}
