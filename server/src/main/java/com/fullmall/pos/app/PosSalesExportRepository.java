package com.fullmall.pos.app;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class PosSalesExportRepository {

    private final JdbcTemplate jdbcTemplate;

    public PosSalesExportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @param costAmount 该行批次分摊成本合计（`pos_sale_allocation.unit_cost_amount × quantity`）；
     *                   未录入成本时为 0，导出层必须据此标注"成本缺失"，不能当作零成本。
     */
    public record SaleLineRow(UUID saleLineId, String saleNo, String skuCode, int quantity,
                              int refundedQuantity, long lineTotalAmount, long costAmount,
                              long costedQuantity, String currency) {
    }

    public List<SaleLineRow> findRecent(int limit) {
        return jdbcTemplate.query("""
                SELECT sl.id, s.sale_no, sl.sku_code_snapshot, sl.quantity, sl.refunded_quantity,
                       sl.line_total_amount, sl.currency,
                       COALESCE(SUM(a.quantity * a.unit_cost_amount), 0) AS cost_amount,
                       COALESCE(SUM(CASE WHEN a.unit_cost_amount IS NULL THEN 0 ELSE a.quantity END), 0)
                           AS costed_quantity
                  FROM pos_sale_line sl
                  JOIN pos_sale s ON s.id = sl.sale_id
                  LEFT JOIN pos_sale_allocation a ON a.sale_line_id = sl.id
                 GROUP BY sl.id, s.sale_no, sl.sku_code_snapshot, sl.quantity, sl.refunded_quantity,
                          sl.line_total_amount, sl.currency, s.created_at
                 ORDER BY s.created_at DESC, sl.id
                 LIMIT ?
                """, (rs, rowNum) -> new SaleLineRow(
                rs.getObject("id", UUID.class),
                rs.getString("sale_no"),
                rs.getString("sku_code_snapshot"),
                rs.getInt("quantity"),
                rs.getInt("refunded_quantity"),
                rs.getLong("line_total_amount"),
                rs.getLong("cost_amount"),
                rs.getLong("costed_quantity"),
                rs.getString("currency")), limit);
    }
}