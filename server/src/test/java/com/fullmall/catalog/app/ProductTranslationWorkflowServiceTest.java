package com.fullmall.catalog.app;

import com.fullmall.catalog.domain.InventoryTrackingLevel;
import com.fullmall.catalog.domain.Product;
import com.fullmall.catalog.domain.ProductStatus;
import com.fullmall.catalog.infra.ProductTranslationRepository;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import com.fullmall.setting.app.SystemSettingService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 译文状态机：DRAFT/MACHINE → REVIEWED → PUBLISHED，OUTDATED 不得当作已发布内容读出去。
 *
 * <p>守两条易回归的点：非法跃迁时不得写库（不留半成品）；只有真正发生变更才写审计，
 * 免得假痕淹没真变更、出事时查不到真正改过什么。
 */
class ProductTranslationWorkflowServiceTest {

    private static final UUID PRODUCT = UUID.fromString("aaaaaaaa-0000-0000-0000-0000000000aa");
    private static final String LOCALE = "es";

    private final ProductTranslationRepository repository = mock(ProductTranslationRepository.class);
    private final AuditEventService auditEvents = mock(AuditEventService.class);
    private final SystemSettingService settings = mock(SystemSettingService.class);
    private final CatalogService catalog = mock(CatalogService.class);
    private final TranslationService translator = mock(TranslationService.class);
    private final RichTextTranslationService richText = mock(RichTextTranslationService.class);
    private final org.springframework.jdbc.core.JdbcTemplate jdbc =
            mock(org.springframework.jdbc.core.JdbcTemplate.class);
    private final ProductTranslationWorkflowService service = new ProductTranslationWorkflowService(
            repository, auditEvents, settings, catalog, translator, richText, jdbc);

    private static ProductTranslationRepository.Row row(TranslationStatus status) {
        return new ProductTranslationRepository.Row(PRODUCT, LOCALE, "Leche", "desc", null, null,
                status, "hash-1", null, null);
    }

    private static Product product(String description) {
        return new Product(PRODUCT, "有机鲜牛奶", description, null, ProductStatus.ACTIVE,
                InventoryTrackingLevel.VARIANT, false, java.util.List.of());
    }

    @Test
    void machineTranslationCanBeReviewed() {
        when(repository.find(PRODUCT, LOCALE)).thenReturn(Optional.of(row(TranslationStatus.MACHINE)));
        when(repository.find(PRODUCT, LOCALE))
                .thenReturn(Optional.of(row(TranslationStatus.MACHINE)))
                .thenReturn(Optional.of(row(TranslationStatus.REVIEWED)));

        var result = service.review(PRODUCT, LOCALE, "reviewer1");

        assertThat(result.status()).isEqualTo(TranslationStatus.REVIEWED);
        verify(repository).updateStatus(eq(PRODUCT), eq(LOCALE), eq(TranslationStatus.REVIEWED),
                eq("reviewer1"), any(Instant.class));
        verify(auditEvents).record(any());
    }

    // 机翻未审就发布要被拒绝，且拒绝时不得落任何状态更新（never 断言守的就是"失败不留半成品"）。
    @Test
    void draftCannotBePublishedDirectly() {
        when(repository.find(PRODUCT, LOCALE)).thenReturn(Optional.of(row(TranslationStatus.MACHINE)));

        assertThatThrownBy(() -> service.publish(PRODUCT, LOCALE, true, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("已审校");
        verify(repository, never()).updateStatus(any(), any(), any(), any(), any());
    }

    @Test
    void reviewedTranslationCanBePublished() {
        when(repository.find(PRODUCT, LOCALE))
                .thenReturn(Optional.of(row(TranslationStatus.REVIEWED)))
                .thenReturn(Optional.of(row(TranslationStatus.PUBLISHED)));

        var result = service.publish(PRODUCT, LOCALE, true, "admin");

        assertThat(result.status()).isEqualTo(TranslationStatus.PUBLISHED);
        verify(auditEvents).record(any());
    }

    // 原文改动后译文变 OUTDATED：前台读到的是"空"，不是旧译文；调用方据此回退源语言。
    @Test
    void publishedReadFiltersOutOutdatedTranslations() {
        when(repository.find(PRODUCT, LOCALE)).thenReturn(Optional.of(row(TranslationStatus.OUTDATED)));

        assertThat(service.published(PRODUCT, LOCALE)).isEmpty();
    }

    @Test
    void sourceChangeMarksOutdatedAnd_auditsIt() {
        when(repository.markOutdatedIfSourceChanged(PRODUCT, LOCALE, "hash-2")).thenReturn(true);

        assertThat(service.markOutdatedOnSourceChange(PRODUCT, LOCALE, "hash-2", "editor")).isTrue();
        verify(auditEvents).record(any());
    }

    // hash 未变就不写审计：否则每次保存商品都刷一条假变更，真实变更无从辨认。
    @Test
    void sourceChangeWithoutEffectDoesNotAudit() {
        when(repository.markOutdatedIfSourceChanged(PRODUCT, LOCALE, "hash-1")).thenReturn(false);

        assertThat(service.markOutdatedOnSourceChange(PRODUCT, LOCALE, "hash-1", "editor")).isFalse();
        verify(auditEvents, never()).record(any());
    }

    @Test
    void missingTranslationIs404() {
        when(repository.find(PRODUCT, LOCALE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.review(PRODUCT, LOCALE, "reviewer1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("译文不存在");
    }

    @Test
    void autoTranslateWritesDraftWithMachineTextForEnabledLocale() {
        when(settings.defaultLocale()).thenReturn("zh-CN");
        when(settings.translationLocales()).thenReturn(java.util.List.of("es", "en", "ja"));
        when(catalog.getProduct(PRODUCT, "zh-CN")).thenReturn(product("新鲜每日配送"));
        when(translator.currentGlossaryVersion()).thenReturn(1);
        when(translator.sourceHash("有机鲜牛奶", "新鲜每日配送")).thenReturn("hash-src");
        when(translator.translate("有机鲜牛奶", LOCALE, 1))
                .thenReturn(new TranslationService.Translation("Leche", "mock", 1));
        when(translator.translate("新鲜每日配送", LOCALE, 1))
                .thenReturn(new TranslationService.Translation("desc", "mock", 1));
        when(repository.find(PRODUCT, LOCALE)).thenReturn(Optional.of(row(TranslationStatus.DRAFT)));

        service.autoTranslate(PRODUCT, LOCALE, "editor");

        verify(repository).upsert(PRODUCT, LOCALE, "Leche", "desc", null, null, "hash-src");
        verify(auditEvents).record(any());
    }

    @Test
    void autoTranslateRejectsDisabledLocale() {
        when(settings.defaultLocale()).thenReturn("zh-CN");
        when(settings.translationLocales()).thenReturn(java.util.List.of("en"));

        assertThatThrownBy(() -> service.autoTranslate(PRODUCT, "fr", "editor"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("未在「系统设置 → 已启用语言」中启用");
    }

    // 操作人是审计主体，空白必须在入口拒绝，不能落一条没有责任人的记录。
    @Test
    void actorIsMandatory() {
        assertThatThrownBy(() -> service.review(PRODUCT, LOCALE, "  "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("操作人");
    }
}