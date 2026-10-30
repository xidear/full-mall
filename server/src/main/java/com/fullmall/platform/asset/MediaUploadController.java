package com.fullmall.platform.asset;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.validation.ValidationRules;
import com.fullmall.setting.app.SystemSettingService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 管理端素材上传（二进制）：走 {@link AssetStorage} 端口，返回可访问 URL，并把结果登记进素材库。
 *
 * <p>与 `asset` 模块的分工：那边管"素材元数据/归属人"（URL 级别），这里只负责
 * "把字节存到对象存储并给出 URL"。类型与体积由 {@link MediaUploadValidator} 判定，
 * 上限读后台配置（系统设置 → 素材上传），改完不需要重启。
 *
 * <p>kind 由嗅探结果决定（IMAGE / VIDEO），不再一律写 IMAGE：视频素材必须能在素材库里
 * 被筛选与预览。purpose 保留业务含义（商品图/规格图/视频/审批附件），与 kind 正交。
 */
@RestController
@RequestMapping("/admin/v1/media")
public class MediaUploadController {

    /** 进素材库的用途；OTHER_SYSTEM 只存对象、不入库（审批附件等）。 */
    private static final Set<String> PURPOSES =
            Set.of("PRODUCT", "SPEC", "VIDEO", "OTHER_SYSTEM");

    private final AssetStorage storage;
    private final MediaUploadValidator validator;
    private final SystemSettingService settings;
    private final JdbcTemplate jdbc;
    private final com.fullmall.asset.app.AssetImageIndexService imageIndex;

    public MediaUploadController(AssetStorage storage, MediaUploadValidator validator,
                                 SystemSettingService settings, JdbcTemplate jdbc,
                                 com.fullmall.asset.app.AssetImageIndexService imageIndex) {
        this.storage = storage;
        this.validator = validator;
        this.settings = settings;
        this.jdbc = jdbc;
        this.imageIndex = imageIndex;
    }

    /**
     * 当前生效的上传规则（类型白名单 + 体积上限）。
     *
     * <p>前端用它渲染 accept 与"最大 xxMB"提示，避免把上限再手写一份在前端；
     * 前端拿到的是展示值，真正的权威判定仍在上传接口里。
     */
    @GetMapping("/limits")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> limits() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imageTypes", ValidationRules.UPLOAD_IMAGE_TYPES);
        result.put("videoTypes", ValidationRules.UPLOAD_VIDEO_TYPES);
        result.put("imageMaxBytes", settings.adminImageMaxBytes());
        result.put("videoMaxBytes", settings.adminVideoMaxBytes());
        return result;
    }

    /**
     * @param purpose 素材用途：PRODUCT（商品图）/SPEC（规格图）/VIDEO（主图视频与封面）/
     *                OTHER_SYSTEM（审批附件等，**不进素材库**）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "PRODUCT") String purpose,
                                      Authentication authentication) {
        String normalized = purpose == null ? "PRODUCT"
                : purpose.trim().toUpperCase(Locale.ROOT);
        if (!PURPOSES.contains(normalized)) {
            throw ApiException.badRequest("MEDIA_PURPOSE_INVALID",
                    "purpose 只能是 " + String.join("/", PURPOSES.stream().sorted().toList())
                            + "，实际=" + purpose);
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException error) {
            throw ApiException.badRequest("ASSET_READ_FAILED", "读取上传文件失败：" + error.getMessage());
        }
        MediaUploadValidator.Validated validated =
                validator.requireValid(MediaUploadValidator.Scope.ADMIN, file.getContentType(), content);
        String key = "assets/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + validated.extension();
        String url = storage.put(key, content, validated.contentType());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", url);
        result.put("key", key);
        result.put("bytes", content.length);
        result.put("provider", storage.provider());
        result.put("purpose", normalized);
        result.put("kind", validated.kind().name());
        result.put("contentType", validated.contentType());
        if (normalized.equals("OTHER_SYSTEM")) {
            result.put("assetId", null);
            return result;
        }
        UUID assetId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO asset (id, name, url, kind, owner_username, purpose)
                VALUES (?, ?, ?, ?, ?, ?)
                """, assetId, file.getOriginalFilename() == null ? "上传素材" : file.getOriginalFilename(),
                url, validated.kind().name(), authentication.getName(), normalized);
        result.put("assetId", assetId.toString());
        // 图片顺手建"图像内容向量"，这样素材一上传就能被"用一句话搜图"命中；
        // 没配多模态供应商时跳过（能力可选），但要记一笔，不假装建好了。
        if ("IMAGE".equals(validated.kind().name())) {
            try {
                imageIndex.index(assetId, null);
                result.put("imageIndexed", true);
            } catch (RuntimeException error) {
                result.put("imageIndexed", false);
                result.put("imageIndexError", error.getMessage());
            }
        }
        return result;
    }
}
