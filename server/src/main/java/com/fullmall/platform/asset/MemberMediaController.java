package com.fullmall.platform.asset;

import com.fullmall.platform.validation.ValidationRules;
import com.fullmall.setting.app.SystemSettingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 顾客端媒体上传（评价晒图/晒视频）。
 *
 * <p>与管理端 {@link MediaUploadController} 的三点差别：
 * <ol>
 *   <li>体积上限用另一套更小的值（后台「素材上传」里配置），避免顾客把商品详情视频传上来；</li>
 *   <li>**不登记进素材库**：顾客照片不属于商品素材，混进去会淹没运营的素材库；</li>
 *   <li>对象 key 前缀是 `reviews/`，便于按用途做生命周期清理与配额统计。</li>
 * </ol>
 *
 * <p>鉴权由 MemberAuthFilter 负责：不带/带错 token 直接 401，这里再兜一层，避免匿名写入对象存储。
 */
@RestController
@RequestMapping("/api/v1/media")
public class MemberMediaController {

    private final AssetStorage storage;
    private final MediaUploadValidator validator;
    private final SystemSettingService settings;

    public MemberMediaController(AssetStorage storage, MediaUploadValidator validator,
                                 SystemSettingService settings) {
        this.storage = storage;
        this.validator = validator;
        this.settings = settings;
    }

    /** 当前生效的顾客端上传规则：前端据此渲染 accept 与"最大 xxMB"提示。 */
    @GetMapping("/limits")
    public Map<String, Object> limits() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imageTypes", ValidationRules.UPLOAD_IMAGE_TYPES);
        result.put("videoTypes", ValidationRules.UPLOAD_VIDEO_TYPES);
        result.put("imageMaxBytes", settings.memberImageMaxBytes());
        result.put("videoMaxBytes", settings.memberVideoMaxBytes());
        return result;
    }

    @PostMapping
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new com.fullmall.platform.error.ApiException(
                    HttpStatus.UNAUTHORIZED, "MEMBER_REQUIRED", "请先登录后再上传图片或视频");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException error) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "ASSET_READ_FAILED", "读取上传文件失败：" + error.getMessage());
        }
        MediaUploadValidator.Validated validated =
                validator.requireValid(MediaUploadValidator.Scope.MEMBER, file.getContentType(), content);
        String key = "reviews/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + validated.extension();
        String url = storage.put(key, content, validated.contentType());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", url);
        result.put("key", key);
        result.put("bytes", content.length);
        result.put("provider", storage.provider());
        result.put("kind", validated.kind().name());
        result.put("contentType", validated.contentType());
        return result;
    }
}
