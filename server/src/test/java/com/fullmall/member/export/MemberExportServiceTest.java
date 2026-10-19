package com.fullmall.member.export;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.export.ExportAuditRepository;
import com.fullmall.platform.scope.Scope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberExportServiceTest {

    private final MemberExportRepository repository = mock(MemberExportRepository.class);
    private final ExportAuditRepository auditRepository = mock(ExportAuditRepository.class);
    private final MemberExportService service = new MemberExportService(repository, auditRepository);

    private static List<MemberExportRepository.MemberRow> rows() {
        return List.of(
                new MemberExportRepository.MemberRow("M0001", "王小明", "13800000001", "GOLD", 500, 10000, "CNY"),
                new MemberExportRepository.MemberRow("M0002", "李静", "13800000002", "STANDARD", 120, 0, "CNY"));
    }

    @Test
    void masksPiiButKeepsBusinessFields() {
        when(repository.findForExport(anyInt())).thenReturn(rows());

        MemberExportService.CsvExport export = service.export(Scope.all(), "admin", 100, "req-1");

        assertThat(export.rowCount()).isEqualTo(2);
        assertThat(export.content())
                .contains("138****0001")
                .contains("138****0002")
                .doesNotContain("13800000001")
                .contains("王*")
                .contains("GOLD")
                .contains("10000");
    }

    @Test
    void writesAuditRowWithActorScopeAndCount() {
        when(repository.findForExport(anyInt())).thenReturn(rows());

        service.export(Scope.all(), "admin", 100, "req-1");

        ArgumentCaptor<ExportAuditRepository.ExportAudit> captor =
                ArgumentCaptor.forClass(ExportAuditRepository.ExportAudit.class);
        verify(auditRepository).record(captor.capture());
        ExportAuditRepository.ExportAudit audit = captor.getValue();
        assertThat(audit.resource()).isEqualTo("member");
        assertThat(audit.scopeKind()).isEqualTo("ALL");
        assertThat(audit.actor()).isEqualTo("admin");
        assertThat(audit.rowCount()).isEqualTo(2);
        assertThat(audit.requestId()).isEqualTo("req-1");
        assertThat(audit.columns()).doesNotContain("idCard").contains("phoneMasked");
    }

    @Test
    void refusesNonPlatformScope() {
        assertThatThrownBy(() -> service.export(Scope.owner("supplier-a"), "supplier-a", 100, "req-2"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("仅限平台运营");
        verify(repository, never()).findForExport(anyInt());
        verify(auditRepository, never()).record(any());
    }

    @Test
    void refusesOutOfRangeLimit() {
        assertThatThrownBy(() -> service.export(Scope.all(), "admin", 0, "req-3"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("导出条数");
        assertThatThrownBy(() -> service.export(Scope.all(), "admin",
                MemberExportService.MAX_LIMIT + 1, "req-4"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("导出条数");
    }

    @Test
    void refusesMissingActor() {
        assertThatThrownBy(() -> service.export(Scope.all(), "  ", 10, "req-5"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("操作人");
    }
}