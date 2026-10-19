package com.fullmall.catalog.app;

import com.fullmall.catalog.infra.ProductTranslationRepository;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
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

class ProductTranslationWorkflowServiceTest {

    private static final UUID PRODUCT = UUID.fromString("aaaaaaaa-0000-0000-0000-0000000000aa");
    private static final String LOCALE = "es";

    private final ProductTranslationRepository repository = mock(ProductTranslationRepository.class);
    private final AuditEventService auditEvents = mock(AuditEventService.class);
    private final ProductTranslationWorkflowService service =
            new ProductTranslationWorkflowService(repository, auditEvents);

    private static ProductTranslationRepository.Row row(TranslationStatus status) {
        return new ProductTranslationRepository.Row(PRODUCT, LOCALE, "Leche", "desc", status,
                "hash-1", null, null);
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
    void actorIsMandatory() {
        assertThatThrownBy(() -> service.review(PRODUCT, LOCALE, "  "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("操作人");
    }
}