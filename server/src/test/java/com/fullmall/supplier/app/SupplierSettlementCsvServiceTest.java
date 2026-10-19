package com.fullmall.supplier.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.export.ExportAuditRepository;
import com.fullmall.supplier.infra.SupplierSettlementRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierSettlementCsvServiceTest {

    private static final UUID SUPPLIER = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final SupplierSettlementRepository repository = mock(SupplierSettlementRepository.class);
    private final ExportAuditRepository auditRepository = mock(ExportAuditRepository.class);
    private final SupplierSettlementCsvService service =
            new SupplierSettlementCsvService(repository, auditRepository);

    private static SupplierSettlementRepository.SettlementRow row() {
        return new SupplierSettlementRepository.SettlementRow(
                UUID.randomUUID(), SUPPLIER,
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-31T00:00:00Z"),
                3, 10_000, 0, 500, 9_500, "CNY", "DRAFT",
                Instant.parse("2026-10-31T01:00:00Z"));
    }

    @Test
    void exportsFixedColumnsWithMinorUnitAmounts() {
        when(repository.findBySupplier(SUPPLIER, 200)).thenReturn(List.of(row()));

        var csv = service.export(SUPPLIER, 200, "supplier:" + SUPPLIER, "req-1");

        assertThat(csv.rowCount()).isEqualTo(1);
        assertThat(csv.content())
                .contains("settlementId,periodStart,periodEnd,orderCount,grossAmount")
                .contains("10000")
                .contains("9500")
                .doesNotContain("refundAmount,0.00");
    }

    @Test
    void writesExportAuditWithScopeAndRowCount() {
        when(repository.findBySupplier(SUPPLIER, 10)).thenReturn(List.of(row()));

        service.export(SUPPLIER, 10, "supplier:" + SUPPLIER, "req-2");

        ArgumentCaptor<ExportAuditRepository.ExportAudit> captor =
                ArgumentCaptor.forClass(ExportAuditRepository.ExportAudit.class);
        verify(auditRepository).record(captor.capture());
        assertThat(captor.getValue().resource()).isEqualTo("supplier_settlement");
        assertThat(captor.getValue().scopeKind()).isEqualTo("OWNER");
        assertThat(captor.getValue().rowCount()).isEqualTo(1);
    }

    @Test
    void rejectsBadLimitWithoutTouchingDatabase() {
        assertThatThrownBy(() -> service.export(SUPPLIER, 0, "supplier:x", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("导出条数");
        assertThatThrownBy(() -> service.export(SUPPLIER, SupplierSettlementCsvService.MAX_ROWS + 1,
                "supplier:x", null))
                .isInstanceOf(ApiException.class);

        verify(repository, never()).findBySupplier(any(), anyInt());
        verify(auditRepository, never()).record(any());
    }

    @Test
    void rejectsMissingActorAndSupplier() {
        assertThatThrownBy(() -> service.export(SUPPLIER, 10, "  ", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("操作人");
        assertThatThrownBy(() -> service.export(null, 10, "supplier:x", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("供应商身份");
    }
}