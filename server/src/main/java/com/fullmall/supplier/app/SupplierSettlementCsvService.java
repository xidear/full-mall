package com.fullmall.supplier.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.export.ExportAuditRepository;
import com.fullmall.supplier.infra.SupplierSettlementRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

/**
 * 结算单 CSV 导出（SUP-021 的导出半边）。
 *
 * <p>约束：
 * <ul>
 *   <li>范围：只导出请求方自己的结算单（supplierId 由认证上下文得出）；</li>
 *   <li>列固定声明，不导出内部字段；</li>
 *   <li>每次导出写 `export_task` 审计（谁、导了多少行、哪些列）。</li>
 * </ul>
 *
 * <p>金额以最小货币单位整数导出，附带币种列；不做格式化成"元"的处理，避免精度歧义。
 */
@Service
public class SupplierSettlementCsvService {

    public static final int MAX_ROWS = 2000;

    private static final List<String> COLUMNS = List.of(
            "settlementId", "periodStart", "periodEnd", "orderCount", "grossAmount",
            "refundAmount", "commissionAmount", "payableAmount", "currency", "status");

    private final SupplierSettlementRepository repository;
    private final ExportAuditRepository auditRepository;

    public SupplierSettlementCsvService(SupplierSettlementRepository repository,
                                        ExportAuditRepository auditRepository) {
        this.repository = repository;
        this.auditRepository = auditRepository;
    }

    public record Csv(String content, int rowCount) {
    }

    public Csv export(UUID supplierId, int limit, String actor, String requestId) {
        if (supplierId == null) {
            throw ApiException.badRequest("SUPPLIER_REQUIRED", "缺少供应商身份");
        }
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "导出必须记录操作人");
        }
        if (limit <= 0 || limit > MAX_ROWS) {
            throw ApiException.badRequest("INVALID_EXPORT_LIMIT",
                    "导出条数必须在 1.." + MAX_ROWS + " 之间，实际=" + limit);
        }

        List<SupplierSettlementRepository.SettlementRow> rows = repository.findBySupplier(supplierId, limit);
        String csv = toCsv(rows);

        auditRepository.record(new ExportAuditRepository.ExportAudit(
                UUID.randomUUID(), "supplier_settlement", "OWNER", actor,
                rows.size(), String.join(",", COLUMNS), requestId));
        return new Csv(csv, rows.size());
    }

    private String toCsv(List<SupplierSettlementRepository.SettlementRow> rows) {
        StringWriter writer = new StringWriter();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(COLUMNS.toArray(String[]::new))
                .get();
        try (CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (SupplierSettlementRepository.SettlementRow row : rows) {
                printer.printRecord(
                        row.id(),
                        row.periodStart(),
                        row.periodEnd(),
                        row.orderCount(),
                        row.grossAmount(),
                        row.refundAmount(),
                        row.commissionAmount(),
                        row.payableAmount(),
                        row.currency(),
                        row.status());
            }
        } catch (IOException error) {
            throw new IllegalStateException("生成结算 CSV 失败", error);
        }
        return writer.toString();
    }
}