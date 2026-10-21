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

    /** 记录换货发货信息并推进状态。 */
    public void markExchangeShipped(UUID afterSaleId, String carrierCode, String trackingNo,
                                   int autoCompleteDays) {
        int updated = jdbcTemplate.update("""
                UPDATE after_sale
                   SET exchange_carrier_code = ?, exchange_tracking_no = ?,
                       exchange_shipped_at = now(),
                       auto_complete_at = now() + make_interval(days => ?),
                       status = 'EXCHANGE_SHIPPED'
                 WHERE id = ? AND status = 'RECEIVED'
                """, carrierCode, trackingNo, autoCompleteDays, afterSaleId);
        if (updated != 1) {
            throw new IllegalStateException("换货发货状态更新失败：" + afterSaleId);
        }
    }

    /** 到期自动完成：仅处理"已发货、超期、顾客未报未收到"的单据。 */
    public java.util.List<UUID> findAutoCompletable(int limit) {
        return jdbcTemplate.query("""
                SELECT id FROM after_sale
                 WHERE status = 'EXCHANGE_SHIPPED'
                   AND auto_complete_at IS NOT NULL
                   AND auto_complete_at <= now()
                   AND disputed_at IS NULL
                 ORDER BY auto_complete_at
                 LIMIT ?
                """, (rs, rowNum) -> rs.getObject("id", UUID.class), limit);
    }

    /** 顾客报"未收到"：阻止自动完成，转人工处理。 */
    public void markDisputed(UUID afterSaleId, String note) {
        int updated = jdbcTemplate.update("""
                UPDATE after_sale SET disputed_at = now(), admin_note = COALESCE(admin_note, ?)
                 WHERE id = ? AND status = 'EXCHANGE_SHIPPED'
                """, note, afterSaleId);
        if (updated != 1) {
            throw new IllegalStateException("只有换货已发货的售后单可以申报未收到：" + afterSaleId);
        }
    }

    public void completeExchangeAuto(UUID afterSaleId) {
        jdbcTemplate.update("""
                UPDATE after_sale SET status = 'COMPLETED', auto_completed = TRUE
                 WHERE id = ? AND status = 'EXCHANGE_SHIPPED' AND disputed_at IS NULL
                """, afterSaleId);
    }

    /** 标记换货完成（只有已发货的换货单可完成）。 */
    public void completeExchange(UUID afterSaleId, String operator) {
        int updated = jdbcTemplate.update("""
                UPDATE after_sale SET status = 'COMPLETED'
                 WHERE id = ? AND status = 'EXCHANGE_SHIPPED'
                """, afterSaleId);
        if (updated != 1) {
            throw new IllegalStateException("换货完成状态更新失败：" + afterSaleId);
        }
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
                       a.created_at, a.approved_at, a.received_at, a.refunded_at, a.rejected_at,
                       a.exchange_carrier_code, a.exchange_tracking_no, a.exchange_shipped_at
                FROM after_sale a JOIN orders o ON o.id = a.order_id
                WHERE a.id = ?
                """, (rs, rowNum) -> map(rs), afterSaleId);
        return rows.isEmpty() ? null : withChildren(rows.get(0));
    }

    /** 按状态计数（分页用）。 */
    public long countByStatus(String status) {
        String filter = status == null ? "" : status.trim().toUpperCase();
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM after_sale a
                 WHERE (?::text = '' OR a.status = ?::text)
                """, Long.class, filter, filter);
        return count == null ? 0 : count;
    }

    public List<AfterSaleView> list(String status, int limit) {
        return list(status, 0, limit);
    }

    /** 分页列表（offset 由 page × size 计算）。 */
    /** 排序白名单：默认按申请时间倒序。 */
    private static String afterSaleOrderBy(String sort, String direction) {
        String column = switch (sort == null ? "" : sort.trim()) {
            case "refundAmount" -> "a.refund_amount";
            case "status" -> "a.status";
            default -> "a.created_at";
        };
        String dir = "asc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "ASC" : "DESC";
        return column + " " + dir;
    }

    public List<AfterSaleView> list(String status, int page, int size) {
        return list(status, page, size, null, null);
    }

    public List<AfterSaleView> list(String status, int page, int size, String sort, String direction) {
        String filter = status == null ? "" : status.trim().toUpperCase();
        return jdbcTemplate.query("""
                SELECT a.id, a.after_sale_no, a.order_id, o.order_no, a.type, a.status, a.reason,
                       a.description, a.contact_phone, a.refund_amount, a.currency, a.admin_note,
                       a.created_at, a.approved_at, a.received_at, a.refunded_at, a.rejected_at,
                       a.exchange_carrier_code, a.exchange_tracking_no, a.exchange_shipped_at
                FROM after_sale a JOIN orders o ON o.id = a.order_id
                WHERE (? = '' OR a.status = ?)
                ORDER BY
                """ + afterSaleOrderBy(sort, direction) + """
                 LIMIT ? OFFSET ?
                """, (rs, rowNum) -> withChildren(map(rs)), filter, filter, size,
                Math.max(page, 0) * Math.max(size, 1));
    }

    public List<AfterSaleView> listByOrder(UUID orderId, String phone) {
        return jdbcTemplate.query("""
                SELECT a.id, a.after_sale_no, a.order_id, o.order_no, a.type, a.status, a.reason,
                       a.description, a.contact_phone, a.refund_amount, a.currency, a.admin_note,
                       a.created_at, a.approved_at, a.received_at, a.refunded_at, a.rejected_at,
                       a.exchange_carrier_code, a.exchange_tracking_no, a.exchange_shipped_at
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

    /** 订单行记录的原出库批次；为空说明拣货时未回填（多行合并）。 */
    public UUID findOrderLineBatch(UUID orderLineId) {
        var rows = jdbcTemplate.query("SELECT batch_id FROM order_line WHERE id = ?",
                (rs, rowNum) -> rs.getObject("batch_id", UUID.class), orderLineId);
        return rows.isEmpty() ? null : rows.get(0);
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
                view.receivedAt(), view.refundedAt(), view.rejectedAt(),
                view.exchangeCarrierCode(), view.exchangeTrackingNo(), view.exchangeShippedAt(),
                lines, events);
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
                rs.getString("exchange_carrier_code"), rs.getString("exchange_tracking_no"),
                instant(rs, "exchange_shipped_at"),
                List.of(), List.of());
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}