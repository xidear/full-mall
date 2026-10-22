package com.fullmall.inventory.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 临期预警：找出 N 天内到期的批次并输出告警（供门店/仓库处理）。
 *
 * <p>只读、不改库存状态——过期拦截已由拣货条件兜住（`expiry_date >= today`），
 * 这里的作用是**提前提醒**，让人去促销或下架。
 */
@Service
public class ExpiryAlertService {

    private static final Logger LOG = LoggerFactory.getLogger(ExpiryAlertService.class);

    private final JdbcTemplate jdbc;
    private final int warnDays;
    private final com.fullmall.notification.app.NotificationService notifications;
    private final boolean notifyEnabled;

    public ExpiryAlertService(JdbcTemplate jdbc,
                              @Value("${app.inventory.expiry-warn-days:30}") int warnDays,
                              com.fullmall.notification.app.NotificationService notifications,
                              @Value("${app.inventory.expiry-notify:true}") boolean notifyEnabled) {
        if (warnDays <= 0) {
            throw new IllegalStateException("app.inventory.expiry-warn-days 必须大于 0，实际=" + warnDays);
        }
        this.jdbc = jdbc;
        this.warnDays = warnDays;
        this.notifications = notifications;
        this.notifyEnabled = notifyEnabled;
    }

    /** 临期批次清单（含已过期，`daysLeft` 为负）。 */
    public List<Map<String, Object>> expiringBatches(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        return jdbc.query("""
                SELECT b.id, b.batch_no, b.location_type, b.location_id, b.quantity, b.expiry_date,
                       s.sku_code
                  FROM inventory_batch b
                  JOIN sku s ON s.id = b.sku_id
                 WHERE b.expiry_date IS NOT NULL
                   AND b.quantity > 0
                   AND b.status IN ('AVAILABLE', 'NEAR_EXPIRY')
                   AND b.expiry_date <= CURRENT_DATE + ?::int
                 ORDER BY b.expiry_date ASC
                 LIMIT ?
                """, (rs, rowNum) -> {
            LocalDate expiry = rs.getDate("expiry_date").toLocalDate();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("batchId", rs.getObject("id", java.util.UUID.class));
            row.put("batchNo", rs.getString("batch_no"));
            row.put("skuCode", rs.getString("sku_code"));
            row.put("locationType", rs.getString("location_type"));
            row.put("locationId", rs.getObject("location_id", java.util.UUID.class));
            row.put("quantity", rs.getInt("quantity"));
            row.put("expiryDate", expiry.toString());
            row.put("daysLeft", java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiry));
            return row;
        }, warnDays, safeLimit);
    }

    /** 每日巡检：有临期就告警。 */
    @Scheduled(cron = "${app.inventory.expiry-cron:0 30 7 * * *}")
    public void dailyCheck() {
        List<Map<String, Object>> rows = expiringBatches(500);
        if (rows.isEmpty()) {
            return;
        }
        long expired = rows.stream().filter(row -> ((Number) row.get("daysLeft")).longValue() < 0).count();
        LOG.warn("临期预警：{} 个批次将在 {} 天内到期（其中已过期 {} 个），请及时促销或下架",
                rows.size(), warnDays, expired);
        if (notifyEnabled) {
            // 没有门店上下文时先推「运营」角色：不知道归属就广播，比不提醒更安全
            notifications.notifyStaff("INVENTORY_EXPIRY",
                    "临期预警：" + rows.size() + " 个批次即将到期",
                    "未来 " + warnDays + " 天内到期 " + rows.size()
                            + " 个批次，其中已过期 " + expired + " 个，请到「临期商品」页处理。");
        }
    }
}
