package com.fullmall.pos.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.export.ExportAuditRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * POS 销售导出，按角色选择投影（SEC-001/SEC-002）。
 *
 * <p>两种投影**列集合不同**，不是前端隐藏：
 * <ul>
 *   <li>{@code OPS}：销售号、SKU、数量、退款数量、行金额、币种；</li>
 *   <li>{@code FINANCE}：在 OPS 基础上多出成本、毛利、毛利率（bps）。</li>
 * </ul>
 *
 * <p>成本缺失（批次未录成本）时，毛利列输出 `COST_MISSING` 而不是 0——
 * 把"没有数据"当成"零成本"会让毛利率虚高。
 */
@Service
public class PosSalesExportService {

    public static final int MAX_ROWS = 5000;
    public static final String PERMISSION_VIEW_COST = "PERM_FINANCE_COST_VIEW";

    private static final List<String> OPS_COLUMNS = List.of(
            "saleNo", "skuCode", "quantity", "refundedQuantity", "lineTotalAmount", "currency");
    private static final List<String> FINANCE_COLUMNS = List.of(
            "saleNo", "skuCode", "quantity", "refundedQuantity", "lineTotalAmount", "currency",
            "costAmount", "grossProfitAmount", "grossMarginBps");

    private final PosSalesExportRepository repository;
    private final ExportAuditRepository auditRepository;

    public PosSalesExportService(PosSalesExportRepository repository,
                                 ExportAuditRepository auditRepository) {
        this.repository = repository;
        this.auditRepository = auditRepository;
    }

    public record Csv(String content, int rowCount, String projection) {
    }

    public Csv export(int limit, boolean canViewCost, String actor, String requestId) {
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "导出必须记录操作人");
        }
        if (limit <= 0 || limit > MAX_ROWS) {
            throw ApiException.badRequest("INVALID_EXPORT_LIMIT",
                    "导出条数必须在 1.." + MAX_ROWS + " 之间，实际=" + limit);
        }

        List<PosSalesExportRepository.SaleLineRow> rows = repository.findRecent(limit);
        String projection = canViewCost ? "FINANCE" : "OPS";
        String csv = canViewCost ? toFinanceCsv(rows) : toOpsCsv(rows);

        auditRepository.record(new ExportAuditRepository.ExportAudit(
                UUID.randomUUID(), "pos_sale_line", "ALL", actor, rows.size(),
                String.join(",", canViewCost ? FINANCE_COLUMNS : OPS_COLUMNS), requestId));

        return new Csv(csv, rows.size(), projection);
    }

    private String toOpsCsv(List<PosSalesExportRepository.SaleLineRow> rows) {
        return write(OPS_COLUMNS, rows, row -> List.of(
                row.saleNo(), row.skuCode(), row.quantity(), row.refundedQuantity(),
                row.lineTotalAmount(), row.currency()));
    }

    private String toFinanceCsv(List<PosSalesExportRepository.SaleLineRow> rows) {
        return write(FINANCE_COLUMNS, rows, row -> {
            int netQuantity = row.quantity() - row.refundedQuantity();
            long netAmount = row.quantity() == 0 ? 0
                    : row.lineTotalAmount() * netQuantity / row.quantity();
            List<Object> cells = new ArrayList<>(List.of(
                    row.saleNo(), row.skuCode(), row.quantity(), row.refundedQuantity(),
                    row.lineTotalAmount(), row.currency()));
            if (row.costedQuantity() < row.quantity()) {
                cells.add(row.costAmount());
                cells.add("COST_MISSING");
                cells.add("COST_MISSING");
            } else {
                long profit = netAmount - row.costAmount();
                cells.add(row.costAmount());
                cells.add(profit);
                cells.add(netAmount == 0 ? 0 : profit * 10_000 / netAmount);
            }
            return cells;
        });
    }

    private String write(List<String> columns, List<PosSalesExportRepository.SaleLineRow> rows,
                         java.util.function.Function<PosSalesExportRepository.SaleLineRow, List<Object>> mapper) {
        StringWriter writer = new StringWriter();
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(columns.toArray(String[]::new)).get();
        try (CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (PosSalesExportRepository.SaleLineRow row : rows) {
                printer.printRecord(mapper.apply(row));
            }
        } catch (IOException error) {
            throw new IllegalStateException("生成 POS 销售 CSV 失败", error);
        }
        return writer.toString();
    }
}