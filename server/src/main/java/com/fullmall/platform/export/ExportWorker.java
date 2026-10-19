package com.fullmall.platform.export;

import com.fullmall.member.export.MemberExportRepository;
import com.fullmall.platform.export.ExportJobService.ExportJob;
import com.fullmall.pos.app.PosSalesExportRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * 导出执行器：领取一个任务并生成 CSV。
 *
 * <p>设计取舍（当前阶段）：
 * <ul>
 *   <li>**串行写文件**，一次只处理一个任务，避免导出把连接池与磁盘 IO 打满；</li>
 *   <li>按 `projection` 决定列集合：`FINANCE` 才包含成本/毛利，`OPS` 不含——
 *       这是权限落地，不是前端隐藏；</li>
 *   <li>行数由任务参数 `limit` 控制（默认 5000），防止一次性拉爆内存；
 *       真正的大表要改 keyset 分批，已在设计文档记录。</li>
 * </ul>
 */
@Component
public class ExportWorker {

    private static final Logger log = LoggerFactory.getLogger(ExportWorker.class);

    private final ExportJobService jobs;
    private final MemberExportRepository memberRepository;
    private final PosSalesExportRepository posRepository;
    private final Path storageDir;
    private final int maxRows;

    public ExportWorker(ExportJobService jobs,
                        MemberExportRepository memberRepository,
                        PosSalesExportRepository posRepository,
                        @Value("${app.export.storage-dir:}") String storageDir,
                        @Value("${app.export.max-rows:5000}") int maxRows) {
        if (maxRows <= 0) {
            throw new IllegalArgumentException("app.export.max-rows 必须大于 0，实际=" + maxRows);
        }
        this.maxRows = maxRows;
        this.jobs = jobs;
        this.memberRepository = memberRepository;
        this.posRepository = posRepository;
        this.storageDir = Path.of(storageDir == null || storageDir.isBlank()
                ? System.getProperty("java.io.tmpdir") + "/fullmall-exports"
                : storageDir);
    }

    /**
     * 处理一个任务。
     *
     * @return 是否处理了任务（没有待处理任务时返回 false）
     */
    @Transactional
    public boolean runOnce() {
        ExportJob job = jobs.claimNext();
        if (job == null) {
            return false;
        }
        try {
            Files.createDirectories(storageDir);
            Path target = storageDir.resolve(job.id() + ".csv");
            int rows = writeCsv(job, target);
            jobs.succeed(job.id(), rows, target.toString());
            log.info("导出完成 job={} resource={} projection={} rows={}",
                    job.id(), job.resource(), job.projection(), rows);
        } catch (ExportTooLargeException error) {
            jobs.fail(job.id(), error.getMessage());
            log.warn("导出超限失败 job={} resource={}: {}", job.id(), job.resource(), error.getMessage());
        } catch (RuntimeException | IOException error) {
            jobs.fail(job.id(), error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
            log.warn("导出失败 job={} resource={} error={}", job.id(), job.resource(), error.getMessage());
        }
        return true;
    }

    private int writeCsv(ExportJob job, Path target) throws IOException {
        boolean finance = "FINANCE".equalsIgnoreCase(job.projection());
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            return switch (job.resource().toLowerCase(Locale.ROOT)) {
                case "member" -> writeMembers(printer);
                case "pos_sale_line" -> writePosSales(printer, finance);
                default -> throw new IllegalArgumentException("不支持的导出资源: " + job.resource());
            };
        }
    }

    /** 超过上限不许静默截断：宁可失败并明确告知，也不让用户以为拿到了全量。 */
    static final class ExportTooLargeException extends RuntimeException {
        ExportTooLargeException(String message) {
            super(message);
        }
    }

    private void requireWithinLimit(int fetched) {
        if (fetched > maxRows) {
            throw new ExportTooLargeException(
                    "导出结果超过上限 " + maxRows + " 行（多取 1 行用于判定）。请缩小时间范围或按门店/语言分批导出");
        }
    }

    private int writeMembers(CSVPrinter printer) throws IOException {
        printer.printRecord("memberNo", "nameMasked", "phoneMasked", "tier", "points", "storedValue", "currency");
        List<MemberExportRepository.MemberRow> rows = memberRepository.findForExport(maxRows + 1);
        requireWithinLimit(rows.size());
        for (MemberExportRepository.MemberRow row : rows) {
            printer.printRecord(row.memberNo(),
                    com.fullmall.platform.export.FieldMasker.name(row.name()),
                    com.fullmall.platform.export.FieldMasker.phone(row.phone()),
                    row.tier(), row.pointsBalance(), row.storedValueBalance(), row.currency());
        }
        return rows.size();
    }

    private int writePosSales(CSVPrinter printer, boolean finance) throws IOException {
        if (finance) {
            printer.printRecord("saleNo", "skuCode", "quantity", "refundedQuantity",
                    "lineTotalAmount", "currency", "costAmount", "grossProfitAmount", "grossMarginBps");
        } else {
            printer.printRecord("saleNo", "skuCode", "quantity", "refundedQuantity",
                    "lineTotalAmount", "currency");
        }
        List<PosSalesExportRepository.SaleLineRow> rows = posRepository.findRecent(maxRows + 1);
        requireWithinLimit(rows.size());
        for (PosSalesExportRepository.SaleLineRow row : rows) {
            if (!finance) {
                printer.printRecord(row.saleNo(), row.skuCode(), row.quantity(),
                        row.refundedQuantity(), row.lineTotalAmount(), row.currency());
                continue;
            }
            int netQty = row.quantity() - row.refundedQuantity();
            long netAmount = row.quantity() == 0 ? 0 : row.lineTotalAmount() * netQty / row.quantity();
            if (row.costedQuantity() < row.quantity()) {
                printer.printRecord(row.saleNo(), row.skuCode(), row.quantity(), row.refundedQuantity(),
                        row.lineTotalAmount(), row.currency(), row.costAmount(), "COST_MISSING", "COST_MISSING");
            } else {
                long profit = netAmount - row.costAmount();
                printer.printRecord(row.saleNo(), row.skuCode(), row.quantity(), row.refundedQuantity(),
                        row.lineTotalAmount(), row.currency(), row.costAmount(), profit,
                        netAmount == 0 ? 0 : profit * 10_000 / netAmount);
            }
        }
        return rows.size();
    }
}