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

    /**
     * 素材来源。E2E 单独一类：测试上传的 m.png / e2e-ref-*.png 混进运营素材库后
     * 既搜不到也删不完，列表默认要把它们挡住。
     */
    private static final Set<String> SOURCES = Set.of("UPLOAD", "AI", "IMPORT", "E2E");

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
                                      @RequestParam(defaultValue = "UPLOAD") String source,
                                      Authentication authentication) {
        String normalizedSource = source == null ? "UPLOAD" : source.trim().toUpperCase(Locale.ROOT);
        if (!SOURCES.contains(normalizedSource)) {
            throw ApiException.badRequest("MEDIA_SOURCE_INVALID",
                    "source 只能是 " + String.join("/", SOURCES.stream().sorted().toList())
                            + "，实际=" + source);
        }
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

        // 内容哈希在写存储**之前**算：同一张图重复上传应当复用已有素材，
        // 而不是又存一份字节、又插一条素材行（素材库会被 m.png 这类重复图撑爆）。
        String sha256 = sha256Hex(content);
        if (!normalized.equals("OTHER_SYSTEM")) {
            var existing = jdbc.query("""
                    SELECT id, url FROM asset WHERE sha256 = ? AND deleted_at IS NULL LIMIT 1
                    """, (rs, rowNum) -> Map.of(
                    "id", rs.getObject("id", UUID.class).toString(),
                    "url", rs.getString("url")), sha256);
            if (!existing.isEmpty()) {
                Map<String, Object> deduped = new LinkedHashMap<>();
                deduped.put("url", existing.get(0).get("url"));
                deduped.put("assetId", existing.get(0).get("id"));
                deduped.put("deduplicated", true);
                deduped.put("sha256", sha256);
                deduped.put("purpose", normalized);
                deduped.put("source", normalizedSource);
                return deduped;
            }
        }
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
        String displayName = file.getOriginalFilename() == null ? "上传素材" : file.getOriginalFilename();
        // 原子 upsert：并发上传同一份文件时，先 SELECT 再 INSERT 会撞 sha256 唯一键（实测 500）。
        // 插入失败即说明别人先插了，回查并当作"去重"返回。
        int inserted = jdbc.update("""
                INSERT INTO asset (id, name, url, kind, owner_username, purpose, source, sha256, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                ON CONFLICT (sha256) WHERE sha256 IS NOT NULL AND deleted_at IS NULL DO NOTHING
                """, assetId, displayName, url, validated.kind().name(),
                authentication.getName(), normalized, normalizedSource, sha256);
        if (inserted == 0) {
            var existing = jdbc.query("""
                    SELECT id, url FROM asset WHERE sha256 = ? AND deleted_at IS NULL LIMIT 1
                    """, (rs, rowNum) -> Map.of(
                    "id", rs.getObject("id", UUID.class).toString(),
                    "url", rs.getString("url")), sha256);
            if (existing.isEmpty()) {
                throw ApiException.badRequest("ASSET_DEDUPE_RACE",
                        "并发上传同内容文件时未能定位已存在素材，请重试；sha256=" + sha256);
            }
            Map<String, Object> deduped = new LinkedHashMap<>();
            deduped.put("url", existing.get(0).get("url"));
            deduped.put("assetId", existing.get(0).get("id"));
            deduped.put("deduplicated", true);
            deduped.put("sha256", sha256);
            deduped.put("purpose", normalized);
            deduped.put("source", normalizedSource);
            return deduped;
        }
        // 物理对象与逻辑素材分开存：换存储/CDN 时只改 asset_object，不动 asset 的引用关系
        jdbc.update("""
                INSERT INTO asset_object (id, asset_id, storage, object_key, url, bytes, content_type)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), assetId, storage.provider(), key, url,
                (long) content.length, validated.contentType());
        result.put("assetId", assetId.toString());
        result.put("sha256", sha256);
        result.put("deduplicated", false);
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

    /** 内容哈希：素材去重与"同一张图重复上传"的唯一依据。 */
    private static String sha256Hex(byte[] content) {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(content));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("JVM 缺少 SHA-256：素材去重无法进行", error);
        }
    }
}
