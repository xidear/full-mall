package com.fullmall.platform.validation;

/**
 * 前端/后端共享的业务校验常量。
 *
 * <p>唯一来源是 `docs/validation.schema.json`；修改后需要运行
 * `npm run generate:validation` 生成前端常量，并由
 * `ValidationRulesContractTest` 校验本类与契约一致。
 */
public final class ValidationRules {

    // 镜像 docs/validation.schema.json 的 $defs 同名规则，由 ValidationRulesContractTest 逐条比对；
    // 改值必须先改 schema 再 cd web && npm run generate:validation，否则 web/packages/api-client/src/validation.ts 漂移。
    /** 不校验运营商号段：海外号码与虚拟号同样放行，号段判断交给短信服务商。 */
    public static final String PHONE_PATTERN = "^\\+?[0-9]{6,20}$";
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PIN_MIN_LENGTH = 4;
    public static final int PIN_MAX_LENGTH = 12;
    public static final int QUANTITY_MIN = 1;
    public static final int QUANTITY_MAX = 99;
    /** 单位是**分**（ADR-0004 金额一律最小货币单位整数）：1 = 0.01 元，不是 1 元。 */
    public static final int PRICE_MIN = 1;
    /** 须与 DB 列宽度相容，否则校验通过却在写库时才报长度错误。 */
    public static final int CONTACT_NAME_MAX = 64;
    /** 排掉空格与斜杠：编码会进 URL 和导出文件名，含分隔符会被截断或造成路径穿越。 */
    public static final String SKU_CODE_PATTERN = "^[A-Za-z0-9._-]{1,64}$";
    /** 全大写、无空格：扫码枪与人工报码时不产生大小写/空白歧义。 */
    public static final String COUPON_CODE_PATTERN = "^[A-Z0-9][A-Z0-9_-]{1,63}$";

    /**
     * 单条评价可附带的媒体条数上限（$defs.reviewMedia.maximum=9，图片与视频合计）。
     * 限制的理由是审核成本与列表页加载：顾客一次传几十条素材，运营看不完、页面也会卡。
     */
    public static final int REVIEW_MEDIA_MAX = 9;

    /**
     * 可入库的图片类型（schema $defs.assetUpload.imageTypes），按服务端 Apache Tika 嗅探出的
     * MIME 匹配。故意不含 image/svg+xml：SVG 是 XML 文档，从对象存储直出会带来同源脚本执行风险，
     * 且无法栅格化生成缩略图。
     */
    public static final java.util.List<String> UPLOAD_IMAGE_TYPES =
            java.util.List.of("image/png", "image/jpeg", "image/webp", "image/gif");
    /** 可入库的视频类型（schema assetUpload.videoTypes）：按容器格式白名单，不含音频与流式格式。 */
    public static final java.util.List<String> UPLOAD_VIDEO_TYPES =
            java.util.List.of("video/mp4", "video/webm", "video/quicktime");

    /** 管理端图片默认上限（5MB，schema assetUpload.adminImageMaxBytes）。后台可改，保存后下一次上传即生效。 */
    public static final long UPLOAD_ADMIN_IMAGE_DEFAULT_BYTES = 5L * 1024 * 1024;
    /** 管理端视频默认上限（10MB，schema assetUpload.adminVideoMaxBytes）。 */
    public static final long UPLOAD_ADMIN_VIDEO_DEFAULT_BYTES = 10L * 1024 * 1024;
    /** 顾客端（评价晒图）图片默认上限（2MB，schema assetUpload.memberImageMaxBytes）。 */
    public static final long UPLOAD_MEMBER_IMAGE_DEFAULT_BYTES = 2L * 1024 * 1024;
    /** 顾客端（评价）视频默认上限（5MB，schema assetUpload.memberVideoMaxBytes）。 */
    public static final long UPLOAD_MEMBER_VIDEO_DEFAULT_BYTES = 5L * 1024 * 1024;

    /** 后台可配上限的下限（schema 各属性的 minimum=100KB）：更小的图没有业务意义。 */
    public static final long UPLOAD_MIN_CONFIGURABLE_BYTES = 100L * 1024;
    /** 管理端图片可配上限的最大值（20MB，schema 的 maximum）。 */
    public static final long UPLOAD_ADMIN_IMAGE_MAX_CONFIGURABLE_BYTES = 20L * 1024 * 1024;
    /** 管理端视频可配上限的最大值（64MB）。等于 multipart 天花板：再大就过不了 Spring 的解析层。 */
    public static final long UPLOAD_ADMIN_VIDEO_MAX_CONFIGURABLE_BYTES = 64L * 1024 * 1024;
    /** 顾客端图片可配上限的最大值（5MB）。 */
    public static final long UPLOAD_MEMBER_IMAGE_MAX_CONFIGURABLE_BYTES = 5L * 1024 * 1024;
    /** 顾客端视频可配上限的最大值（20MB）。 */
    public static final long UPLOAD_MEMBER_VIDEO_MAX_CONFIGURABLE_BYTES = 20L * 1024 * 1024;

    /**
     * Spring multipart 单文件天花板（64MB，schema assetUpload.multipartCeilingBytes）。
     * 必须与 application.yml 的 spring.servlet.multipart.max-file-size 一致：后台可配上限一旦超过它，
     * 请求会在 multipart 解析阶段被拒，报错与体积配置看起来无关，排障极费时间。
     */
    public static final long UPLOAD_MULTIPART_CEILING_BYTES = 64L * 1024 * 1024;

    private ValidationRules() {
    }
}
