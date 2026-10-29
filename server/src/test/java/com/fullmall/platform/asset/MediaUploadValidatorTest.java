package com.fullmall.platform.asset;

import com.fullmall.platform.error.ApiException;
import com.fullmall.setting.app.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 上传类型/体积判定。守住的不变量：
 *
 * <ul>
 *   <li>类型以 magic bytes（Tika）为准，改后缀/伪造 Content-Type 不能把非图片塞进来；</li>
 *   <li>SVG 继续被拒（白名单不含 image/svg+xml）；</li>
 *   <li>视频能通过，并且用视频上限而不是图片上限；</li>
 *   <li>管理端与顾客端各用各的上限，顾客端更小；</li>
 *   <li>HEIC 明确要求先转码，不落一个浏览器打不开的文件。</li>
 * </ul>
 */
class MediaUploadValidatorTest {

    private static final long ADMIN_IMAGE_LIMIT = 5L * 1024 * 1024;
    private static final long ADMIN_VIDEO_LIMIT = 10L * 1024 * 1024;
    private static final long MEMBER_IMAGE_LIMIT = 2L * 1024 * 1024;
    private static final long MEMBER_VIDEO_LIMIT = 5L * 1024 * 1024;

    private final SystemSettingService settings = mock(SystemSettingService.class);
    private final MediaUploadValidator validator = new MediaUploadValidator(settings);

    @BeforeEach
    void stubLimits() {
        when(settings.adminImageMaxBytes()).thenReturn(ADMIN_IMAGE_LIMIT);
        when(settings.adminVideoMaxBytes()).thenReturn(ADMIN_VIDEO_LIMIT);
        when(settings.memberImageMaxBytes()).thenReturn(MEMBER_IMAGE_LIMIT);
        when(settings.memberVideoMaxBytes()).thenReturn(MEMBER_VIDEO_LIMIT);
    }

    @Test
    void sniffsPngEvenWhenClientClaimsOctetStream() {
        var validated = validator.requireValid(MediaUploadValidator.Scope.ADMIN,
                "application/octet-stream", png(1024));

        assertThat(validated.kind()).isEqualTo(MediaUploadValidator.Kind.IMAGE);
        assertThat(validated.contentType()).isEqualTo("image/png");
        // 扩展名取自嗅探结果，而不是原始文件名
        assertThat(validated.extension()).isEqualTo("png");
    }

    @Test
    void acceptsWebpBecauseItIsWhitelisted() {
        var validated = validator.requireValid(MediaUploadValidator.Scope.ADMIN,
                "image/webp", webp(1024));

        assertThat(validated.kind()).isEqualTo(MediaUploadValidator.Kind.IMAGE);
        assertThat(validated.extension()).isEqualTo("webp");
    }

    @Test
    void rejectsSvgWithActionableMessage() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.requireValid(MediaUploadValidator.Scope.ADMIN, "image/svg+xml", svg))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("image/svg+xml")
                .hasMessageContaining("png/jpeg/webp/gif")
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("ASSET_TYPE_NOT_ALLOWED");
    }

    @Test
    void acceptsMp4WithVideoLimit() {
        var validated = validator.requireValid(MediaUploadValidator.Scope.ADMIN,
                "video/mp4", mp4(1024));

        assertThat(validated.kind()).isEqualTo(MediaUploadValidator.Kind.VIDEO);
        assertThat(validated.extension()).isEqualTo("mp4");
    }

    @Test
    void videoUsesVideoLimitNotImageLimit() {
        // 6MB：超过图片上限（5MB）但低于视频上限（10MB）——必须通过，否则"能传视频"就是假的。
        var validated = validator.requireValid(MediaUploadValidator.Scope.ADMIN,
                "video/mp4", mp4(6L * 1024 * 1024));

        assertThat(validated.kind()).isEqualTo(MediaUploadValidator.Kind.VIDEO);
        assertThat(validated.bytes()).isEqualTo((int) (6L * 1024 * 1024));
    }

    @Test
    void memberImageLimitIsSmallerThanAdminAndReportsHowToChangeIt() {
        byte[] image = png(3L * 1024 * 1024);

        assertThat(validator.requireValid(MediaUploadValidator.Scope.ADMIN, "image/png", image).kind())
                .isEqualTo(MediaUploadValidator.Kind.IMAGE);

        assertThatThrownBy(() -> validator.requireValid(MediaUploadValidator.Scope.MEMBER, "image/png", image))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("2097152")
                .hasMessageContaining("系统设置")
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("ASSET_TOO_LARGE");
    }

    @Test
    void heicIsRecognizedButMustBeConvertedFirst() {
        assertThatThrownBy(() -> validator.requireValid(MediaUploadValidator.Scope.MEMBER,
                "image/heic", heic(1024)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("HEIC")
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("ASSET_HEIC_NOT_CONVERTED");
    }

    @Test
    void rejectsContentThatDoesNotMatchDeclaredMajorType() {
        // 把 mp4 改名成 .png 上传：声明 image/png，实际 video/mp4。
        assertThatThrownBy(() -> validator.requireValid(MediaUploadValidator.Scope.ADMIN,
                "image/png", mp4(1024)))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("ASSET_TYPE_MISMATCH");
    }

    @Test
    void rejectsBareTextPretendingToBeImage() {
        assertThatThrownBy(() -> validator.requireValid(MediaUploadValidator.Scope.ADMIN,
                "image/png", "not an image at all".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("ASSET_TYPE_MISMATCH");
    }

    @Test
    void rejectsEmptyContent() {
        assertThatThrownBy(() -> validator.requireValid(MediaUploadValidator.Scope.ADMIN,
                "image/png", new byte[0]))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("ASSET_EMPTY");
    }

    // ---- 构造最小可嗅探样本：只需要文件头，够 Tika 判型即可 ----

    private static byte[] png(long size) {
        byte[] bytes = new byte[(int) size];
        byte[] header = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }

    private static byte[] webp(long size) {
        byte[] bytes = new byte[(int) size];
        byte[] header = {'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }

    private static byte[] mp4(long size) {
        byte[] bytes = new byte[(int) size];
        byte[] header = {0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2'};
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }

    private static byte[] heic(long size) {
        byte[] bytes = new byte[(int) size];
        byte[] header = {0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c'};
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }
}
