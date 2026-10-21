package com.fullmall.purchase.app;

import com.fullmall.catalog.app.SkuCostService;
import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 采购与收货：**成本的唯一合法来源**。
 *
 * <p>收货确认时按行写入成本（移动加权平均）。本次不写 `inventory_batch`（需要仓库/库位/效期），
 * 该部分仍在 G7 剩余范围内，已在文档标注，避免误以为库存批次也一起解决了。
 */
@Service
public class PurchaseService {

    private final JdbcTemplate jdbc;
    private final SkuCostService costService;

    public PurchaseService(JdbcTemplate jdbc, SkuCostService costService) {
        this.jdbc = jdbc;
        this.costService = costService;
    }

    public record Line(UUID skuId, long quantity, long unitCost) {
    }

    public record ReceiptResult(UUID purchaseId, int lines, long totalQuantity, long totalAmount) {
    }

    @Transactional
    public UUID create(UUID supplierId, String currency, List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            throw ApiException.badRequest("PURCHASE_LINES_EMPTY", "采购单至少需要一行");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO purchase_order (id, order_no, supplier_id, status, currency)
                VALUES (?, ?, ?, 'PLACED', ?)
                """, id, "PO-" + id.toString().substring(0, 12), supplierId, currency);
        for (Line line : lines) {
            if (line.quantity() <= 0) {
                throw ApiException.badRequest("PURCHASE_QUANTITY_INVALID", "采购数量必须大于 0");
            }
            if (line.unitCost() < 0) {
                throw ApiException.badRequest("PURCHASE_COST_INVALID", "采购单价不能为负");
            }
            jdbc.update("""
                    INSERT INTO purchase_order_line (id, purchase_id, sku_id, quantity, unit_cost)
                    VALUES (?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), id, line.skuId(), line.quantity(), line.unitCost());
        }
        return id;
    }

    /**
     * 确认收货：按行写成本 + 写库存批次（含批次单位成本）。
     *
     * <p>批次与成本必须同时落库：只写成本会让库存账对不上，只写批次则毛利无法核算。
     */
    @Transactional
    public ReceiptResult receive(UUID purchaseId, String locationType, UUID locationId) {
        if (locationType == null || locationType.isBlank() || locationId == null) {
            throw ApiException.badRequest("RECEIPT_LOCATION_REQUIRED",
                    "收货必须指定库存地点（locationType + locationId）");
        }
        return doReceive(purchaseId, locationType, locationId);
    }

    @Transactional
    public ReceiptResult receive(UUID purchaseId) {
        throw ApiException.badRequest("RECEIPT_LOCATION_REQUIRED",
                "收货必须指定库存地点：请调用 receive(purchaseId, locationType, locationId)");
    }

    private ReceiptResult doReceive(UUID purchaseId, String locationType, UUID locationId) {
        String status = jdbc.query("SELECT status FROM purchase_order WHERE id = ?",
                rs -> rs.next() ? rs.getString("status") : null, purchaseId);
        if (status == null) {
            throw ApiException.notFound("PURCHASE_NOT_FOUND", "采购单不存在：" + purchaseId);
        }
        if (!"PLACED".equals(status)) {
            throw ApiException.conflict("PURCHASE_NOT_RECEIVABLE",
                    "只有已下单（PLACED）的采购单可以收货，当前=" + status);
        }

        record Row(UUID id, UUID skuId, long quantity, long unitCost) {
        }
        List<Row> rows = jdbc.query("""
                SELECT id, sku_id, quantity, unit_cost FROM purchase_order_line
                 WHERE purchase_id = ? AND received_qty = 0
                 ORDER BY id
                """, (rs, rowNum) -> new Row(rs.getObject("id", UUID.class),
                rs.getObject("sku_id", UUID.class), rs.getLong("quantity"),
                rs.getLong("unit_cost")), purchaseId);

        long totalQuantity = 0;
        long totalAmount = 0;
        for (Row row : rows) {
            costService.receive(row.skuId(), row.quantity(), row.unitCost(),
                    "PURCHASE_RECEIPT", purchaseId.toString());
            jdbc.update("""
                    INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                                 quantity, unit_cost_amount, unit_cost_currency)
                    VALUES (?, ?, ?, ?, ?, ?, ?, (SELECT currency FROM purchase_order WHERE id = ?))
                    """, UUID.randomUUID(), row.skuId(), locationType.toUpperCase(), locationId,
                    batchNo(purchaseId, row.id()), row.quantity(), row.unitCost(), purchaseId);
            jdbc.update("UPDATE purchase_order_line SET received_qty = quantity WHERE id = ?", row.id());
            totalQuantity += row.quantity();
            totalAmount += row.quantity() * row.unitCost();
        }
        jdbc.update("""
                UPDATE purchase_order SET status = 'RECEIVED', received_at = now() WHERE id = ?
                """, purchaseId);
        return new ReceiptResult(purchaseId, rows.size(), totalQuantity, totalAmount);
    }

    /** 批次号：采购单 + 行号，便于追溯成本来源。 */
    private String batchNo(UUID purchaseId, UUID lineId) {
        return "PO-" + purchaseId.toString().substring(0, 8) + "-" + lineId.toString().substring(0, 6);
    }
}
