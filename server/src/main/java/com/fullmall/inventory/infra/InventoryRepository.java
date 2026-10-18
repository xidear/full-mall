package com.fullmall.inventory.infra;

import com.fullmall.inventory.domain.InventoryBatch;
import com.fullmall.inventory.domain.InventoryBatchStatus;
import com.fullmall.inventory.domain.LocationAvailability;
import com.fullmall.inventory.domain.LocationType;
import com.fullmall.inventory.domain.Reservation;
import com.fullmall.inventory.domain.ReservationStatus;
import com.fullmall.inventory.domain.StockMove;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class InventoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public InventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<InventoryBatch> findAvailableBatches(UUID skuId, LocationType locationType, UUID locationId,
                                                     LocalDate minimumExpiry) {
        return jdbcTemplate.query("""
                SELECT id, sku_id, location_type, location_id, batch_no, production_date, expiry_date,
                       quantity, reserved_quantity, status, unit_cost_amount, unit_cost_currency, received_at
                FROM inventory_batch
                WHERE sku_id = ?
                  AND location_type = ?
                  AND location_id = ?
                  AND status IN ('AVAILABLE', 'NEAR_EXPIRY')
                  AND quantity > reserved_quantity
                  AND (expiry_date IS NULL OR expiry_date >= ?)
                ORDER BY expiry_date ASC NULLS LAST, received_at ASC
                """, (rs, rowNum) -> mapBatch(rs), skuId, locationType.name(), locationId, minimumExpiry);
    }

    public List<LocationAvailability> findAvailableLocations(UUID skuId, LocalDate minimumExpiry) {
        return jdbcTemplate.query("""
                SELECT location_type, location_id, SUM(quantity - reserved_quantity) AS available_quantity
                FROM inventory_batch
                WHERE sku_id = ?
                  AND status IN ('AVAILABLE', 'NEAR_EXPIRY')
                  AND quantity > reserved_quantity
                  AND (expiry_date IS NULL OR expiry_date >= ?)
                GROUP BY location_type, location_id
                HAVING SUM(quantity - reserved_quantity) > 0
                """, (rs, rowNum) -> new LocationAvailability(
                        LocationType.valueOf(rs.getString("location_type")),
                        rs.getObject("location_id", UUID.class),
                        rs.getInt("available_quantity")), skuId, minimumExpiry);
    }

    public boolean reserve(UUID batchId, int quantity, LocalDate minimumExpiry) {
        int updated = jdbcTemplate.update("""
                UPDATE inventory_batch
                SET reserved_quantity = reserved_quantity + ?
                WHERE id = ?
                  AND status IN ('AVAILABLE', 'NEAR_EXPIRY')
                  AND quantity - reserved_quantity >= ?
                  AND (expiry_date IS NULL OR expiry_date >= ?)
                """, quantity, batchId, quantity, minimumExpiry);
        return updated == 1;
    }

    public boolean release(UUID batchId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE inventory_batch
                SET reserved_quantity = GREATEST(reserved_quantity - ?, 0)
                WHERE id = ?
                """, quantity, batchId);
        return updated == 1;
    }

    public boolean ship(UUID batchId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE inventory_batch
                SET quantity = quantity - ?, reserved_quantity = reserved_quantity - ?
                WHERE id = ?
                  AND quantity >= ?
                  AND reserved_quantity >= ?
                """, quantity, quantity, batchId, quantity, quantity);
        return updated == 1;
    }

    public void insertReservation(Reservation reservation) {
        jdbcTemplate.update("""
                INSERT INTO reservation (id, order_id, order_line_id, batch_id, quantity, status, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, now())
                """,
                reservation.id(),
                reservation.orderId(),
                reservation.orderLineId(),
                reservation.batchId(),
                reservation.quantity(),
                reservation.status().name(),
                reservation.expiresAt() == null ? null : java.sql.Timestamp.from(reservation.expiresAt()));
    }

    public List<Reservation> findReservationsByOrder(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT id, order_id, order_line_id, batch_id, quantity, status, expires_at
                FROM reservation
                WHERE order_id = ?
                ORDER BY created_at
                """, (rs, rowNum) -> new Reservation(
                rs.getObject("id", UUID.class),
                rs.getObject("order_id", UUID.class),
                rs.getObject("order_line_id", UUID.class),
                rs.getObject("batch_id", UUID.class),
                rs.getInt("quantity"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant()), orderId);
    }

    public List<Reservation> findReservationsByOrderLines(UUID orderId, List<UUID> orderLineIds) {
        if (orderLineIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(orderLineIds.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(orderId);
        args.addAll(orderLineIds);
        String sql = """
                SELECT id, order_id, order_line_id, batch_id, quantity, status, expires_at
                FROM reservation
                WHERE order_id = ? AND order_line_id IN (%s) AND status = 'CONFIRMED'
                ORDER BY created_at
                """.formatted(placeholders);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Reservation(
                rs.getObject("id", UUID.class),
                rs.getObject("order_id", UUID.class),
                rs.getObject("order_line_id", UUID.class),
                rs.getObject("batch_id", UUID.class),
                rs.getInt("quantity"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant()), args.toArray());
    }

    public List<Reservation> findExpiredReservations() {
        return jdbcTemplate.query("""
                SELECT id, order_id, order_line_id, batch_id, quantity, status, expires_at
                FROM reservation
                WHERE status = 'RESERVED' AND expires_at < now()
                ORDER BY expires_at
                LIMIT 200
                """, (rs, rowNum) -> new Reservation(
                rs.getObject("id", UUID.class),
                rs.getObject("order_id", UUID.class),
                rs.getObject("order_line_id", UUID.class),
                rs.getObject("batch_id", UUID.class),
                rs.getInt("quantity"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant()));
    }

    public void updateReservationStatus(UUID reservationId, ReservationStatus expected, ReservationStatus next) {
        jdbcTemplate.update("""
                UPDATE reservation
                SET status = ?
                WHERE id = ? AND status = ?
                """, next.name(), reservationId, expected.name());
    }

    public void confirmOrderReservations(UUID orderId) {
        jdbcTemplate.update("""
                UPDATE reservation
                SET status = 'CONFIRMED', expires_at = NULL
                WHERE order_id = ? AND status = 'RESERVED'
                """, orderId);
    }

    public void insertStockMove(StockMove move) {
        jdbcTemplate.update("""
                INSERT INTO stock_move (id, batch_id, sku_id, location_type, location_id,
                                        move_type, quantity, reference_type, reference_id, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """,
                UUID.randomUUID(),
                move.batchId(),
                move.skuId(),
                move.locationType() == null ? null : move.locationType().name(),
                move.locationId(),
                move.moveType(),
                move.quantity(),
                move.referenceType(),
                move.referenceId());
    }

    public List<InventoryBatch> findBatches(LocationType locationType, UUID locationId, UUID skuId, Integer expiringInDays) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, sku_id, location_type, location_id, batch_no, production_date, expiry_date,
                       quantity, reserved_quantity, status, unit_cost_amount, unit_cost_currency, received_at
                FROM inventory_batch
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (locationType != null) {
            sql.append(" AND location_type = ?");
            args.add(locationType.name());
        }
        if (locationId != null) {
            sql.append(" AND location_id = ?");
            args.add(locationId);
        }
        if (skuId != null) {
            sql.append(" AND sku_id = ?");
            args.add(skuId);
        }
        if (expiringInDays != null) {
            sql.append(" AND expiry_date IS NOT NULL AND expiry_date <= CURRENT_DATE + (? * INTERVAL '1 day')");
            args.add(expiringInDays);
        }
        sql.append(" ORDER BY expiry_date ASC NULLS LAST, batch_no ASC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapBatch(rs), args.toArray());
    }

    private InventoryBatch mapBatch(ResultSet rs) throws SQLException {
        Long unitCostAmount = rs.getObject("unit_cost_amount", Long.class);
        String unitCostCurrency = rs.getString("unit_cost_currency");
        Money unitCost = unitCostAmount == null ? null : new Money(unitCostAmount, unitCostCurrency);
        return new InventoryBatch(
                rs.getObject("id", UUID.class),
                rs.getObject("sku_id", UUID.class),
                LocationType.valueOf(rs.getString("location_type")),
                rs.getObject("location_id", UUID.class),
                rs.getString("batch_no"),
                rs.getDate("production_date") == null ? null : rs.getDate("production_date").toLocalDate(),
                rs.getDate("expiry_date") == null ? null : rs.getDate("expiry_date").toLocalDate(),
                rs.getInt("quantity"),
                rs.getInt("reserved_quantity"),
                InventoryBatchStatus.valueOf(rs.getString("status")),
                unitCost,
                requireReceivedAt(rs));
    }

    private Instant requireReceivedAt(ResultSet rs) throws SQLException {
        java.sql.Timestamp receivedAt = rs.getTimestamp("received_at");
        if (receivedAt == null) {
            throw new SQLException("inventory_batch.received_at must not be null");
        }
        return receivedAt.toInstant();
    }
}