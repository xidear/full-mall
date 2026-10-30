package com.fullmall.catalog.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 商品 CSV 导入（title,skuCode,price，价格单位元）。
 * 整批校验：任一行不合法则整批不落库；错误带行号。
 */
@Service
public class ProductCsvImporter {

    private static final String HEADER = "title,skuCode,price";

    private final JdbcTemplate jdbc;

    public ProductCsvImporter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Result(int products, int skus) {
    }

    @Transactional
    public Result importCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            throw ApiException.badRequest("CSV_EMPTY", "CSV 内容为空");
        }
        String[] lines = csv.split("\\r?\\n");
        int start = 0;
        if (!lines[0].isBlank() && lines[0].trim().equalsIgnoreCase(HEADER)) {
            start = 1;
        }
        List<String[]> rows = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (int i = start; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            int lineNo = i + 1;
            String[] cells = line.split(",", -1);
            if (cells.length < 3) {
                throw ApiException.badRequest("CSV_ROW_INVALID",
                        "第 " + lineNo + " 行应为 3 列，实际 " + cells.length + " 列");
            }
            String title = cells[0].trim();
            String skuCode = cells[1].trim();
            if (title.isEmpty() || skuCode.isEmpty()) {
                throw ApiException.badRequest("CSV_ROW_INVALID", "第 " + lineNo + " 行标题或 SKU 编码为空");
            }
            if (seen.contains(skuCode)) {
                throw ApiException.badRequest("CSV_DUPLICATED_SKU", "第 " + lineNo + " 行 SKU 编码重复：" + skuCode);
            }
            seen.add(skuCode);
            // 每行都会新建商品，SKU 编码的唯一性是"商品内唯一"，这里不再查全库是否已有同名编码
            // （那样会把"别的商品用过同款编码"误判成冲突）；同文件内重复由上面的 seen 拦住。
            BigDecimal price;
            try {
                price = new BigDecimal(cells[2].trim());
            } catch (NumberFormatException error) {
                throw ApiException.badRequest("CSV_PRICE_INVALID", "第 " + lineNo + " 行价格不是数字：" + cells[2]);
            }
            if (price.signum() <= 0) {
                throw ApiException.badRequest("CSV_PRICE_INVALID", "第 " + lineNo + " 行价格必须大于 0");
            }
            rows.add(new String[]{title, skuCode, String.valueOf(price.multiply(BigDecimal.valueOf(100)).longValueExact())});
        }
        if (rows.isEmpty()) {
            throw ApiException.badRequest("CSV_NO_ROWS", "没有可导入的数据行");
        }
        for (String[] row : rows) {
            UUID productId = UUID.randomUUID();
            jdbc.update("INSERT INTO product (id, title, status) VALUES (?, ?, 'ACTIVE')", productId, row[0]);
            jdbc.update("""
                    INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                    VALUES (?, ?, ?, ?, 'CNY')
                    """, UUID.randomUUID(), productId, row[1], Long.parseLong(row[2]));
        }
        return new Result(rows.size(), rows.size());
    }
}
