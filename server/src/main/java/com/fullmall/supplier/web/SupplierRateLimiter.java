package com.fullmall.supplier.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * 供应商限流（SUP-033）：按 access key 的固定窗口计数，超限返回 429 + `Retry-After`。
 *
 * <p>为什么放数据库：内存计数在多副本下各自为政，实际额度会变成 N 倍。
 * 这里用 `INSERT ... ON CONFLICT DO UPDATE ... RETURNING` 做原子自增，一次往返拿到当前计数。
 *
 * <p>`maxRequests <= 0` 表示**显式关闭**限流（配置项写死，不是静默跳过）。
 */
@Component
public class SupplierRateLimiter {

    private final JdbcTemplate jdbcTemplate;

    public SupplierRateLimiter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Decision(boolean allowed, int currentCount, int maxRequests, long retryAfterSeconds) {
    }

    public Decision check(String accessKey, int maxRequests, Duration window, Instant now) {
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalArgumentException("accessKey 不能为空");
        }
        if (window == null || window.toSeconds() <= 0) {
            throw new IllegalArgumentException("限流窗口必须大于 0 秒，实际=" + window);
        }
        if (maxRequests <= 0) {
            return new Decision(true, 0, maxRequests, 0);
        }

        long windowSeconds = window.toSeconds();
        long bucket = now.getEpochSecond() / windowSeconds;
        Instant windowStart = Instant.ofEpochSecond(bucket * windowSeconds);

        Integer count = jdbcTemplate.queryForObject("""
                INSERT INTO supplier_rate_window (access_key, window_start, request_count)
                VALUES (?, ?, 1)
                ON CONFLICT (access_key, window_start)
                DO UPDATE SET request_count = supplier_rate_window.request_count + 1
                RETURNING request_count
                """, Integer.class, accessKey, Timestamp.from(windowStart));
        if (count == null) {
            throw new IllegalStateException("限流计数返回 null: accessKey=" + accessKey);
        }

        if (count == 1) {
            // 新窗口第一次请求时清理历史窗口，避免表无限增长；失败不影响当前请求。
            jdbcTemplate.update("DELETE FROM supplier_rate_window WHERE window_start < ?",
                    Timestamp.from(windowStart.minus(window.multipliedBy(10))));
        }

        long retryAfter = Math.max(1, windowStart.plus(window).getEpochSecond() - now.getEpochSecond());
        return new Decision(count <= maxRequests, count, maxRequests, count <= maxRequests ? 0 : retryAfter);
    }
}