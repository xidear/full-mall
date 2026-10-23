package com.fullmall.payment.reconcile;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 支付对账（A2）：把渠道账单与本地订单逐笔比对。
 *
 * <p>四类结果：MATCHED / BILL_ONLY（渠道有本地无，最危险）/ LOCAL_ONLY / AMOUNT_MISMATCH。
 * 按 (bill_date, order_no) 唯一，重复导入同日账单幂等覆盖。
 */
@Service
public class PaymentReconciliationService {

    private final JdbcTemplate jdbc;

    public PaymentReconciliationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Row(String orderNo, long amountFen) {
    }

    public record Summary(int matched, int billOnly, int localOnly, int amountMismatch) {
    }

    @Transactional
    public Summary reconcile(LocalDate billDate, List<Row> billRows) {
        if (billDate == null) {
            throw ApiException.badRequest("BILL_DATE_REQUIRED", "账单日期不能为空");
        }
        if (billRows == null || billRows.isEmpty()) {
            throw ApiException.badRequest("BILL_EMPTY", "账单没有数据行");
        }
        jdbc.update("DELETE FROM payment_reconciliation WHERE bill_date = ?", billDate);
        Map<String, Long> local = new LinkedHashMap<>();
        jdbc.query("SELECT order_no, total_amount FROM orders WHERE status = 'PAID'",
                rs -> {
                    local.put(rs.getString("order_no"), rs.getLong("total_amount"));
                });
        int matched = 0;
        int billOnly = 0;
        int amountMismatch = 0;
        List<String> seen = new ArrayList<>();
        for (Row row : billRows) {
            if (row.orderNo() == null || row.orderNo().isBlank()) {
                throw ApiException.badRequest("BILL_ROW_INVALID", "账单行缺少商户单号");
            }
            if (seen.contains(row.orderNo())) {
                throw ApiException.badRequest("BILL_DUPLICATED_ORDER",
                        "账单中商户单号重复：" + row.orderNo());
            }
            seen.add(row.orderNo());
            Long localFen = local.get(row.orderNo());
            String kind;
            if (localFen == null) {
                kind = "BILL_ONLY";
                billOnly++;
            } else if (localFen != row.amountFen()) {
                kind = "AMOUNT_MISMATCH";
                amountMismatch++;
            } else {
                kind = "MATCHED";
                matched++;
            }
            insert(billDate, row.orderNo(), kind, row.amountFen(), localFen);
        }
        int localOnly = 0;
        for (var entry : local.entrySet()) {
            if (!seen.contains(entry.getKey())) {
                insert(billDate, entry.getKey(), "LOCAL_ONLY", null, entry.getValue());
                localOnly++;
            }
        }
        return new Summary(matched, billOnly, localOnly, amountMismatch);
    }

    public List<Map<String, Object>> differences(LocalDate billDate) {
        return jdbc.query("""
                SELECT order_no, kind, bill_fen, local_fen, created_at
                  FROM payment_reconciliation
                 WHERE bill_date = ? AND kind <> 'MATCHED'
                 ORDER BY kind, order_no
                """, (rs, rowNum) -> Map.of(
                "orderNo", rs.getString("order_no"),
                "kind", rs.getString("kind"),
                "billFen", rs.getObject("bill_fen") == null ? 0L : rs.getLong("bill_fen"),
                "localFen", rs.getObject("local_fen") == null ? 0L : rs.getLong("local_fen"),
                "createdAt", rs.getTimestamp("created_at").toInstant().toString()), billDate);
    }

    private void insert(LocalDate billDate, String orderNo, String kind,
                        Long billFen, Long localFen) {
        jdbc.update("""
                INSERT INTO payment_reconciliation (id, bill_date, order_no, kind, bill_fen, local_fen)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), billDate, orderNo, kind, billFen, localFen);
    }

    public static long yuanToFen(String yuan) {
        try {
            return new BigDecimal(yuan.trim()).movePointRight(2).longValueExact();
        } catch (RuntimeException error) {
            throw ApiException.badRequest("BILL_AMOUNT_INVALID", "账单金额非法：" + yuan);
        }
    }
}
