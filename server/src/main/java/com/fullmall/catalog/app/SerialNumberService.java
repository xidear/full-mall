package com.fullmall.catalog.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 单件序列号管理（IMEI/唯一码）。
 *
 * <p>规则：
 * <ul>
 *   <li>序列号**全局唯一**（跨批次也不能重复）；</li>
 *   <li>登记时必须落在**已存在的批次**上，且同一批次登记数量不超过批次现存数量；</li>
 *   <li>重复序列号**明确报错**（不静默跳过——重复往往意味着贴错标签）；</li>
 *   <li>支持按序列号查"在哪、属于哪批、卖没卖"。</li>
 * </ul>
 */
@Service
public class SerialNumberService {

    private final JdbcTemplate jdbc;

    public SerialNumberService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Registered(int created) {
    }

    @Transactional
    public Registered register(UUID batchId, List<String> serialNumbers) {
        if (batchId == null) {
            throw ApiException.badRequest("SERIAL_BATCH_REQUIRED", "必须指定批次");
        }
        if (serialNumbers == null || serialNumbers.isEmpty()) {
            throw ApiException.badRequest("SERIAL_LIST_EMPTY", "序列号列表不能为空");
        }
        var batch = jdbc.query("""
                SELECT sku_id, quantity FROM inventory_batch WHERE id = ?
                """, rs -> rs.next()
                ? Map.of("skuId", rs.getObject("sku_id", UUID.class),
                         "quantity", rs.getInt("quantity"))
                : null, batchId);
        if (batch == null) {
            throw ApiException.notFound("BATCH_NOT_FOUND", "批次不存在：" + batchId);
        }
        UUID skuId = (UUID) batch.get("skuId");
        int batchQuantity = (Integer) batch.get("quantity");

        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sku_serial WHERE batch_id = ?", Integer.class, batchId);
        int already = existing == null ? 0 : existing;
        List<String> cleaned = new ArrayList<>();
        for (String raw : serialNumbers) {
            String serial = raw == null ? "" : raw.trim();
            if (serial.isEmpty()) {
                throw ApiException.badRequest("SERIAL_EMPTY", "序列号不能为空");
            }
            if (cleaned.contains(serial.toUpperCase(java.util.Locale.ROOT))) {
                throw ApiException.badRequest("SERIAL_DUPLICATED_IN_REQUEST",
                        "提交的序列号有重复：" + serial);
            }
            cleaned.add(serial.toUpperCase(java.util.Locale.ROOT));
        }
        if (already + cleaned.size() > batchQuantity) {
            throw ApiException.conflict("SERIAL_EXCEEDS_BATCH",
                    "该批次现有 " + batchQuantity + " 件，已登记 " + already
                            + " 个序列号，本次再登记 " + cleaned.size() + " 个会超出");
        }
        int created = 0;
        for (String serial : cleaned) {
            try {
                created += jdbc.update("""
                        INSERT INTO sku_serial (id, sku_id, batch_id, serial_no)
                        VALUES (?, ?, ?, ?)
                        """, UUID.randomUUID(), skuId, batchId, serial);
            } catch (org.springframework.dao.DuplicateKeyException duplicate) {
                throw ApiException.conflict("SERIAL_ALREADY_EXISTS",
                        "序列号已存在（可能已登记到其它批次）：" + serial);
            }
        }
        return new Registered(created);
    }

    /** 按序列号查：在哪批、什么状态、卖给了哪张订单行。 */
    public Map<String, Object> lookup(String serialNo) {
        if (serialNo == null || serialNo.isBlank()) {
            throw ApiException.badRequest("SERIAL_REQUIRED", "序列号不能为空");
        }
        var rows = jdbc.query("""
                SELECT s.serial_no, s.status, s.batch_id, s.order_line_id, s.created_at,
                       b.batch_no, b.sku_id, k.sku_code
                  FROM sku_serial s
                  JOIN inventory_batch b ON b.id = s.batch_id
                  JOIN sku k ON k.id = s.sku_id
                 WHERE s.serial_no = ?
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("serialNo", rs.getString("serial_no"));
            row.put("status", rs.getString("status"));
            row.put("batchId", rs.getObject("batch_id", UUID.class));
            row.put("batchNo", rs.getString("batch_no"));
            row.put("skuId", rs.getObject("sku_id", UUID.class));
            row.put("skuCode", rs.getString("sku_code"));
            row.put("orderLineId", rs.getObject("order_line_id", UUID.class));
            row.put("createdAt", rs.getTimestamp("created_at").toInstant());
            return row;
        }, serialNo.trim().toUpperCase(java.util.Locale.ROOT));
        if (rows.isEmpty()) {
            throw ApiException.notFound("SERIAL_NOT_FOUND", "序列号不存在：" + serialNo);
        }
        return rows.get(0);
    }
}
