package com.fullmall.pos.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.export.ExportAuditRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PosSalesExportServiceTest {

    private final PosSalesExportRepository repository = mock(PosSalesExportRepository.class);
    private final ExportAuditRepository auditRepository = mock(ExportAuditRepository.class);
    private final PosSalesExportService service = new PosSalesExportService(repository, auditRepository);

    private static PosSalesExportRepository.SaleLineRow row(int quantity, int refunded,
                                                            long lineTotal, long cost, long costedQty) {
        return new PosSalesExportRepository.SaleLineRow(
                UUID.randomUUID(), "S001", "MILK-1L", quantity, refunded, lineTotal,
                cost, costedQty, "CNY");
    }

    @Test
    void opsProjectionNeverContainsCostOrMarginColumns() {
        when(repository.findRecent(anyInt())).thenReturn(List.of(row(2, 0, 3980, 2400, 2)));

        var csv = service.export(100, false, "operator1", "req-1");

        assertThat(csv.projection()).isEqualTo("OPS");
        assertThat(csv.content()).doesNotContain("costAmount")
                .doesNotContain("grossProfit")
                .doesNotContain("2400");
    }

    @Test
    void financeProjectionAddsCostAndMargin() {
        when(repository.findRecent(anyInt())).thenReturn(List.of(row(2, 0, 3980, 2400, 2)));

        var csv = service.export(100, true, "cfo", "req-2");

        assertThat(csv.projection()).isEqualTo("FINANCE");
        assertThat(csv.content()).contains("costAmount").contains("grossMarginBps");
        // 毛利 3980-2400=1580，毛利率 1580*10000/3980 = 3969 bps
        assertThat(csv.content()).contains("2400").contains("1580").contains("3969");
    }

    @Test
    void missingCostIsLabelledInsteadOfTreatedAsZero() {
        when(repository.findRecent(anyInt())).thenReturn(List.of(row(2, 0, 3980, 0, 0)));

        var csv = service.export(100, true, "cfo", "req-3");

        assertThat(csv.content()).contains("COST_MISSING");
        // 成本缺失时毛利与毛利率两列都应是 COST_MISSING，而不是算出 0 或虚高毛利
        assertThat(csv.content()).contains(",0,COST_MISSING,COST_MISSING");
    }

    @Test
    void refundedQuantityReducesNetAmountInMargin() {
        // 2 件共 3980，退 1 件 → 净额 1990；成本 2400 → 毛利 -410
        when(repository.findRecent(anyInt())).thenReturn(List.of(row(2, 1, 3980, 2400, 2)));

        var csv = service.export(100, true, "cfo", "req-4");

        assertThat(csv.content()).contains("-410");
    }

    @Test
    void auditsProjectionAndRowCount() {
        when(repository.findRecent(anyInt())).thenReturn(List.of(row(1, 0, 1000, 600, 1)));

        service.export(10, true, "cfo", "req-5");

        ArgumentCaptor<ExportAuditRepository.ExportAudit> captor =
                ArgumentCaptor.forClass(ExportAuditRepository.ExportAudit.class);
        verify(auditRepository).record(captor.capture());
        assertThat(captor.getValue().columns()).contains("costAmount");
        assertThat(captor.getValue().rowCount()).isEqualTo(1);
    }

    @Test
    void rejectsBadLimitWithoutQuerying() {
        assertThatThrownBy(() -> service.export(0, true, "cfo", null))
                .isInstanceOf(ApiException.class);
        verify(repository, never()).findRecent(anyInt());
        verify(auditRepository, never()).record(any());
    }
}