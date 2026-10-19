package com.fullmall.supplier.app;

import com.fullmall.platform.importing.CsvImportException;
import com.fullmall.platform.importing.CsvRowError;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 供应商库存 feed CSV 解析与校验（SUP-021 导入半边）。
 *
 * <p>规则：
 * <ul>
 *   <li>表头必须完全匹配（顺序固定），否则整批拒绝；</li>
 *   <li>逐行校验：UUID 格式、可用数量非负、价格非负或留空、币种 3 位；</li>
 *   <li>业务键 `skuId` 重复即错误；</li>
 *   <li>**收集全部错误**后一次性抛出，不允许“部分成功”。</li>
 * </ul>
 */
@Component
public class SupplierStockFeedCsvParser {

    public static final List<String> HEADER =
            List.of("skuId", "supplierSkuCode", "availableQuantity", "priceAmount", "priceCurrency");

    public record StockFeedRow(int rowNumber, UUID skuId, String supplierSkuCode,
                               int availableQuantity, Long priceAmount, String priceCurrency) {
    }

    public List<StockFeedRow> parse(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new CsvImportException("CSV 内容为空",
                    List.of(new CsvRowError(1, "file", "文件内容为空")));
        }

        List<CsvRowError> errors = new ArrayList<>();
        List<StockFeedRow> rows = new ArrayList<>();
        Set<UUID> seenSku = new HashSet<>();

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .get()
                .parse(new StringReader(csv))) {

            List<String> actualHeader = parser.getHeaderNames();
            if (!actualHeader.equals(HEADER)) {
                throw new CsvImportException("CSV 表头不匹配",
                        List.of(new CsvRowError(1, "header",
                                "期望 " + HEADER + "，实际 " + actualHeader)));
            }

            for (CSVRecord record : parser) {
                int line = (int) record.getRecordNumber() + 1;
                if (record.size() != HEADER.size()) {
                    errors.add(new CsvRowError(line, "row",
                            "列数应为 " + HEADER.size() + "，实际 " + record.size()));
                    continue;
                }

                UUID skuId = null;
                String rawSku = record.get("skuId");
                try {
                    skuId = UUID.fromString(rawSku);
                } catch (IllegalArgumentException error) {
                    errors.add(new CsvRowError(line, "skuId", "不是合法 UUID: " + rawSku));
                }

                String supplierSkuCode = record.get("supplierSkuCode");
                if (supplierSkuCode.isBlank()) {
                    errors.add(new CsvRowError(line, "supplierSkuCode", "不能为空"));
                } else if (supplierSkuCode.length() > 64) {
                    errors.add(new CsvRowError(line, "supplierSkuCode", "长度不能超过 64"));
                }

                Integer quantity = parseInt(errors, line, "availableQuantity", record.get("availableQuantity"));
                if (quantity != null && quantity < 0) {
                    errors.add(new CsvRowError(line, "availableQuantity", "不能为负: " + quantity));
                }

                Long price = null;
                String rawPrice = record.get("priceAmount");
                if (!rawPrice.isBlank()) {
                    try {
                        price = Long.parseLong(rawPrice);
                        if (price < 0) {
                            errors.add(new CsvRowError(line, "priceAmount", "不能为负: " + price));
                        }
                    } catch (NumberFormatException error) {
                        errors.add(new CsvRowError(line, "priceAmount", "不是整数（最小货币单位）: " + rawPrice));
                    }
                }

                String currency = record.get("priceCurrency").toUpperCase(Locale.ROOT);
                if (price != null && (currency.length() != 3 || !currency.chars().allMatch(Character::isLetter))) {
                    errors.add(new CsvRowError(line, "priceCurrency", "必须是 3 位 ISO 币种: " + currency));
                }

                if (skuId != null && !seenSku.add(skuId)) {
                    errors.add(new CsvRowError(line, "skuId", "同一文件内重复: " + skuId));
                }

                if (skuId != null && quantity != null && quantity >= 0
                        && supplierSkuCode != null && !supplierSkuCode.isBlank()) {
                    rows.add(new StockFeedRow(line, skuId, supplierSkuCode, quantity, price,
                            price == null ? null : currency));
                }
            }
        } catch (IOException error) {
            throw new IllegalStateException("解析 CSV 失败", error);
        }

        if (!errors.isEmpty()) {
            throw new CsvImportException("CSV 校验失败，共 " + errors.size() + " 处问题", errors);
        }
        if (rows.isEmpty()) {
            throw new CsvImportException("CSV 没有数据行",
                    List.of(new CsvRowError(2, "file", "除表头外没有有效数据行")));
        }
        return rows;
    }

    private Integer parseInt(List<CsvRowError> errors, int line, String field, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException error) {
            errors.add(new CsvRowError(line, field, "不是整数: " + raw));
            return null;
        }
    }
}