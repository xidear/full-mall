package com.fullmall.platform.asset;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 图片上传（二进制）：走 {@link AssetStorage} 端口，返回可访问 URL。
 *
 * <p>与 `asset` 模块的分工：那边管"素材元数据/归属人"（URL 级别），
 * 这里只负责"把字节存到对象存储并给出 URL"。
 */
@RestController
@RequestMapping("/admin/v1/media")
public class MediaUploadController {

    private final AssetStorage storage;
    private final ImageUploadValidator validator;

    public MediaUploadController(AssetStorage storage, ImageUploadValidator validator) {
        this.storage = storage;
        this.validator = validator;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        try {
            byte[] content = file.getBytes();
            String key = validator.requireValidKey(file.getContentType(), content);
            String url = storage.put(key, content, file.getContentType());
            return Map.of("url", url, "key", key,
                    "bytes", content.length, "provider", storage.provider());
        } catch (IOException error) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "ASSET_READ_FAILED", "读取上传文件失败：" + error.getMessage());
        }
    }
}
