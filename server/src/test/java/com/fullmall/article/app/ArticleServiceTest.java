package com.fullmall.article.app;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleServiceTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ArticleService service = new ArticleService(jdbc);

    @Test
    void alwaysJoinsEnglishDefaultSoUntranslatedArticlesFallBack() {
        when(jdbc.query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<ArticleService.ArticleSummary>>any(),
                any(), any(), any(), anyInt())).thenReturn(List.of());

        service.listPublished("HELP", "es-ES", "zh-CN", 10);

        verify(jdbc).query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<ArticleService.ArticleSummary>>any(),
                eq("es-ES"), eq("en"), eq("HELP"), eq(10));
    }

    @Test
    void blankLocaleUsesSiteDefaultForLookup() {
        when(jdbc.query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<ArticleService.ArticleSummary>>any(),
                any(), any(), any(), anyInt())).thenReturn(List.of());

        service.listPublished("HELP", null, "zh-CN", 10);

        verify(jdbc).query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<ArticleService.ArticleSummary>>any(),
                eq("zh-CN"), eq("en"), eq("HELP"), eq(10));
    }

    @Test
    void rejectsBadLimitAndBlankSlug() {
        assertThatThrownBy(() -> service.listPublished("HELP", "zh-CN", "zh-CN", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findBySlug("  ", "zh-CN", "zh-CN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingArticleIs404() {
        when(jdbc.query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<ArticleService.ArticleView>>any(),
                any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.findBySlug("nope", "zh-CN", "zh-CN"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("未发布");
    }
}
