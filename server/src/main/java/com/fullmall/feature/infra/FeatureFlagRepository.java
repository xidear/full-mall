package com.fullmall.feature.infra;

import com.fullmall.feature.domain.FeatureFlag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FeatureFlagRepository {

    private final JdbcTemplate jdbcTemplate;

    public FeatureFlagRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, FeatureFlag> findAll() {
        List<FeatureFlag> rows = jdbcTemplate.query("""
                SELECT code, enabled, updated_by, updated_at, reason
                  FROM feature_flag
                 ORDER BY code
                """, (rs, rowNum) -> new FeatureFlag(
                rs.getString("code"),
                rs.getBoolean("enabled"),
                rs.getString("updated_by"),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getString("reason")));
        Map<String, FeatureFlag> result = new LinkedHashMap<>();
        rows.forEach(flag -> result.put(flag.code(), flag));
        return result;
    }

    /**
     * @return 影响行数；调用方必须校验为 1，否则说明开关编码不存在。
     */
    public int update(String code, boolean enabled, String updatedBy, String reason, Instant updatedAt) {
        return jdbcTemplate.update("""
                UPDATE feature_flag
                   SET enabled = ?, updated_by = ?, updated_at = ?, reason = ?
                 WHERE code = ?
                """, enabled, updatedBy, Timestamp.from(updatedAt), reason, code);
    }
}