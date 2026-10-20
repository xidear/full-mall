package com.fullmall.platform.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 审计快照保留清理。
 *
 * <p>JaVers 的 `jv_*` 表之间**没有数据库外键**（V24 只建了索引），但逻辑上仍必须按序删除：
 * `jv_snapshot` → `jv_commit_property` → `jv_commit` → 清理孤立 `jv_global_id`。
 *
 * <p>按 `jv_commit.commit_date`（真实时间戳）判断，不用 `commit_date_instant`（字符串，格式依赖 JaVers 版本）。
 */
@Service
public class AuditRetentionService {

    private final JdbcTemplate jdbcTemplate;

    public AuditRetentionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Purged(int commits, int snapshots, int globalIds) {
    }

    @Transactional
    public Purged purgeOlderThan(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("审计保留天数必须大于 0，实际=" + days);
        }

        // 先取过期 commit 的主键；`commit_fk` 是 bigint，不能与时间戳直接比较
        List<Long> commitPks = jdbcTemplate.queryForList(
                "SELECT commit_pk FROM jv_commit WHERE commit_date < now() - make_interval(days => ?)",
                Long.class, days);
        if (commitPks.isEmpty()) {
            return new Purged(0, 0, 0);
        }
        Long[] pks = commitPks.toArray(new Long[0]);

        int snapshots = jdbcTemplate.update(
                "DELETE FROM jv_snapshot WHERE commit_fk = ANY(?)", (Object) pks);
        int properties = jdbcTemplate.update(
                "DELETE FROM jv_commit_property WHERE commit_fk = ANY(?)", (Object) pks);
        int commits = jdbcTemplate.update(
                "DELETE FROM jv_commit WHERE commit_pk = ANY(?)", (Object) pks);
        int globalIds = jdbcTemplate.update("""
                DELETE FROM jv_global_id g
                 WHERE NOT EXISTS (SELECT 1 FROM jv_snapshot s WHERE s.global_id_fk = g.global_id_pk)
                   AND NOT EXISTS (SELECT 1 FROM jv_global_id child WHERE child.owner_id_fk = g.global_id_pk)
                """);

        return new Purged(commits, snapshots, globalIds);
    }
}
