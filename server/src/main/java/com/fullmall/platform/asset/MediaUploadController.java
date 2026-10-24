package com.fullmall.platform.asset;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
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
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public MediaUploadController(AssetStorage storage, ImageUploadValidator validator,
                                 org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.storage = storage;
        this.validator = validator;
        this.jdbc = jdbc;
    }

    /**
     * @param purpose 素材用途：PRODUCT（商品图）/SPEC（规格图）/VIDEO（视频封面等）/
     *                OTHER_SYSTEM（审批附件等，**不进素材库**）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "PRODUCT") String purpose,
                                      org.springframework.security.core.Authentication authentication) {
        try {
            String normalized = purpose == null ? "PRODUCT"
                    : purpose.trim().toUpperCase(java.util.Locale.ROOT);
            if (!java.util.Set.of("PRODUCT", "SPEC", "VIDEO", "OTHER_SYSTEM").contains(normalized)) {
                throw com.fullmall.platform.error.ApiException.badRequest(
                        "MEDIA_PURPOSE_INVALID", "purpose 只能是 PRODUCT/SPEC/VIDEO/OTHER_SYSTEM");
            }
            byte[] content = file.getBytes();
            String key = validator.requireValidKey(file.getContentType(), content);
            String url = storage.put(key, content, file.getContentType());
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("url", url);
            result.put("key", key);
            result.put("bytes", content.length);
            result.put("provider", storage.provider());
            result.put("purpose", normalized);
            if (!"OTHER_SYSTEM".equals(normalized)) {
                UUID assetId = UUID.randomUUID();
                jdbc.update("""
                        INSERT INTO asset (id, name, url, kind, owner_username, purpose)
                        VALUES (?, ?, ?, 'IMAGE', ?, ?)
                        """, assetId, file.getOriginalFilename() == null ? "上传素材" : file.getOriginalFilename(),
                        url, authentication.getName(), normalized);
                result.put("assetId", assetId.toString());
            } else {
                result.put("assetId", null);
            }
            return result;
        } catch (IOException error) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "ASSET_READ_FAILED", "读取上传文件失败：" + error.getMessage());
        }
    }
}
