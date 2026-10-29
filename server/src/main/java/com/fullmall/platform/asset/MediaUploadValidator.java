package com.fullmall.platform.asset;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.validation.ValidationRules;
import com.fullmall.setting.app.SystemSettingService;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 素材上传的类型与体积校验（纯判断 + 体积上限读库，便于测试）。
 *
 * <p>判定依据是 **Apache Tika 的 magic-bytes 嗅探结果**，不是客户端上报的 Content-Type：
 * 改后缀或伪造请求头都绕不过去。客户端声明的类型只用于一致性核对（声明 image/png 实际是 mp4
 * 这种混淆会被拒绝），落库扩展名一律取自嗅探结果，避免"文件名是 .jpg、内容却是别的"。
 *
 * <p>白名单取自 {@link ValidationRules}（镜像 docs/validation.schema.json），刻意不含
 * `image/svg+xml`：SVG 是 XML 文档，从对象存储直出会给同源带来脚本执行风险，也无法栅格化生成缩略图。
 * HEIC/HEIF 能被识别但需要浏览器先转码，这里明确报错而不是入库一个打不开的文件。
 *
 * <p>体积上限按"谁在上传"区分：管理端与顾客端各有一套，读 {@link SystemSettingService}
 * （后台可改，下一次上传即生效）。
 */
@Component
public class MediaUploadValidator {

    /** 上传者场景：决定用哪一套体积上限。 */
    public enum Scope {
        /** 管理端：商品图、规格图、主图视频、素材库。 */
        ADMIN,
        /** 顾客端：评价晒图/晒视频。 */
        MEMBER
    }

    /** 落库类型：与 asset.kind 的取值一致（IMAGE / VIDEO）。 */
    public enum Kind {
        IMAGE, VIDEO
    }

    /** 校验结果：只有通过校验才会生成存储 key。 */
    public record Validated(Kind kind, String contentType, String extension, int bytes) {
    }

    /**
     * 嗅探器：Tika 的实例是线程安全的，构造一次复用。
     * 只用 magic-bytes 识别（不解析容器内容），因此开销是读前若干字节。
     */
    private static final Tika TIKA = new Tika();

    /** 扩展名由嗅探结果决定，绝不取原始文件名。 */
    private static final Map<String, String> EXT = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp",
            "image/gif", "gif",
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/quicktime", "mov");

    /** Tika 可能返回的同义类型，统一成白名单里的写法再比对。 */
    private static final Map<String, String> ALIASES = Map.of(
            "image/jpg", "image/jpeg",
            "image/pjpeg", "image/jpeg",
            "image/x-png", "image/png",
            "video/x-m4v", "video/mp4",
            "video/mov", "video/quicktime",
            "video/x-quicktime", "video/quicktime");

    /** 能识别但浏览器普遍不能直接显示，需要先转码；返回可操作的错误而不是存进去。 */
    private static final Set<String> NEEDS_BROWSER_CONVERSION = Set.of("image/heic", "image/heif");

    private final SystemSettingService settings;

    public MediaUploadValidator(SystemSettingService settings) {
        this.settings = settings;
    }

    /**
     * 校验一次上传。
     *
     * @param scope               上传者场景，决定体积上限
     * @param declaredContentType 客户端上报的 Content-Type，可为空；只用于一致性核对
     * @param content             文件字节；为空直接失败
     * @return 嗅探出的类型、落库扩展名与字节数
     * @throws ApiException 类型不在白名单、声明与实际不符、需要转码、为空或超体积
     */
    public Validated requireValid(Scope scope, String declaredContentType, byte[] content) {
        if (content == null || content.length == 0) {
            throw ApiException.badRequest("ASSET_EMPTY", "上传内容为空（0 字节）");
        }
        String sniffed = normalize(TIKA.detect(content));
        String declared = normalize(declaredContentType);
        // 只核对大类（image/video）：具体类型以嗅探为准（扩展名也取自嗅探结果），
        // 这样 png 被声明成 image/x-png 这类同义写法不会被误杀；
        // 而"把 mp4 改名成 .png"（声明 image/png、内容 video/mp4）会在这里被拦下。
        if (!declared.isBlank() && !declared.equals("application/octet-stream")
                && !majorType(declared).equals(majorType(sniffed))) {
            throw ApiException.badRequest("ASSET_TYPE_MISMATCH",
                    "声明类型 " + declaredContentType + " 与实际内容 " + sniffed + " 不一致");
        }

        Kind kind;
        long limit;
        if (ValidationRules.UPLOAD_IMAGE_TYPES.contains(sniffed)) {
            kind = Kind.IMAGE;
            limit = scope == Scope.ADMIN ? settings.adminImageMaxBytes() : settings.memberImageMaxBytes();
        } else if (ValidationRules.UPLOAD_VIDEO_TYPES.contains(sniffed)) {
            kind = Kind.VIDEO;
            limit = scope == Scope.ADMIN ? settings.adminVideoMaxBytes() : settings.memberVideoMaxBytes();
        } else if (NEEDS_BROWSER_CONVERSION.contains(sniffed)) {
            throw ApiException.badRequest("ASSET_HEIC_NOT_CONVERTED",
                    "HEIC/HEIF 需要先转成 JPEG 再上传（网页端会自动转码；直接调接口请先转换），实际内容=" + sniffed);
        } else {
            throw ApiException.badRequest("ASSET_TYPE_NOT_ALLOWED",
                    "只允许上传 " + allowedDescription() + "，实际内容=" + sniffed
                            + (declared.isBlank() ? "" : "（声明 " + declaredContentType + "）")
                            + ("image/svg+xml".equals(sniffed) ? "；SVG 暂不支持，请导出为 png/jpeg 再上传" : ""));
        }

        if (content.length > limit) {
            throw ApiException.badRequest("ASSET_TOO_LARGE",
                    "文件 " + content.length + " 字节，超过" + (kind == Kind.VIDEO ? "视频" : "图片")
                            + "上限 " + limit + " 字节（≈" + megabytes(limit) + "MB）；"
                            + "可在后台「系统设置 → 素材上传」调整");
        }
        return new Validated(kind, sniffed, EXT.get(sniffed), content.length);
    }

    /** 前端展示与错误文案共用的一段允许类型描述。 */
    public static String allowedDescription() {
        StringJoiner images = new StringJoiner("/");
        for (String type : ValidationRules.UPLOAD_IMAGE_TYPES) {
            images.add(displayName(type));
        }
        StringJoiner videos = new StringJoiner("/");
        for (String type : ValidationRules.UPLOAD_VIDEO_TYPES) {
            videos.add(displayName(type));
        }
        return "图片（" + images + "）或视频（" + videos + "）";
    }

    /** 展示用扩展名：落库仍用 EXT，这里只求"用户认得"（jpeg 不写成 jpg，免得被当成不支持 jpg）。 */
    private static String displayName(String contentType) {
        return "image/jpeg".equals(contentType) ? "jpeg" : EXT.get(contentType);
    }

    private static String normalize(String contentType) {
        if (contentType == null) {
            return "";
        }
        // 去掉 "image/png; charset=binary" 这类参数，再统一小写后查别名表。
        String base = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        return ALIASES.getOrDefault(base, base);
    }

    private static String majorType(String contentType) {
        int slash = contentType.indexOf('/');
        return slash < 0 ? contentType : contentType.substring(0, slash);
    }

    private static String megabytes(long bytes) {
        return String.format(Locale.ROOT, "%.1f", bytes / 1024.0 / 1024.0);
    }
}
