package com.fullmall.platform.asset;

import com.fullmall.platform.error.GlobalExceptionHandler;
import com.fullmall.setting.app.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 上传接口的行为约束（不依赖 Spring 上下文，避免被其它模块的迁移/装配拖累）：
 * SVG 继续被拒、视频能通过、kind 按内容落库、顾客端无会话不能写对象存储。
 */
class MediaUploadEndpointTest {

    private static final long ADMIN_IMAGE = 5L * 1024 * 1024;
    private static final long ADMIN_VIDEO = 10L * 1024 * 1024;
    private static final long MEMBER_VIDEO = 5L * 1024 * 1024;

    private final AssetStorage storage = mock(AssetStorage.class);
    private final SystemSettingService settings = mock(SystemSettingService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final MediaUploadValidator validator = new MediaUploadValidator(settings);

    private MockMvc admin;
    private MockMvc member;

    @BeforeEach
    void setUp() {
        when(settings.adminImageMaxBytes()).thenReturn(ADMIN_IMAGE);
        when(settings.adminVideoMaxBytes()).thenReturn(ADMIN_VIDEO);
        when(settings.memberImageMaxBytes()).thenReturn(2L * 1024 * 1024);
        when(settings.memberVideoMaxBytes()).thenReturn(MEMBER_VIDEO);
        when(storage.put(anyString(), any(byte[].class), anyString()))
                .thenAnswer(call -> "http://cdn.example.com/" + call.getArgument(0));
        when(storage.provider()).thenReturn("local");

        admin = MockMvcBuilders.standaloneSetup(
                        new MediaUploadController(storage, validator, settings, jdbc))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        member = MockMvcBuilders.standaloneSetup(
                        new MemberMediaController(storage, validator, settings))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void svgIsRejectedWithActionableCode() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.svg", "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes(StandardCharsets.UTF_8));

        admin.perform(multipart("/admin/v1/media").file(file)
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASSET_TYPE_NOT_ALLOWED"));
    }

    @Test
    void videoUploadStoresVideoKind() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", mp4(2048));

        admin.perform(multipart("/admin/v1/media").file(file).param("purpose", "VIDEO")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("VIDEO"))
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.containsString(".mp4")));

        verify(jdbc).update(anyString(), any(Object[].class));
        verify(storage).put(org.mockito.ArgumentMatchers.argThat(key -> key.startsWith("assets/")),
                any(byte[].class), eq("video/mp4"));
    }

    @Test
    void limitsEndpointReflectsConfiguredValues() throws Exception {
        admin.perform(get("/admin/v1/media/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageMaxBytes").value((int) ADMIN_IMAGE))
                .andExpect(jsonPath("$.videoMaxBytes").value((int) ADMIN_VIDEO))
                .andExpect(jsonPath("$.videoTypes[0]").value("video/mp4"));
    }

    @Test
    void anonymousMemberUploadIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", png(512));

        member.perform(multipart("/api/v1/media").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MEMBER_REQUIRED"));

        org.mockito.Mockito.verifyNoInteractions(storage);
    }

    @Test
    void memberVideoUsesMemberLimitAndSkipsAssetLibrary() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", mp4(1024));

        member.perform(multipart("/api/v1/media").file(file)
                        .principal(new UsernamePasswordAuthenticationToken(
                                "eeeeeeee-9999-9999-9999-999999999999", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("VIDEO"));

        // 顾客素材不入素材库：只写对象存储，不写 asset 表
        verify(storage).put(org.mockito.ArgumentMatchers.argThat(key -> key.startsWith("reviews/")),
                any(byte[].class), eq("video/mp4"));
        org.mockito.Mockito.verifyNoInteractions(jdbc);
    }

    @Test
    void memberUploadRejectsSvg() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "x.svg", "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8));

        member.perform(multipart("/api/v1/media").file(file)
                        .principal(new UsernamePasswordAuthenticationToken("m1", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASSET_TYPE_NOT_ALLOWED"));
    }

    private static byte[] png(int size) {
        byte[] bytes = new byte[size];
        byte[] header = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }

    private static byte[] mp4(int size) {
        byte[] bytes = new byte[size];
        byte[] header = {0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2'};
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }
}
