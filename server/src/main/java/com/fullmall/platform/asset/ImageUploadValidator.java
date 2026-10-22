package com.fullmall.platform.asset;

import com.fullmall.platform.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传校验与 key 生成（纯逻辑，便于测试）。
 *
 * <p>只接受图片类型；超过大小上限直接拒绝；key 按 `assets/yyyyMM/uuid.ext` 生成，
 * 避免用原始文件名（防路径穿越与重名覆盖）。
 */
@Component
public class ImageUploadValidator {

    private static final Set<String> ALLOWED = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/gif");
    private static final Map<String, String> EXT = Map.of(
            "image/png", "png", "image/jpeg", "jpg", "image/webp", "webp", "image/gif", "gif");

    private final long maxBytes;

    public ImageUploadValidator(@Value("${app.asset.max-bytes:5242880}") long maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalStateException("app.asset.max-bytes 必须大于 0，实际=" + maxBytes);
        }
        this.maxBytes = maxBytes;
    }

    public String requireValidKey(String contentType, byte[] content) {
        String type = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(type)) {
            throw ApiException.badRequest("ASSET_TYPE_NOT_ALLOWED",
                    "只允许上传图片（png/jpeg/webp/gif），收到：" + contentType);
        }
        if (content == null || content.length == 0) {
            throw ApiException.badRequest("ASSET_EMPTY", "上传内容为空");
        }
        if (content.length > maxBytes) {
            throw ApiException.badRequest("ASSET_TOO_LARGE",
                    "文件 " + content.length + " 字节，超过上限 " + maxBytes);
        }
        return "assets/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + EXT.get(type);
    }
}
