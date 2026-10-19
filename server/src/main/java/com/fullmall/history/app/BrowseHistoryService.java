package com.fullmall.history.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 浏览足迹：同一会员同一 SKU 只保留一行，重复浏览**更新时间并累加次数**，
 * 这样"最近浏览"不会因为反复点同一商品而刷屏。
 */
@Service
public class BrowseHistoryService {

    private final JdbcTemplate jdbcTemplate;

    public BrowseHistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Entry(UUID skuId, Instant viewedAt, int viewCount) {
    }

    @Transactional
    public void record(UUID memberId, UUID skuId) {
        if (skuId == null) {
            throw ApiException.badRequest("SKU_REQUIRED", "必须指定商品");
        }
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sku WHERE id = ?", Integer.class, skuId);
        if (exists == null || exists == 0) {
            throw ApiException.notFound("SKU_NOT_FOUND", "商品不存在: " + skuId);
        }
        jdbcTemplate.update("""
                INSERT INTO member_browse_history (member_id, sku_id) VALUES (?, ?)
                ON CONFLICT (member_id, sku_id) DO UPDATE
                   SET viewed_at = now(), view_count = member_browse_history.view_count + 1
                """, memberId, skuId);
    }

    public List<Entry> recent(UUID memberId, int limit) {
        if (limit <= 0 || limit > 100) {
            throw ApiException.badRequest("INVALID_LIMIT", "查询条数必须在 1..100，实际=" + limit);
        }
        return jdbcTemplate.query("""
                SELECT sku_id, viewed_at, view_count FROM member_browse_history
                 WHERE member_id = ? ORDER BY viewed_at DESC LIMIT ?
                """, (rs, rowNum) -> new Entry(
                rs.getObject("sku_id", UUID.class), rs.getTimestamp("viewed_at").toInstant(),
                rs.getInt("view_count")), memberId, limit);
    }

    @Transactional
    public int clear(UUID memberId) {
        return jdbcTemplate.update("DELETE FROM member_browse_history WHERE member_id = ?", memberId);
    }
}
