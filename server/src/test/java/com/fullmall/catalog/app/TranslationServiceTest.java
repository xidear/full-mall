package com.fullmall.catalog.app;

import com.fullmall.platform.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TranslationServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final TranslationProvider provider = mock(TranslationProvider.class);
    private final TranslationService service = new TranslationService(jdbcTemplate, provider);

    private void glossaryEmpty() {
        when(jdbcTemplate.query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String[]>>any(),
                any())).thenReturn(List.of());
    }

    private void memoryEmpty() {
        when(jdbcTemplate.query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(),
                any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void memoryHitAvoidsProviderCallAndBilling() {
        when(jdbcTemplate.query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(),
                any(), any(), any())).thenReturn(List.of("已缓存译文"));

        var result = service.translate("有机鲜牛奶", "es", 1);

        assertThat(result.text()).isEqualTo("已缓存译文");
        assertThat(result.source()).isEqualTo("memory");
        verify(provider, never()).translate(anyString(), anyString());
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void memoryMissCallsProviderAndStoresResult() {
        memoryEmpty();
        glossaryEmpty();
        when(provider.translate(anyString(), anyString())).thenReturn("leche orgánica");
        when(provider.name()).thenReturn("mock");
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        var result = service.translate("有机鲜牛奶", "es", 1);

        assertThat(result.text()).isEqualTo("leche orgánica");
        assertThat(result.source()).isEqualTo("mock");
        verify(jdbcTemplate).update(anyString(), any(), eq("es"), eq(1), eq("leche orgánica"), eq("mock"));
    }

    @Test
    void glossaryReplacesBrandTermsBeforeTranslation() {
        memoryEmpty();
        when(jdbcTemplate.query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String[]>>any(),
                any())).thenReturn(List.<String[]>of(new String[]{"Full Mall", "Full Mall ES"}));
        when(provider.translate(anyString(), anyString())).thenReturn("x");
        when(provider.name()).thenReturn("mock");
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        service.translate("Full Mall 鲜奶", "es", 1);

        verify(provider).translate(eq("Full Mall ES 鲜奶"), eq("es"));
    }

    @Test
    void rejectsBlankInputs() {
        assertThatThrownBy(() -> service.translate("  ", "es", 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("源文");
        assertThatThrownBy(() -> service.translate("x", " ", 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("目标语言");
    }

    @Test
    void onlyReviewedTranslationsMayBePublished() {
        assertThatThrownBy(() -> TranslationStatus.DRAFT.requirePublishable(false))
                .isInstanceOf(ApiException.class).hasMessageContaining("已审校");
        assertThatThrownBy(() -> TranslationStatus.MACHINE.requirePublishable(true))
                .isInstanceOf(ApiException.class);

        // 已审校可发布（不抛异常）
        TranslationStatus.REVIEWED.requirePublishable(true);
    }

    @Test
    void statusMachineRejectsIllegalTransitions() {
        assertThat(TranslationStatus.MACHINE.canTransitionTo(TranslationStatus.PUBLISHED)).isFalse();
        assertThat(TranslationStatus.PUBLISHED.canTransitionTo(TranslationStatus.REVIEWED)).isFalse();
        assertThat(TranslationStatus.REVIEWED.canTransitionTo(TranslationStatus.PUBLISHED)).isTrue();
        assertThat(TranslationStatus.PUBLISHED.canTransitionTo(TranslationStatus.OUTDATED)).isTrue();
        assertThat(TranslationStatus.OUTDATED.canTransitionTo(TranslationStatus.REVIEWED)).isTrue();
    }
}