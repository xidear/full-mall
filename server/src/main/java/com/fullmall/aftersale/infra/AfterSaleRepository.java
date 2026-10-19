package com.fullmall.aftersale.infra;

import com.fullmall.aftersale.api.AfterSaleDtos.AfterSaleEventView;
import com.fullmall.aftersale.api.AfterSaleDtos.AfterSaleLineView;
import com.fullmall.aftersale.api.AfterSaleDtos.AfterSaleView;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class AfterSaleRepository {

    private final JdbcTemplate jdbcTemplate;

    public AfterSaleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertAfterSale(UUID id, String afterSaleNo, UUID orderId, String type, String reason,
                                String description, String phone, long refundAmount, String currency) {
        jdbcTemplate.update("""
                INSERT INTO after_sale (id, after_sale_no, order_id, type, status, reason, description,
                                        contact_phone, refund_amount, currency)
                VALUES (?, ?, ?, ?, 'REQUESTED', ?, ?, ?, ?, ?)
                """, id, afterSaleNo, orderId, type, reason, description, phone, refundAmount, currency);
    }

    public void insertLine(UUID afterSaleId, UUID orderLineId, UUID skuId, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO after_sale_line (id, after_sale_id, order_line_id, sku_id, quantity)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), afterSaleId, orderLineId, skuId, quantity);
    }

    public void insertEvent(UUID afterSaleId, String eventType, String note, String operatorName) {
        jdbcTemplate.update("""
                INSERT INTO after_sale_event (id, after_sale_id, event_type, note, operator_name)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), afterSaleId, eventType, note, operatorName);
    }

    public AfterSaleView find(UUID afterSaleId) {
        List<AfterSaleView> rows = jdbcTemplate.query("""
                SELECT a.id, a.after_sale_no, a.order_id, o.order_no, a.type, a.status, a.reason,
                       a.description, a.contact_phone, a.refund_amount, a.currency, a.admin_note,
                       a.created_at, a.approved_at, a.received_at, a.refunded_at, a.rejected_at
                FROM after_sale a JOIN orders o ON o.id = a.order_id
                WHERE a.id = ?
                """, (rs, rowNum) -> map(rs), afterSaleId);
        return rows.isEmpty() ? null : withChildren(rows.get(0));
    }

    public List<AfterSaleView> list(String status, int limit) {
        String filter = status == null ? "" : status.trim().toUpperCase();
        return jdbcTemplate.query("""
                SELECT a.id, a.after_sale_no, a.order_id, o.order_no, a.type, a.status, a.reason,
                       a.description, a.contact_phone, a.refund_amount, a.currency, a.admin_note,
                       a.created_at, a.approved_at, a.received_at, a.refunded_at, a.rejected_at
                FROM after_sale a JOIN orders o ON o.id = a.order_id
                WHERE (? = '' OR a.status = ?)
                ORDER BY a.created_at DESC LIMIT ?
                """, (rs, rowNum) -> withChildren(map(rs)), filter, filter, limit);
    }

    public List<AfterSaleView> listByOrder(UUID orderId, String phone) {
        return jdbcTemplate.query("""
                SELECT a.id, a.after_sale_no, a.order_id, o.order_no, a.type, a.status, a.reason,
                       a.description, a.contact_phone, a.refund_amount, a.currency, a.admin_note,
                       a.created_at, a.approved_at, a.received_at, a.refunded_at, a.rejected_at
                FROM after_sale a JOIN orders o ON o.id = a.order_id
                WHERE a.order_id = ? AND a.contact_phone = ?
                ORDER BY a.created_at DESC
                """, (rs, rowNum) -> withChildren(map(rs)), orderId, phone);
    }

    public int countActive(UUID orderId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM after_sale
                WHERE order_id = ? AND status IN ('REQUESTED', 'APPROVED', 'RECEIVED')
                """, Integer.class, orderId);
        return count == null ? 0 : count;
    }

    public long sumRefunded(UUID orderId) {
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount), 0) FROM after_sale
                WHERE order_id = ? AND status = 'REFUNDED'
                """, Long.class, orderId);
        return sum == null ? 0 : sum;
    }

    public void approve(UUID afterSaleId, String adminNote) {
        jdbcTemplate.update("""
                UPDATE after_sale SET status = 'APPROVED', admin_note = ?, approved_at = now()
                WHERE id = ? AND status = 'REQUESTED'
                """, adminNote, afterSaleId);
    }

    public void reject(UUID afterSaleId, String reason) {
        jdbcTemplate.update("""
                UPDATE after_sale SET status = 'REJECTED', admin_note = ?, rejected_at = now()
                WHERE id = ? AND status IN ('REQUESTED', 'APPROVED')
                """, reason, afterSaleId);
    }

    public void markReceived(UUID afterSaleId) {
        jdbcTemplate.update("""
                UPDATE after_sale SET status = 'RECEIVED', received_at = now()
                WHERE id = ? AND status = 'APPROVED'
                """, afterSaleId);
    }

    public void markRefunded(UUID afterSaleId) {
        jdbcTemplate.update("""
                UPDATE after_sale SET status = 'REFUNDED', refunded_at = now()
                WHERE id = ? AND status = 'RECEIVED'
                """, afterSaleId);
    }

    public UUID insertQuarantineBatch(UUID skuId, String locationType, UUID locationId,
                                      String batchNo, int quantity, String currency) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                             quantity, reserved_quantity, status, received_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, 'QUARANTINE', now(), now())
                """, id, skuId, locationType, locationId, batchNo, quantity);
        return id;
    }

    public void insertStockMove(UUID batchId, UUID skuId, String locationType, UUID locationId,
                                int quantity, UUID afterSaleId) {
        jdbcTemplate.update("""
                INSERT INTO stock_move (id, batch_id, sku_id, location_type, location_id,
                                        move_type, quantity, reference_type, reference_id, occurred_at)
                VALUES (?, ?, ?, ?, ?, 'RETURN_RECEIPT', ?, 'AFTER_SALE', ?, now())
                """, UUID.randomUUID(), batchId, skuId, locationType, locationId, quantity, afterSaleId);
    }

    private AfterSaleView withChildren(AfterSaleView view) {
        List<AfterSaleLineView> lines = jdbcTemplate.query("""
                SELECT l.id, l.order_line_id, l.sku_id, ol.title_snapshot, ol.sku_code_snapshot, l.quantity
                FROM after_sale_line l JOIN order_line ol ON ol.id = l.order_line_id
                WHERE l.after_sale_id = ? ORDER BY l.id
                """, (rs, rowNum) -> new AfterSaleLineView(
                rs.getObject("id", UUID.class), rs.getObject("order_line_id", UUID.class),
                rs.getObject("sku_id", UUID.class), rs.getString("title_snapshot"),
                rs.getString("sku_code_snapshot"), rs.getInt("quantity")), view.id());
        List<AfterSaleEventView> events = jdbcTemplate.query("""
                SELECT id, event_type, note, operator_name, created_at
                FROM after_sale_event WHERE after_sale_id = ? ORDER BY created_at
                """, (rs, rowNum) -> new AfterSaleEventView(
                rs.getObject("id", UUID.class), rs.getString("event_type"), rs.getString("note"),
                rs.getString("operator_name"), rs.getTimestamp("created_at").toInstant()), view.id());
        return new AfterSaleView(view.id(), view.afterSaleNo(), view.orderId(), view.orderNo(),
                view.type(), view.status(), view.reason(), view.description(), view.contactPhone(),
                view.refundAmount(), view.adminNote(), view.createdAt(), view.approvedAt(),
                view.receivedAt(), view.refundedAt(), view.rejectedAt(), lines, events);
    }

    private AfterSaleView map(ResultSet rs) throws SQLException {
        return new AfterSaleView(
                rs.getObject("id", UUID.class), rs.getString("after_sale_no"),
                rs.getObject("order_id", UUID.class), rs.getString("order_no"),
                rs.getString("type"), rs.getString("status"), rs.getString("reason"),
                rs.getString("description"), rs.getString("contact_phone"),
                new Money(rs.getLong("refund_amount"), rs.getString("currency")),
                rs.getString("admin_note"), instant(rs, "created_at"), instant(rs, "approved_at"),
                instant(rs, "received_at"), instant(rs, "refunded_at"), instant(rs, "rejected_at"),
                List.of(), List.of());
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}