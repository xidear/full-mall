package com.fullmall.supplier.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 供应商售后（商城 → 供应商）。
 *
 * <p>**与商城售后单是两回事**：商城售后单面向顾客（退货/换货/退款），
 * 本表面向供应商（退货折让、扣款），流程与状态机独立，不能共用同一状态字段。
 */
@Service
public class SupplierReturnService {

    private static final List<String> STATUSES =
            List.of("REQUESTED", "ACCEPTED", "REJECTED", "SETTLED");

    public record SupplierReturn(UUID id, UUID supplierId, UUID orderId, String reason,
                                 long amount, String currency, String status, String note,
                                 String createdBy, Instant createdAt, Instant closedAt) {
    }

    private final JdbcTemplate jdbcTemplate;

    public SupplierReturnService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UUID create(UUID supplierId, UUID orderId, String reason, long amount,
                       String currency, String createdBy) {
        if (supplierId == null || orderId == null) {
            throw ApiException.badRequest("SUPPLIER_RETURN_REQUIRED", "供应商与订单都不能为空");
        }
        if (reason == null || reason.isBlank()) {
            throw ApiException.badRequest("SUPPLIER_RETURN_REASON_REQUIRED", "必须填写退货原因");
        }
        if (amount < 0) {
            throw ApiException.badRequest("SUPPLIER_RETURN_AMOUNT_INVALID", "金额不能为负：" + amount);
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw ApiException.badRequest("OPERATOR_REQUIRED", "必须记录操作人");
        }
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO supplier_return (id, supplier_id, order_id, reason, amount_amount,
                                             currency, status, created_by)
                VALUES (?, ?, ?, ?, ?, ?, 'REQUESTED', ?)
                """, id, supplierId, orderId, reason.trim(), amount,
                currency == null || currency.isBlank() ? "CNY" : currency.trim().toUpperCase(Locale.ROOT),
                createdBy);
        return id;
    }

    /** 状态推进：只允许 REQUESTED → ACCEPTED/REJECTED，ACCEPTED → SETTLED。 */
    @Transactional
    public void transition(UUID id, String target, String note) {
        String normalized = target == null ? "" : target.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw ApiException.badRequest("SUPPLIER_RETURN_STATUS_INVALID",
                    "状态只能是 " + STATUSES + "，实际=" + target);
        }
        String current = statusOf(id);
        boolean allowed = switch (current) {
            case "REQUESTED" -> normalized.equals("ACCEPTED") || normalized.equals("REJECTED");
            case "ACCEPTED" -> normalized.equals("SETTLED");
            default -> false;
        };
        if (!allowed) {
            throw ApiException.conflict("SUPPLIER_RETURN_TRANSITION_INVALID",
                    "不允许从 " + current + " 变为 " + normalized);
        }
        boolean closing = normalized.equals("REJECTED") || normalized.equals("SETTLED");
        jdbcTemplate.update("""
                UPDATE supplier_return
                   SET status = ?, note = ?,
                       closed_at = CASE WHEN ? THEN now() ELSE closed_at END
                 WHERE id = ?
                """, normalized, note == null ? null : note.trim(), closing, id);
    }

    /** 供应商侧表态：必须属于该供应商，且只能 ACCEPTED/REJECTED。 */
    @Transactional
    public void transitionForSupplier(UUID supplierId, UUID returnId, String target, String note) {
        List<UUID> owners = jdbcTemplate.queryForList(
                "SELECT supplier_id FROM supplier_return WHERE id = ?", UUID.class, returnId);
        if (owners.isEmpty()) {
            throw ApiException.notFound("SUPPLIER_RETURN_NOT_FOUND", "供应商售后单不存在: " + returnId);
        }
        if (!owners.get(0).equals(supplierId)) {
            // 不泄露存在性：对他人的单据返回"不存在"
            throw ApiException.notFound("SUPPLIER_RETURN_NOT_FOUND", "供应商售后单不存在: " + returnId);
        }
        transition(returnId, target, note);
    }

    /**
     * 结算扣减：该供应商在周期内**已结算（SETTLED）**的退货金额合计。
     *
     * <p>按 `closed_at` 落在周期内计算；未结算的单不扣，避免"还没谈定就先扣钱"。
     */
    public long settledAmount(UUID supplierId, java.time.Instant periodStart,
                              java.time.Instant periodEnd, String currency) {
        Long value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_amount), 0) FROM supplier_return
                 WHERE supplier_id = ? AND status = 'SETTLED'
                   AND currency = ?
                   AND closed_at >= ? AND closed_at < ?
                """, Long.class, supplierId, currency,
                java.sql.Timestamp.from(periodStart), java.sql.Timestamp.from(periodEnd));
        return value == null ? 0 : value;
    }

    public Map<String, Object> list(UUID supplierId, String status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * safeSize;
        String filter = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        if (filter != null && !STATUSES.contains(filter)) {
            throw ApiException.badRequest("SUPPLIER_RETURN_STATUS_INVALID", "未知状态：" + status);
        }
        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM supplier_return
                 WHERE (?::uuid IS NULL OR supplier_id = ?::uuid)
                   AND (?::text IS NULL OR status = ?::text)
                """, Long.class, supplierId, supplierId, filter, filter);
        var items = jdbcTemplate.query("""
                SELECT id, supplier_id, order_id, reason, amount_amount, currency, status, note,
                       created_by, created_at, closed_at
                  FROM supplier_return
                 WHERE (?::uuid IS NULL OR supplier_id = ?::uuid)
                   AND (?::text IS NULL OR status = ?::text)
                 ORDER BY created_at DESC
                 LIMIT ? OFFSET ?
                """, (rs, rowNum) -> new SupplierReturn(
                rs.getObject("id", UUID.class), rs.getObject("supplier_id", UUID.class),
                rs.getObject("order_id", UUID.class), rs.getString("reason"),
                rs.getLong("amount_amount"), rs.getString("currency"), rs.getString("status"),
                rs.getString("note"), rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("closed_at") == null ? null : rs.getTimestamp("closed_at").toInstant()),
                supplierId, supplierId, filter, filter, safeSize, offset);
        return Map.of("items", items, "page", Math.max(page, 0), "size", safeSize,
                "total", total == null ? 0L : total);
    }

    private String statusOf(UUID id) {
        List<String> rows = jdbcTemplate.queryForList(
                "SELECT status FROM supplier_return WHERE id = ?", String.class, id);
        if (rows.isEmpty()) {
            throw ApiException.notFound("SUPPLIER_RETURN_NOT_FOUND", "供应商售后单不存在: " + id);
        }
        return rows.get(0);
    }

    /**
     * 供应商退货对账：按供应商汇总周期内**已同意**的退货金额。
     *
     * <p>用途：结算时扣减的金额要和这里对得上；运营核对差异时先看这张表。
     */
    public java.util.List<java.util.Map<String, Object>> reconciliation(int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        return jdbcTemplate.query("""
                SELECT r.supplier_id, s.name AS supplier_name, r.currency,
                       COUNT(*) AS accepted_count,
                       COALESCE(SUM(r.amount_amount), 0) AS accepted_amount
                  FROM supplier_return r
                  LEFT JOIN supplier s ON s.id = r.supplier_id
                 WHERE r.status = 'ACCEPTED'
                   AND r.created_at >= now() - make_interval(days => ?)
                 GROUP BY r.supplier_id, s.name, r.currency
                 ORDER BY accepted_amount DESC
                """, (rs, rowNum) -> {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("supplierId", rs.getObject("supplier_id", UUID.class));
            row.put("supplierName", rs.getString("supplier_name"));
            row.put("currency", rs.getString("currency"));
            row.put("acceptedCount", rs.getInt("accepted_count"));
            row.put("acceptedAmount", rs.getLong("accepted_amount"));
            return row;
        }, safeDays);
    }

    /**
     * 对账审计：**已同意退货金额** vs **结算单已扣减金额**，返回差额。
     *
     * <p>为什么需要：扣减发生在生成结算单时，如果运营漏生成或周期不重叠，
     * 扣减就会缺失；靠人眼对两张表必然漏。差额非 0 时页面必须显式告警。
     */
    public java.util.Map<String, Object> reconciliationAudit(int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        Long accepted = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_amount), 0) FROM supplier_return
                 WHERE status = 'ACCEPTED'
                   AND created_at >= now() - make_interval(days => ?)
                """, Long.class, safeDays);
        Long deducted = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount), 0) FROM supplier_settlement
                 WHERE created_at >= now() - make_interval(days => ?)
                """, Long.class, safeDays);
        long acceptedAmount = accepted == null ? 0 : accepted;
        long deductedAmount = deducted == null ? 0 : deducted;
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("days", safeDays);
        result.put("acceptedReturnAmount", acceptedAmount);
        result.put("settledDeductionAmount", deductedAmount);
        result.put("difference", acceptedAmount - deductedAmount);
        result.put("consistent", acceptedAmount == deductedAmount);
        if (acceptedAmount != deductedAmount) {
            org.slf4j.LoggerFactory.getLogger(SupplierReturnService.class).warn(
                    "供应商退货对账差额：已同意 {}，已扣减 {}，差额 {}（近 {} 天）",
                    acceptedAmount, deductedAmount, acceptedAmount - deductedAmount, safeDays);
        }
        return result;
    }

    /** 待处理退货数（供应商待办用）。 */
    public int pendingCount(UUID supplierId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM supplier_return
                 WHERE supplier_id = ? AND status = 'REQUESTED'
                """, Integer.class, supplierId);
        return count == null ? 0 : count;
    }

    /** Keyset 分页：按 (created_at, id) 倒序，保留供应商与状态筛选。 */
    public Map<String, Object> listKeyset(UUID supplierId, String status,
                                          String cursorCreatedAt, UUID cursorId, int size) {
        var cursor = com.fullmall.platform.paging.KeysetPage.Cursor.parse(cursorCreatedAt, cursorId);
        int safeSize = com.fullmall.platform.paging.KeysetPage.normalizeSize(size);
        String filter = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        if (filter != null && !STATUSES.contains(filter)) {
            throw ApiException.badRequest("SUPPLIER_RETURN_STATUS_INVALID", "未知状态：" + status);
        }
        var items = jdbcTemplate.query("""
                SELECT id, supplier_id, order_id, reason, amount_amount, currency, status, note,
                       created_by, created_at, closed_at
                  FROM supplier_return
                 WHERE (?::uuid IS NULL OR supplier_id = ?::uuid)
                   AND (?::text IS NULL OR status = ?::text)
                   AND (?::timestamptz IS NULL
                        OR (created_at, id) < (?::timestamptz, ?::uuid))
                 ORDER BY created_at DESC, id DESC
                 LIMIT ?
                """, (rs, rowNum) -> new SupplierReturn(
                rs.getObject("id", UUID.class), rs.getObject("supplier_id", UUID.class),
                rs.getObject("order_id", UUID.class), rs.getString("reason"),
                rs.getLong("amount_amount"), rs.getString("currency"), rs.getString("status"),
                rs.getString("note"), rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("closed_at") == null ? null : rs.getTimestamp("closed_at").toInstant()),
                supplierId, supplierId, filter, filter,
                cursor.createdAt() == null ? null : java.sql.Timestamp.from(cursor.createdAt()),
                cursor.createdAt() == null ? null : java.sql.Timestamp.from(cursor.createdAt()),
                cursor.id(), safeSize);
        return com.fullmall.platform.paging.KeysetPage.toResponse(
                items, safeSize, SupplierReturn::createdAt, SupplierReturn::id);
    }
}
