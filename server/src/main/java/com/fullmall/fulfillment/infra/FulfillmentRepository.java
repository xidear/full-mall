package com.fullmall.fulfillment.infra;

import com.fullmall.fulfillment.domain.FulfillmentLine;
import com.fullmall.fulfillment.domain.FulfillmentOrder;
import com.fullmall.fulfillment.domain.FulfillmentStatus;
import com.fullmall.fulfillment.domain.PickTask;
import com.fullmall.fulfillment.domain.PickTaskLine;
import com.fullmall.fulfillment.domain.Pickup;
import com.fullmall.fulfillment.dto.PickupDetail;
import com.fullmall.fulfillment.domain.PickupStatus;
import com.fullmall.platform.domain.FulfillmentType;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class FulfillmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public FulfillmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertFulfillmentOrder(FulfillmentOrder order) {
        jdbcTemplate.update("""
                INSERT INTO fulfillment_order (id, order_id, type, status, source_id, carrier_code,
                                              tracking_no, pickup_code, pickup_expires_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """,
                order.id(),
                order.orderId(),
                order.type().name(),
                order.status().name(),
                order.sourceId(),
                order.carrierCode(),
                order.trackingNo(),
                order.pickupCode(),
                order.pickupExpiresAt() == null ? null : java.sql.Timestamp.from(order.pickupExpiresAt()));
    }

    public void insertFulfillmentLine(FulfillmentLine line) {
        jdbcTemplate.update("""
                INSERT INTO fulfillment_line (id, fulfillment_order_id, order_line_id, sku_id, quantity)
                VALUES (?, ?, ?, ?, ?)
                """,
                line.id(), line.fulfillmentOrderId(), line.orderLineId(), line.skuId(), line.quantity());
    }

    public List<FulfillmentOrder> findByOrderId(UUID orderId) {
        List<FulfillmentOrder> orders = jdbcTemplate.query("""
                SELECT id, order_id, type, status, source_id, carrier_code, tracking_no,
                       pickup_code, pickup_expires_at
                FROM fulfillment_order
                WHERE order_id = ?
                ORDER BY created_at
                """, (rs, rowNum) -> mapFulfillmentBase(rs), orderId);
        return orders.stream().map(this::withLines).toList();
    }

    public FulfillmentOrder findById(UUID fulfillmentOrderId) {
        List<FulfillmentOrder> orders = jdbcTemplate.query("""
                SELECT id, order_id, type, status, source_id, carrier_code, tracking_no,
                       pickup_code, pickup_expires_at
                FROM fulfillment_order
                WHERE id = ?
                """, (rs, rowNum) -> mapFulfillmentBase(rs), fulfillmentOrderId);
        return orders.stream().findFirst().map(this::withLines).orElse(null);
    }

    public List<FulfillmentLine> findLines(UUID fulfillmentOrderId) {
        return jdbcTemplate.query("""
                SELECT id, fulfillment_order_id, order_line_id, sku_id, quantity
                FROM fulfillment_line
                WHERE fulfillment_order_id = ?
                ORDER BY id
                """, (rs, rowNum) -> new FulfillmentLine(
                rs.getObject("id", UUID.class),
                rs.getObject("fulfillment_order_id", UUID.class),
                rs.getObject("order_line_id", UUID.class),
                rs.getObject("sku_id", UUID.class),
                rs.getInt("quantity")), fulfillmentOrderId);
    }

    public void updateStatus(UUID fulfillmentOrderId, FulfillmentStatus status) {
        int updated = jdbcTemplate.update("""
                UPDATE fulfillment_order SET status = ? WHERE id = ?
                """, status.name(), fulfillmentOrderId);
        if (updated != 1) {
            throw new IllegalStateException("Fulfillment order not found: " + fulfillmentOrderId);
        }
    }

    public void updateShipment(UUID fulfillmentOrderId, String carrierCode, String trackingNo) {
        int updated = jdbcTemplate.update("""
                UPDATE fulfillment_order
                SET carrier_code = ?, tracking_no = ?
                WHERE id = ?
                """, carrierCode, trackingNo, fulfillmentOrderId);
        if (updated != 1) {
            throw new IllegalStateException("Fulfillment order not found: " + fulfillmentOrderId);
        }
    }

    public void updateSupplierAcceptance(UUID fulfillmentOrderId, String note, java.time.Instant acceptedAt) {
        int updated = jdbcTemplate.update("""
                UPDATE fulfillment_order
                SET supplier_accepted_at = ?, supplier_note = ?
                WHERE id = ?
                """, java.sql.Timestamp.from(acceptedAt), note, fulfillmentOrderId);
        if (updated != 1) {
            throw new IllegalStateException("Fulfillment order not found: " + fulfillmentOrderId);
        }
    }

    public void updateSupplierNote(UUID fulfillmentOrderId, String note) {
        int updated = jdbcTemplate.update("""
                UPDATE fulfillment_order SET supplier_note = ? WHERE id = ?
                """, note, fulfillmentOrderId);
        if (updated != 1) {
            throw new IllegalStateException("Fulfillment order not found: " + fulfillmentOrderId);
        }
    }

    public void insertPickTask(PickTask task) {
        jdbcTemplate.update("""
                INSERT INTO pick_task (id, fulfillment_order_id, store_id, status, created_at)
                VALUES (?, ?, ?, ?, now())
                """, task.id(), task.fulfillmentOrderId(), task.storeId(), task.status());
    }

    public void insertPickTaskLine(PickTaskLine line) {
        jdbcTemplate.update("""
                INSERT INTO pick_task_line (id, pick_task_id, sku_id, quantity, suggested_batch_id,
                                            suggested_expiry_date, picked_quantity)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                line.id(),
                line.pickTaskId(),
                line.skuId(),
                line.quantity(),
                line.suggestedBatchId(),
                line.suggestedExpiryDate() == null ? null : java.sql.Date.valueOf(line.suggestedExpiryDate()),
                line.pickedQuantity());
    }

    public PickTask findPickTask(UUID taskId) {
        List<PickTask> tasks = jdbcTemplate.query("""
                SELECT id, fulfillment_order_id, store_id, status
                FROM pick_task
                WHERE id = ?
                """, (rs, rowNum) -> mapPickTaskBase(rs), taskId);
        return tasks.stream()
                .findFirst()
                .map(task -> new PickTask(task.id(), task.fulfillmentOrderId(), task.storeId(),
                        task.status(), findPickTaskLines(task.id())))
                .orElse(null);
    }

    public List<PickTask> findPickTasksByStore(UUID storeId, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, fulfillment_order_id, store_id, status
                FROM pick_task
                WHERE store_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(storeId);
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY created_at");
        List<PickTask> tasks = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new PickTask(
                rs.getObject("id", UUID.class),
                rs.getObject("fulfillment_order_id", UUID.class),
                rs.getObject("store_id", UUID.class),
                rs.getString("status"),
                List.of()), args.toArray());
        return tasks.stream()
                .map(task -> new PickTask(task.id(), task.fulfillmentOrderId(), task.storeId(),
                        task.status(), findPickTaskLines(task.id())))
                .toList();
    }

    public List<PickTaskLine> findPickTaskLines(UUID taskId) {
        return jdbcTemplate.query("""
                SELECT ptl.id, ptl.pick_task_id, ptl.sku_id,
                       COALESCE(pt.title, p.title, ptl.sku_id::text) AS title,
                       ptl.quantity, ptl.suggested_batch_id, b.batch_no AS suggested_batch_no,
                       ptl.suggested_expiry_date, ptl.picked_quantity
                FROM pick_task_line ptl
                JOIN sku s ON s.id = ptl.sku_id
                JOIN product p ON p.id = s.product_id
                LEFT JOIN product_translation pt ON pt.product_id = p.id AND pt.locale = 'zh-CN'
                LEFT JOIN inventory_batch b ON b.id = ptl.suggested_batch_id
                WHERE ptl.pick_task_id = ?
                ORDER BY ptl.id
                """, (rs, rowNum) -> new PickTaskLine(
                rs.getObject("id", UUID.class),
                rs.getObject("pick_task_id", UUID.class),
                rs.getObject("sku_id", UUID.class),
                rs.getString("title"),
                rs.getInt("quantity"),
                rs.getObject("suggested_batch_id", UUID.class),
                rs.getString("suggested_batch_no"),
                rs.getDate("suggested_expiry_date") == null ? null : rs.getDate("suggested_expiry_date").toLocalDate(),
                rs.getInt("picked_quantity")), taskId);
    }

    public boolean markPickLinePicked(UUID lineId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE pick_task_line
                SET picked_quantity = ?
                WHERE id = ? AND picked_quantity = 0
                """, quantity, lineId);
        return updated == 1;
    }

    public long countUnpicked(UUID taskId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pick_task_line
                WHERE pick_task_id = ? AND picked_quantity < quantity
                """, Long.class, taskId);
        if (count == null) {
            throw new IllegalStateException("Unpicked count query returned null for task " + taskId);
        }
        return count;
    }

    public void updatePickTaskStatus(UUID taskId, String status) {
        int updated = jdbcTemplate.update("UPDATE pick_task SET status = ? WHERE id = ?", status, taskId);
        if (updated != 1) {
            throw new IllegalStateException("Pick task not found: " + taskId);
        }
    }

    public void insertPickup(Pickup pickup) {
        jdbcTemplate.update("""
                INSERT INTO pickup (id, fulfillment_order_id, store_id, pickup_code, status,
                                    expires_at, completed_at, contact_phone, total_uses, used_count, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """,
                pickup.id(),
                pickup.fulfillmentOrderId(),
                pickup.storeId(),
                pickup.pickupCode(),
                pickup.status().name(),
                java.sql.Timestamp.from(pickup.expiresAt()),
                pickup.completedAt() == null ? null : java.sql.Timestamp.from(pickup.completedAt()),
                pickup.contactPhone(),
                pickup.totalUses(),
                pickup.usedCount());
    }

    public Pickup findPickupByCode(String pickupCode) {
        List<Pickup> pickups = jdbcTemplate.query("""
                SELECT id, fulfillment_order_id, store_id, pickup_code, status, expires_at,
                       completed_at, contact_phone, total_uses, used_count
                FROM pickup
                WHERE pickup_code = ?
                """, (rs, rowNum) -> mapPickup(rs), pickupCode);
        return pickups.stream().findFirst().orElse(null);
    }

    public PickupDetail findPickupDetail(String pickupCode) {
        List<PickupDetail> details = jdbcTemplate.query("""
                SELECT p.pickup_code, p.status, p.expires_at, p.total_uses, p.used_count,
                       s.name AS store_name, o.order_no, o.contact_name, o.contact_phone,
                       o.currency, o.total_amount, f.id AS fulfillment_order_id
                FROM pickup p
                JOIN store s ON s.id = p.store_id
                JOIN fulfillment_order f ON f.id = p.fulfillment_order_id
                JOIN orders o ON o.id = f.order_id
                WHERE p.pickup_code = ?
                """, (rs, rowNum) -> {
            UUID fulfillmentOrderId = rs.getObject("fulfillment_order_id", UUID.class);
            return new PickupDetail(
                    rs.getString("pickup_code"),
                    rs.getString("status"),
                    rs.getString("store_name"),
                    rs.getString("order_no"),
                    rs.getString("contact_name"),
                    rs.getString("contact_phone"),
                    new Money(rs.getLong("total_amount"), rs.getString("currency")),
                    rs.getInt("total_uses"),
                    rs.getInt("used_count"),
                    Math.max(0, rs.getInt("total_uses") - rs.getInt("used_count")),
                    rs.getTimestamp("expires_at").toInstant(),
                    findPickupItems(fulfillmentOrderId));
        }, pickupCode);
        return details.stream().findFirst().orElse(null);
    }

    public String incrementPickupUsage(UUID pickupId) {
        List<String> statuses = jdbcTemplate.query("""
                UPDATE pickup
                SET used_count = used_count + 1,
                    status = CASE WHEN used_count + 1 >= total_uses THEN 'COMPLETED' ELSE 'PARTIALLY_USED' END,
                    completed_at = CASE WHEN used_count + 1 >= total_uses THEN now() ELSE completed_at END
                WHERE id = ?
                  AND used_count < total_uses
                  AND status IN ('READY', 'PARTIALLY_USED')
                RETURNING status
                """, (rs, rowNum) -> rs.getString("status"), pickupId);
        return statuses.stream().findFirst().orElse(null);
    }

    private List<PickupDetail.PickupItem> findPickupItems(UUID fulfillmentOrderId) {
        return jdbcTemplate.query("""
                SELECT ol.title_snapshot, ol.quantity
                FROM fulfillment_line fl
                JOIN order_line ol ON ol.id = fl.order_line_id
                WHERE fl.fulfillment_order_id = ?
                ORDER BY ol.title_snapshot
                """, (rs, rowNum) -> new PickupDetail.PickupItem(
                rs.getString("title_snapshot"),
                rs.getInt("quantity")), fulfillmentOrderId);
    }

    public Pickup findPickupByFulfillmentId(UUID fulfillmentOrderId) {
        List<Pickup> pickups = jdbcTemplate.query("""
                SELECT id, fulfillment_order_id, store_id, pickup_code, status, expires_at,
                       completed_at, contact_phone, total_uses, used_count
                FROM pickup
                WHERE fulfillment_order_id = ?
                """, (rs, rowNum) -> mapPickup(rs), fulfillmentOrderId);
        return pickups.stream().findFirst().orElse(null);
    }

    public void updatePickupStatus(UUID pickupId, PickupStatus expected, PickupStatus next, Instant completedAt) {
        int updated = jdbcTemplate.update("""
                UPDATE pickup
                SET status = ?, completed_at = ?
                WHERE id = ? AND status = ?
                """, next.name(), completedAt == null ? null : java.sql.Timestamp.from(completedAt),
                pickupId, expected.name());
        if (updated != 1) {
            throw new IllegalStateException("Pickup status transition failed: " + pickupId
                    + " " + expected + " -> " + next);
        }
    }

    public void updatePickupStatusByFulfillmentId(UUID fulfillmentOrderId, PickupStatus status) {
        int updated = jdbcTemplate.update("""
                UPDATE pickup SET status = ? WHERE fulfillment_order_id = ?
                """, status.name(), fulfillmentOrderId);
        if (updated != 1) {
            throw new IllegalStateException("Pickup not found for fulfillment: " + fulfillmentOrderId);
        }
    }

    private FulfillmentOrder mapFulfillmentBase(ResultSet rs) throws SQLException {
        return new FulfillmentOrder(
                rs.getObject("id", UUID.class),
                rs.getObject("order_id", UUID.class),
                FulfillmentType.valueOf(rs.getString("type")),
                FulfillmentStatus.valueOf(rs.getString("status")),
                rs.getObject("source_id", UUID.class),
                rs.getString("carrier_code"),
                rs.getString("tracking_no"),
                rs.getString("pickup_code"),
                rs.getTimestamp("pickup_expires_at") == null ? null : rs.getTimestamp("pickup_expires_at").toInstant(),
                List.of());
    }

    private FulfillmentOrder withLines(FulfillmentOrder fulfillment) {
        return new FulfillmentOrder(
                fulfillment.id(), fulfillment.orderId(), fulfillment.type(), fulfillment.status(),
                fulfillment.sourceId(), fulfillment.carrierCode(), fulfillment.trackingNo(),
                fulfillment.pickupCode(), fulfillment.pickupExpiresAt(), findLines(fulfillment.id()));
    }

    private PickTask mapPickTaskBase(ResultSet rs) throws SQLException {
        return new PickTask(
                rs.getObject("id", UUID.class),
                rs.getObject("fulfillment_order_id", UUID.class),
                rs.getObject("store_id", UUID.class),
                rs.getString("status"),
                List.of());
    }

    private Pickup mapPickup(ResultSet rs) throws SQLException {
        return new Pickup(
                rs.getObject("id", UUID.class),
                rs.getObject("fulfillment_order_id", UUID.class),
                rs.getObject("store_id", UUID.class),
                rs.getString("pickup_code"),
                PickupStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                rs.getString("contact_phone"),
                rs.getInt("total_uses"),
                rs.getInt("used_count"));
    }
}