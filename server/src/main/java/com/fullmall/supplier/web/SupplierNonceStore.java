package com.fullmall.supplier.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * 供应商 nonce 防重放（SUP-032）。
 *
 * <p>为什么落库：内存 Map 在**重启后清空**、且**多副本之间不共享**，重放窗口内重启一次即可重放成功。
 * 这里用 `(access_key, nonce)` 主键做原子去重：插入成功=首次，冲突=重放。
 *
 * <p>过期清理：每次成功插入顺带删除超出 TTL 的记录（轻量、无需额外调度）。
 * 单条 DELETE 命中索引，规模大后可改为定时任务 + 分区，已记录在实施文档。
 */
@Component
public class SupplierNonceStore {

    private final JdbcTemplate jdbcTemplate;

    public SupplierNonceStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @return true 表示首次出现（放行），false 表示重放（拒绝）。
     */
    public boolean tryRegister(String accessKey, String nonce, Instant now, long ttlSeconds) {
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalArgumentException("accessKey 不能为空");
        }
        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException("nonce 不能为空");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("nonce TTL 必须大于 0，实际=" + ttlSeconds);
        }

        int inserted = jdbcTemplate.update("""
                INSERT INTO supplier_nonce (access_key, nonce, created_at)
                VALUES (?, ?, ?)
                ON CONFLICT (access_key, nonce) DO NOTHING
                """, accessKey, nonce, Timestamp.from(now));
        if (inserted == 0) {
            return false;
        }

        jdbcTemplate.update("DELETE FROM supplier_nonce WHERE created_at < ?",
                Timestamp.from(now.minusSeconds(ttlSeconds)));
        return true;
    }
}