package com.fullmall.setting.infra;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SystemSettingRepository {

    private final JdbcTemplate jdbcTemplate;

    public SystemSettingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, String> findAll() {
        List<Map.Entry<String, String>> rows = jdbcTemplate.query("""
                SELECT setting_key, setting_value FROM system_setting ORDER BY setting_key
                """, (rs, rowNum) -> Map.entry(rs.getString("setting_key"), rs.getString("setting_value")));
        Map<String, String> result = new LinkedHashMap<>();
        rows.forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    public void upsert(String key, String value) {
        jdbcTemplate.update("""
                INSERT INTO system_setting (setting_key, setting_value, updated_at)
                VALUES (?, ?, now())
                ON CONFLICT (setting_key)
                DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = now()
                """, key, value);
    }
}