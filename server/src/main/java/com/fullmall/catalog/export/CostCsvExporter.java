package com.fullmall.catalog.export;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SKU 成本导出（ERP/财务口径）。
 *
 * <p>口径说明：
 * <ul>
 *   <li>`avgCost` 是移动加权平均成本；从未入库时**留空**（不是 0）；</li>
 *   <li>`stale=true` 表示已卖完，均价为最后一次参考值；</li>
 *   <li>金额单位为分，CSV 同时输出"元"列便于财务直接看。</li>
 * </ul>
 *
 * <p>只用导出快照，不做实时计算——成本已经由收货流程维护在 `sku_cost` 里。
 */
@Component
public class CostCsvExporter {

    private final JdbcTemplate jdbc;

    public CostCsvExporter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public static final String HEADER =
            "skuCode,productTitle,onHandQty,onHandAmount,avgCostFen,avgCostYuan,stale";

    public List<String> rows(int limit) {
        return jdbc.query("""
                SELECT s.sku_code, p.title, c.quantity, c.amount, c.avg_cost_amount, c.stale
                  FROM sku_cost c
                  JOIN sku s ON s.id = c.sku_id
                  JOIN product p ON p.id = s.product_id
                 ORDER BY s.sku_code
                 LIMIT ?
                """, (rs, rowNum) -> String.join(",",
                csv(rs.getString("sku_code")),
                csv(rs.getString("title")),
                String.valueOf(rs.getLong("quantity")),
                String.valueOf(rs.getLong("amount")),
                String.valueOf(rs.getLong("avg_cost_amount")),
                String.format("%.2f", rs.getLong("avg_cost_amount") / 100.0),
                String.valueOf(rs.getBoolean("stale"))), Math.min(Math.max(limit, 1), 5000));
    }

    public String csv(int limit) {
        StringBuilder builder = new StringBuilder(HEADER).append('\n');
        for (String row : rows(limit)) {
            builder.append(row).append('\n');
        }
        return builder.toString();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        return value.contains(",") || value.contains("\"")
                ? "\"" + value.replace("\"", "\"\"") + "\""
                : value;
    }
}
