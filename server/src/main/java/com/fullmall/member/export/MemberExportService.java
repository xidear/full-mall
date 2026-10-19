package com.fullmall.member.export;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.export.ExportAuditRepository;
import com.fullmall.platform.export.FieldMasker;
import com.fullmall.platform.scope.Scope;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 会员导出（SEC-002 的落地切片）。
 *
 * <p>三层裁剪：
 * <ol>
 *   <li><b>范围</b>：仅平台运营（{@link Scope#isAll()}）可导出会员，供应商/门店范围直接拒绝；</li>
 *   <li><b>字段</b>：输出列在 {@link #COLUMNS} 里固定声明，不含身份证、地址等未声明字段；</li>
 *   <li><b>脱敏</b>：手机号与姓名经 {@link FieldMasker} 掩码。</li>
 * </ol>
 *
 * <p>每次导出都写 {@code export_task} 审计（追加写），不静默失败。
 */
@Service
public class MemberExportService {

    public static final int MAX_LIMIT = 5000;

    private static final String RESOURCE = "member";
    private static final List<String> COLUMNS = List.of(
            "memberNo", "nameMasked", "phoneMasked", "tier", "pointsBalance", "storedValueBalance", "currency");

    private final MemberExportRepository repository;
    private final ExportAuditRepository auditRepository;
    private final com.fullmall.platform.risk.RiskAlertService riskAlerts;

    public MemberExportService(MemberExportRepository repository, ExportAuditRepository auditRepository,
                               com.fullmall.platform.risk.RiskAlertService riskAlerts) {
        this.repository = repository;
        this.auditRepository = auditRepository;
        this.riskAlerts = riskAlerts;
    }

    public record CsvExport(String filename, String content, int rowCount) {
    }

    public CsvExport export(Scope scope, String actor, int limit, String requestId) {
        if (!scope.isAll()) {
            // SEC-004：越权导出尝试属于风险事件，先告警再拒绝
            riskAlerts.raise("EXPORT_SCOPE_DENIED", com.fullmall.platform.risk.RiskAlertService.Severity.HIGH,
                    actor == null || actor.isBlank() ? "unknown-scope-" + scope.kind() : actor,
                    "scope=" + scope.kind() + ";resource=member", java.time.Duration.ofHours(1));
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "EXPORT_SCOPE_DENIED", "会员导出仅限平台运营");
        }
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "导出必须记录操作人");
        }
        if (limit <= 0 || limit > MAX_LIMIT) {
            throw ApiException.badRequest("INVALID_EXPORT_LIMIT",
                    "导出条数必须在 1.." + MAX_LIMIT + " 之间，实际=" + limit);
        }

        List<MemberExportRepository.MemberRow> rows = repository.findForExport(limit);
        String csv = toCsv(rows);
        auditRepository.record(new ExportAuditRepository.ExportAudit(
                UUID.randomUUID(), RESOURCE, scope.kind().name(), actor,
                rows.size(), String.join(",", COLUMNS), requestId));

        return new CsvExport("member-export-" + Instant.now().toEpochMilli() + ".csv", csv, rows.size());
    }

    private String toCsv(List<MemberExportRepository.MemberRow> rows) {
        StringWriter writer = new StringWriter();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(COLUMNS.toArray(String[]::new))
                .get();
        try (CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (MemberExportRepository.MemberRow row : rows) {
                printer.printRecord(
                        row.memberNo(),
                        FieldMasker.name(row.name()),
                        FieldMasker.phone(row.phone()),
                        row.tier(),
                        row.pointsBalance(),
                        row.storedValueBalance(),
                        row.currency());
            }
        } catch (IOException error) {
            throw new IllegalStateException("生成会员导出 CSV 失败", error);
        }
        return writer.toString();
    }
}