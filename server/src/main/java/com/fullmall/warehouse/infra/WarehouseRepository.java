package com.fullmall.warehouse.infra;

import com.fullmall.platform.money.Money;
import com.fullmall.warehouse.api.WarehouseDtos.LocationView;
import com.fullmall.warehouse.api.WarehouseDtos.ReceiptLineView;
import com.fullmall.warehouse.api.WarehouseDtos.ReceiptView;
import com.fullmall.warehouse.api.WarehouseDtos.ReplenishmentSuggestion;
import com.fullmall.warehouse.api.WarehouseDtos.StocktakeLineView;
import com.fullmall.warehouse.api.WarehouseDtos.StocktakeView;
import com.fullmall.warehouse.api.WarehouseDtos.TransferLineView;
import com.fullmall.warehouse.api.WarehouseDtos.TransferView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class WarehouseRepository {

    private final JdbcTemplate jdbcTemplate;

    public WarehouseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LocationView> locations() {
        return jdbcTemplate.query("""
                SELECT id, name, 'WAREHOUSE' AS type FROM warehouse WHERE active = true
                UNION ALL
                SELECT id, name, 'STORE' AS type FROM store WHERE active = true
                ORDER BY type, name
                """, (rs, rowNum) -> new LocationView(
                rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("type")));
    }

    // ------------------------------------------------------------------ receipts

    public UUID insertReceipt(String receiptNo, String locationType, UUID locationId,
                              UUID supplierId, String note) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO goods_receipt (id, receipt_no, location_type, location_id, supplier_id, status, note)
                VALUES (?, ?, ?, ?, ?, 'DRAFT', ?)
                """, id, receiptNo, locationType, locationId, supplierId, note);
        return id;
    }

    public void insertReceiptLine(UUID receiptId, UUID skuId, String batchNo, LocalDate productionDate,
                                  LocalDate expiryDate, int quantity, Long cost, String currency) {
        jdbcTemplate.update("""
                INSERT INTO goods_receipt_line (id, receipt_id, sku_id, batch_no, production_date,
                                                expiry_date, quantity, unit_cost_amount, currency)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), receiptId, skuId, batchNo, productionDate, expiryDate,
                quantity, cost, currency);
    }

    public void markReceiptConfirmed(UUID receiptId, Instant confirmedAt) {
        jdbcTemplate.update("""
                UPDATE goods_receipt SET status = 'CONFIRMED', confirmed_at = ?
                WHERE id = ? AND status = 'DRAFT'
                """, Timestamp.from(confirmedAt), receiptId);
    }

    public ReceiptView findReceipt(UUID receiptId) {
        List<ReceiptView> rows = jdbcTemplate.query("""
                SELECT id, receipt_no, location_type, location_id, status, note, created_at, confirmed_at
                FROM goods_receipt WHERE id = ?
                """, (rs, rowNum) -> mapReceipt(rs), receiptId);
        return rows.isEmpty() ? null : withReceiptLines(rows.get(0));
    }

    public List<ReceiptView> listReceipts(int limit) {
        return jdbcTemplate.query("""
                SELECT id, receipt_no, location_type, location_id, status, note, created_at, confirmed_at
                FROM goods_receipt ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> withReceiptLines(mapReceipt(rs)), limit);
    }

    private ReceiptView withReceiptLines(ReceiptView receipt) {
        List<ReceiptLineView> lines = jdbcTemplate.query("""
                SELECT l.id, l.sku_id, s.sku_code, COALESCE(pt.title, p.title) AS title, l.batch_no,
                       l.production_date, l.expiry_date, l.quantity, l.unit_cost_amount, l.currency
                FROM goods_receipt_line l
                JOIN sku s ON s.id = l.sku_id
                JOIN product p ON p.id = s.product_id
                LEFT JOIN product_translation pt ON pt.product_id = p.id AND pt.locale = 'zh-CN'
                WHERE l.receipt_id = ? ORDER BY l.id
                """, (rs, rowNum) -> {
            Long cost = rs.getObject("unit_cost_amount", Long.class);
            String currency = rs.getString("currency");
            return new ReceiptLineView(
                    rs.getObject("id", UUID.class),
                    rs.getObject("sku_id", UUID.class),
                    rs.getString("sku_code"),
                    rs.getString("title"),
                    rs.getString("batch_no"),
                    rs.getObject("production_date", LocalDate.class),
                    rs.getObject("expiry_date", LocalDate.class),
                    rs.getInt("quantity"),
                    cost == null ? null : new Money(cost, currency));
        }, receipt.id());
        return new ReceiptView(receipt.id(), receipt.receiptNo(), receipt.locationType(), receipt.locationId(),
                receipt.status(), receipt.note(), receipt.createdAt(), receipt.confirmedAt(), lines);
    }

    private ReceiptView mapReceipt(ResultSet rs) throws SQLException {
        return new ReceiptView(
                rs.getObject("id", UUID.class), rs.getString("receipt_no"), rs.getString("location_type"),
                rs.getObject("location_id", UUID.class), rs.getString("status"), rs.getString("note"),
                instant(rs, "created_at"), instant(rs, "confirmed_at"), List.of());
    }

    // ------------------------------------------------------------------ stocktake

    public UUID insertStocktake(String stocktakeNo, String locationType, UUID locationId, String note) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO stocktake (id, stocktake_no, location_type, location_id, status, note)
                VALUES (?, ?, ?, ?, 'DRAFT', ?)
                """, id, stocktakeNo, locationType, locationId, note);
        return id;
    }

    public void insertStocktakeLine(UUID stocktakeId, UUID batchId, UUID skuId, int systemQuantity) {
        jdbcTemplate.update("""
                INSERT INTO stocktake_line (id, stocktake_id, batch_id, sku_id, system_quantity)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), stocktakeId, batchId, skuId, systemQuantity);
    }

    public List<BatchRow> batchesAt(String locationType, UUID locationId) {
        return jdbcTemplate.query("""
                SELECT id, sku_id, batch_no, production_date, expiry_date, quantity, reserved_quantity,
                       unit_cost_amount, unit_cost_currency
                FROM inventory_batch
                WHERE location_type = ? AND location_id = ?
                ORDER BY expiry_date ASC NULLS LAST, batch_no
                """, (rs, rowNum) -> mapBatch(rs), locationType, locationId);
    }

    public StocktakeView findStocktake(UUID stocktakeId) {
        List<StocktakeView> rows = jdbcTemplate.query("""
                SELECT id, stocktake_no, location_type, location_id, status, note, created_at, confirmed_at
                FROM stocktake WHERE id = ?
                """, (rs, rowNum) -> mapStocktake(rs), stocktakeId);
        return rows.isEmpty() ? null : withStocktakeLines(rows.get(0));
    }

    public List<StocktakeView> listStocktakes(int limit) {
        return jdbcTemplate.query("""
                SELECT id, stocktake_no, location_type, location_id, status, note, created_at, confirmed_at
                FROM stocktake ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> withStocktakeLines(mapStocktake(rs)), limit);
    }

    private StocktakeView withStocktakeLines(StocktakeView stocktake) {
        List<StocktakeLineView> lines = jdbcTemplate.query("""
                SELECT l.id, l.batch_id, l.sku_id, s.sku_code, COALESCE(pt.title, p.title) AS title,
                       b.batch_no, b.expiry_date, l.system_quantity, l.counted_quantity, l.variance, l.reason
                FROM stocktake_line l
                JOIN inventory_batch b ON b.id = l.batch_id
                JOIN sku s ON s.id = l.sku_id
                JOIN product p ON p.id = s.product_id
                LEFT JOIN product_translation pt ON pt.product_id = p.id AND pt.locale = 'zh-CN'
                WHERE l.stocktake_id = ? ORDER BY l.id
                """, (rs, rowNum) -> new StocktakeLineView(
                rs.getObject("id", UUID.class), rs.getObject("batch_id", UUID.class),
                rs.getObject("sku_id", UUID.class), rs.getString("sku_code"), rs.getString("title"),
                rs.getString("batch_no"), rs.getObject("expiry_date", LocalDate.class),
                rs.getInt("system_quantity"),
                rs.getObject("counted_quantity", Integer.class),
                rs.getObject("variance", Integer.class),
                rs.getString("reason")), stocktake.id());
        return new StocktakeView(stocktake.id(), stocktake.stocktakeNo(), stocktake.locationType(),
                stocktake.locationId(), stocktake.status(), stocktake.note(), stocktake.createdAt(),
                stocktake.confirmedAt(), lines);
    }

    private StocktakeView mapStocktake(ResultSet rs) throws SQLException {
        return new StocktakeView(rs.getObject("id", UUID.class), rs.getString("stocktake_no"),
                rs.getString("location_type"), rs.getObject("location_id", UUID.class),
                rs.getString("status"), rs.getString("note"), instant(rs, "created_at"),
                instant(rs, "confirmed_at"), List.of());
    }

    public void updateStocktakeLine(UUID lineId, int countedQuantity, int variance, String reason) {
        jdbcTemplate.update("""
                UPDATE stocktake_line SET counted_quantity = ?, variance = ?, reason = ?
                WHERE id = ?
                """, countedQuantity, variance, reason, lineId);
    }

    public void setBatchQuantity(UUID batchId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE inventory_batch SET quantity = ? WHERE id = ?
                """, quantity, batchId);
        if (updated != 1) {
            throw new IllegalStateException("Batch not found: " + batchId);
        }
    }

    public void markStocktakeConfirmed(UUID stocktakeId, Instant confirmedAt) {
        jdbcTemplate.update("""
                UPDATE stocktake SET status = 'CONFIRMED', confirmed_at = ?
                WHERE id = ? AND status = 'DRAFT'
                """, Timestamp.from(confirmedAt), stocktakeId);
    }

    // ------------------------------------------------------------------ transfer

    public UUID insertTransfer(String transferNo, String fromType, UUID fromId, String toType,
                               UUID toId, String note) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO stock_transfer (id, transfer_no, from_type, from_id, to_type, to_id, status, note)
                VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?)
                """, id, transferNo, fromType, fromId, toType, toId, note);
        return id;
    }

    public void insertTransferLine(UUID transferId, UUID skuId, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO stock_transfer_line (id, transfer_id, sku_id, quantity)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), transferId, skuId, quantity);
    }

    public TransferView findTransfer(UUID transferId) {
        List<TransferView> rows = jdbcTemplate.query("""
                SELECT id, transfer_no, from_type, from_id, to_type, to_id, status, note,
                       created_at, shipped_at, received_at
                FROM stock_transfer WHERE id = ?
                """, (rs, rowNum) -> mapTransfer(rs), transferId);
        return rows.isEmpty() ? null : withTransferLines(rows.get(0));
    }

    public List<TransferView> listTransfers(int limit) {
        return jdbcTemplate.query("""
                SELECT id, transfer_no, from_type, from_id, to_type, to_id, status, note,
                       created_at, shipped_at, received_at
                FROM stock_transfer ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> withTransferLines(mapTransfer(rs)), limit);
    }

    private TransferView withTransferLines(TransferView transfer) {
        List<TransferLineView> lines = jdbcTemplate.query("""
                SELECT l.id, l.sku_id, s.sku_code, COALESCE(pt.title, p.title) AS title, l.quantity
                FROM stock_transfer_line l
                JOIN sku s ON s.id = l.sku_id
                JOIN product p ON p.id = s.product_id
                LEFT JOIN product_translation pt ON pt.product_id = p.id AND pt.locale = 'zh-CN'
                WHERE l.transfer_id = ? ORDER BY l.id
                """, (rs, rowNum) -> new TransferLineView(
                rs.getObject("id", UUID.class), rs.getObject("sku_id", UUID.class),
                rs.getString("sku_code"), rs.getString("title"), rs.getInt("quantity")), transfer.id());
        return new TransferView(transfer.id(), transfer.transferNo(), transfer.fromType(), transfer.fromId(),
                transfer.toType(), transfer.toId(), transfer.status(), transfer.note(), transfer.createdAt(),
                transfer.shippedAt(), transfer.receivedAt(), lines);
    }

    private TransferView mapTransfer(ResultSet rs) throws SQLException {
        return new TransferView(rs.getObject("id", UUID.class), rs.getString("transfer_no"),
                rs.getString("from_type"), rs.getObject("from_id", UUID.class),
                rs.getString("to_type"), rs.getObject("to_id", UUID.class),
                rs.getString("status"), rs.getString("note"), instant(rs, "created_at"),
                instant(rs, "shipped_at"), instant(rs, "received_at"), List.of());
    }

    public List<BatchRow> availableBatchesAt(UUID skuId, String locationType, UUID locationId) {
        return jdbcTemplate.query("""
                SELECT id, sku_id, batch_no, production_date, expiry_date, quantity, reserved_quantity,
                       unit_cost_amount, unit_cost_currency
                FROM inventory_batch
                WHERE sku_id = ? AND location_type = ? AND location_id = ?
                  AND status IN ('AVAILABLE', 'NEAR_EXPIRY')
                  AND quantity > reserved_quantity
                ORDER BY expiry_date ASC NULLS LAST, received_at ASC
                """, (rs, rowNum) -> mapBatch(rs), skuId, locationType, locationId);
    }

    public boolean decrementForTransfer(UUID batchId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE inventory_batch SET quantity = quantity - ?
                WHERE id = ? AND quantity - reserved_quantity >= ?
                """, quantity, batchId, quantity);
        return updated == 1;
    }

    public void insertTransferAllocation(UUID transferLineId, BatchRow batch, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO stock_transfer_allocation (id, transfer_line_id, source_batch_id, batch_no,
                                                       production_date, expiry_date, quantity,
                                                       unit_cost_amount, currency)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), transferLineId, batch.id(), batch.batchNo(),
                batch.productionDate(), batch.expiryDate(), quantity,
                batch.unitCost() == null ? null : batch.unitCost().amount(),
                batch.unitCost() == null ? null : batch.unitCost().currency());
    }

    public void markTransferShipped(UUID transferId, Instant shippedAt) {
        jdbcTemplate.update("""
                UPDATE stock_transfer SET status = 'IN_TRANSIT', shipped_at = ?
                WHERE id = ? AND status = 'DRAFT'
                """, Timestamp.from(shippedAt), transferId);
    }

    public List<AllocationRow> allocations(UUID transferId) {
        return jdbcTemplate.query("""
                SELECT a.id, a.transfer_line_id, a.batch_no, a.production_date, a.expiry_date,
                       a.quantity, a.unit_cost_amount, a.currency, l.sku_id
                FROM stock_transfer_allocation a
                JOIN stock_transfer_line l ON l.id = a.transfer_line_id
                WHERE l.transfer_id = ?
                ORDER BY a.id
                """, (rs, rowNum) -> new AllocationRow(
                rs.getObject("sku_id", UUID.class), rs.getString("batch_no"),
                rs.getObject("production_date", LocalDate.class),
                rs.getObject("expiry_date", LocalDate.class),
                rs.getInt("quantity"),
                rs.getObject("unit_cost_amount", Long.class),
                rs.getString("currency")), transferId);
    }

    public UUID upsertDestinationBatch(String locationType, UUID locationId, AllocationRow allocation) {
        int updated = jdbcTemplate.update("""
                UPDATE inventory_batch SET quantity = quantity + ?
                WHERE sku_id = ? AND location_type = ? AND location_id = ? AND batch_no = ?
                """, allocation.quantity(), allocation.skuId(), locationType, locationId, allocation.batchNo());
        if (updated == 1) {
            List<UUID> ids = jdbcTemplate.query("""
                    SELECT id FROM inventory_batch
                    WHERE sku_id = ? AND location_type = ? AND location_id = ? AND batch_no = ?
                    """, (rs, rowNum) -> rs.getObject("id", UUID.class),
                    allocation.skuId(), locationType, locationId, allocation.batchNo());
            if (ids.isEmpty()) {
                throw new IllegalStateException("Destination batch missing after update");
            }
            return ids.get(0);
        }
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                             production_date, expiry_date, quantity, reserved_quantity,
                                             status, unit_cost_amount, unit_cost_currency, received_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 'AVAILABLE', ?, ?, now(), now())
                """, id, allocation.skuId(), locationType, locationId, allocation.batchNo(),
                allocation.productionDate(), allocation.expiryDate(), allocation.quantity(),
                allocation.unitCostAmount(), allocation.currency());
        return id;
    }

    public void markTransferReceived(UUID transferId, Instant receivedAt) {
        jdbcTemplate.update("""
                UPDATE stock_transfer SET status = 'RECEIVED', received_at = ?
                WHERE id = ? AND status = 'IN_TRANSIT'
                """, Timestamp.from(receivedAt), transferId);
    }

    // ------------------------------------------------------------------ stock / moves

    public boolean batchExists(UUID skuId, String locationType, UUID locationId, String batchNo) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_batch
                WHERE sku_id = ? AND location_type = ? AND location_id = ? AND batch_no = ?
                """, Integer.class, skuId, locationType, locationId, batchNo);
        return count != null && count > 0;
    }

    public UUID insertBatch(UUID skuId, String locationType, UUID locationId, String batchNo,
                            LocalDate productionDate, LocalDate expiryDate, int quantity,
                            Long unitCost, String currency) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                             production_date, expiry_date, quantity, reserved_quantity,
                                             status, unit_cost_amount, unit_cost_currency, received_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 'AVAILABLE', ?, ?, now(), now())
                """, id, skuId, locationType, locationId, batchNo, productionDate, expiryDate,
                quantity, unitCost, currency);
        return id;
    }

    public void insertStockMove(UUID batchId, UUID skuId, String locationType, UUID locationId,
                                String moveType, int quantity, String referenceType, UUID referenceId) {
        jdbcTemplate.update("""
                INSERT INTO stock_move (id, batch_id, sku_id, location_type, location_id,
                                        move_type, quantity, reference_type, reference_id, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """, UUID.randomUUID(), batchId, skuId, locationType, locationId, moveType,
                quantity, referenceType, referenceId);
    }

    public List<ReplenishmentSuggestion> replenishment(String locationType, UUID locationId, int target) {
        return jdbcTemplate.query("""
                SELECT b.sku_id, s.sku_code, COALESCE(pt.title, p.title) AS title,
                       SUM(GREATEST(b.quantity - b.reserved_quantity, 0)) AS available,
                       MIN(b.expiry_date) AS nearest_expiry
                FROM inventory_batch b
                JOIN sku s ON s.id = b.sku_id
                JOIN product p ON p.id = s.product_id
                LEFT JOIN product_translation pt ON pt.product_id = p.id AND pt.locale = 'zh-CN'
                WHERE b.location_type = ? AND b.location_id = ?
                  AND b.status IN ('AVAILABLE', 'NEAR_EXPIRY')
                GROUP BY b.sku_id, s.sku_code, p.title, pt.title
                HAVING SUM(GREATEST(b.quantity - b.reserved_quantity, 0)) < ?
                ORDER BY available ASC
                LIMIT 100
                """, (rs, rowNum) -> {
            int available = rs.getInt("available");
            return new ReplenishmentSuggestion(
                    rs.getObject("sku_id", UUID.class), rs.getString("sku_code"), rs.getString("title"),
                    locationType, locationId, available, target, Math.max(target - available, 0),
                    rs.getObject("nearest_expiry", LocalDate.class));
        }, locationType, locationId, target);
    }

    // ------------------------------------------------------------------ mapping

    private BatchRow mapBatch(ResultSet rs) throws SQLException {
        Long cost = rs.getObject("unit_cost_amount", Long.class);
        String currency = rs.getString("unit_cost_currency");
        return new BatchRow(
                rs.getObject("id", UUID.class),
                rs.getObject("sku_id", UUID.class),
                rs.getString("batch_no"),
                rs.getObject("production_date", LocalDate.class),
                rs.getObject("expiry_date", LocalDate.class),
                rs.getInt("quantity"),
                rs.getInt("reserved_quantity"),
                cost == null ? null : new Money(cost, currency));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    public record BatchRow(
            UUID id, UUID skuId, String batchNo, LocalDate productionDate, LocalDate expiryDate,
            int quantity, int reservedQuantity, Money unitCost
    ) {
    }

    public record AllocationRow(
            UUID skuId, String batchNo, LocalDate productionDate, LocalDate expiryDate,
            int quantity, Long unitCostAmount, String currency
    ) {
    }
}