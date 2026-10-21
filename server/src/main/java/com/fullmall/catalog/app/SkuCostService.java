package com.fullmall.catalog.app;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * SKU 成本：按移动加权平均维护，并写成本流水。
 *
 * <p>成本来源必须是真实业务单据（收货/退货/调整），**不提供手填**；
 * 无记录时 {@link #current} 返回空，调用方不得当作 0。
 */
@Service
public class SkuCostService {

    private final JdbcTemplate jdbc;

    public SkuCostService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Snapshot(long quantity, long amount, long avgCost, boolean stale) {
    }

    /** 最近一个有成本的批次（来源可追溯）；没有返回 null。 */
    public java.util.Map<String, Object> latestBatch(UUID skuId) {
        var rows = jdbc.query("""
                SELECT batch_no, quantity, unit_cost_amount, unit_cost_currency, received_at
                  FROM inventory_batch
                 WHERE sku_id = ? AND unit_cost_amount IS NOT NULL
                 ORDER BY received_at DESC LIMIT 1
                """, (rs, rowNum) -> {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("batchNo", rs.getString("batch_no"));
            row.put("quantity", rs.getLong("quantity"));
            row.put("unitCost", rs.getLong("unit_cost_amount"));
            row.put("currency", rs.getString("unit_cost_currency"));
            row.put("receivedAt", rs.getTimestamp("received_at").toInstant());
            return row;
        }, skuId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 读取当前成本；从未入库过返回 null。 */
    public Snapshot current(UUID skuId) {
        var rows = jdbc.query("""
                SELECT quantity, amount, avg_cost_amount, stale FROM sku_cost WHERE sku_id = ?
                """, (rs, rowNum) -> new Snapshot(rs.getLong("quantity"), rs.getLong("amount"),
                rs.getLong("avg_cost_amount"), rs.getBoolean("stale")), skuId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 收货入库：重算均价并写 RECEIPT 流水（同一事务，避免只改一半）。 */
    @Transactional
    public Snapshot receive(UUID skuId, long quantity, long unitCost, String referenceType,
                            String referenceId) {
        var current = current(skuId);
        var state = current == null
                ? MovingAverageCost.initial()
                : new MovingAverageCost.State(current.quantity(), current.amount(),
                        current.avgCost(), current.stale());
        var next = MovingAverageCost.receive(state, quantity, unitCost);

        jdbc.update("""
                INSERT INTO sku_cost (sku_id, quantity, amount, avg_cost_amount, stale, updated_at)
                VALUES (?, ?, ?, ?, ?, now())
                ON CONFLICT (sku_id) DO UPDATE
                   SET quantity = EXCLUDED.quantity, amount = EXCLUDED.amount,
                       avg_cost_amount = EXCLUDED.avg_cost_amount, stale = EXCLUDED.stale,
                       updated_at = now()
                """, skuId, next.quantity(), next.amount(), next.avgCost(), next.stale());
        insertMovement(skuId, "RECEIPT", quantity, unitCost, quantity * unitCost,
                referenceType, referenceId);
        return new Snapshot(next.quantity(), next.amount(), next.avgCost(), next.stale());
    }

    /**
     * 售后退货入库：**按原批次成本**回冲。
     *
     * <p>为什么不能用当前均价：退货商品当初是按批次成本进的货，
     * 用当前均价回冲会凭空产生利润或亏损（均价可能已经涨/跌）。
     * 批次没有成本记录时**明确拒绝**，不按均价"猜一个"。
     */
    @Transactional
    public Snapshot returnFromBatch(UUID skuId, UUID batchId, long quantity) {
        record BatchCost(long unitCost) {
        }
        var rows = jdbc.query("""
                SELECT unit_cost_amount FROM inventory_batch WHERE id = ? AND sku_id = ?
                """, (rs, rowNum) -> new BatchCost(rs.getLong("unit_cost_amount")), batchId, skuId);
        if (rows.isEmpty()) {
            throw com.fullmall.platform.error.ApiException.notFound("BATCH_NOT_FOUND",
                    "批次不存在或不属于该 SKU：" + batchId);
        }
        if (rows.get(0).unitCost() <= 0) {
            throw com.fullmall.platform.error.ApiException.conflict("BATCH_COST_UNKNOWN",
                    "该批次没有成本记录，无法按原批次回冲：" + batchId);
        }
        // 退货入库不增加批次数量（原批次是历史凭证），只回冲 SKU 级成本与金额
        var current = current(skuId);
        var state = current == null
                ? MovingAverageCost.initial()
                : new MovingAverageCost.State(current.quantity(), current.amount(),
                        current.avgCost(), current.stale());
        var next = MovingAverageCost.receive(state, quantity, rows.get(0).unitCost());
        jdbc.update("""
                INSERT INTO sku_cost (sku_id, quantity, amount, avg_cost_amount, stale, updated_at)
                VALUES (?, ?, ?, ?, ?, now())
                ON CONFLICT (sku_id) DO UPDATE
                   SET quantity = EXCLUDED.quantity, amount = EXCLUDED.amount,
                       avg_cost_amount = EXCLUDED.avg_cost_amount, stale = EXCLUDED.stale,
                       updated_at = now()
                """, skuId, next.quantity(), next.amount(), next.avgCost(), next.stale());
        insertMovement(skuId, "RETURN", quantity, rows.get(0).unitCost(),
                quantity * rows.get(0).unitCost(), "INVENTORY_BATCH", batchId.toString());
        return new Snapshot(next.quantity(), next.amount(), next.avgCost(), next.stale());
    }

    /** 销售出库：按均价结转，均价不变；库存不足直接报错。 */
    @Transactional
    public Snapshot issue(UUID skuId, long quantity, String referenceType, String referenceId) {
        var current = current(skuId);
        if (current == null) {
            throw com.fullmall.platform.error.ApiException.conflict("SKU_COST_UNKNOWN",
                    "该 SKU 没有成本记录，无法结转成本（需先入库）：" + skuId);
        }
        var state = new MovingAverageCost.State(current.quantity(), current.amount(),
                current.avgCost(), current.stale());
        var next = MovingAverageCost.issue(state, quantity);

        jdbc.update("""
                UPDATE sku_cost SET quantity = ?, amount = ?, stale = ?, updated_at = now()
                 WHERE sku_id = ?
                """, next.quantity(), next.amount(), next.stale(), skuId);
        insertMovement(skuId, "ISSUE", -quantity, current.avgCost(), quantity * current.avgCost(),
                referenceType, referenceId);
        return new Snapshot(next.quantity(), next.amount(), next.avgCost(), next.stale());
    }

    private void insertMovement(UUID skuId, String type, long quantity, long unitCost, long amount,
                                String referenceType, String referenceId) {
        jdbc.update("""
                INSERT INTO stock_cost_movement (id, sku_id, movement_type, quantity, unit_cost,
                                                 amount, reference_type, reference_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), skuId, type, quantity, unitCost, amount,
                referenceType, referenceId);
    }
}
