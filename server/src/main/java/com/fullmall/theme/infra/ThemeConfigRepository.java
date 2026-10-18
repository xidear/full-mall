package com.fullmall.theme.infra;

import com.fullmall.theme.domain.ThemeConfigRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ThemeConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    public ThemeConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ThemeConfigRecord find(String channel, String market) {
        List<ThemeConfigRecord> records = jdbcTemplate.query("""
                SELECT id, channel, market_code, config_json, version, updated_at
                FROM theme_config
                WHERE channel = ? AND market_code = ?
                """, (rs, rowNum) -> new ThemeConfigRecord(
                rs.getObject("id", UUID.class),
                rs.getString("channel"),
                rs.getString("market_code"),
                rs.getString("config_json"),
                rs.getInt("version"),
                rs.getTimestamp("updated_at").toInstant()), channel, market);
        return records.stream().findFirst().orElse(null);
    }

    public ThemeConfigRecord upsert(String channel, String market, String configJson) {
        jdbcTemplate.update("""
                INSERT INTO theme_config (id, channel, market_code, config_json, version, updated_at)
                VALUES (?, ?, ?, ?::jsonb, 1, now())
                ON CONFLICT (channel, market_code)
                DO UPDATE SET config_json = EXCLUDED.config_json,
                              version = theme_config.version + 1,
                              updated_at = now()
                """, UUID.randomUUID(), channel, market, configJson);
        ThemeConfigRecord saved = find(channel, market);
        if (saved == null) {
            throw new IllegalStateException("Theme config was not saved for " + channel + "/" + market);
        }
        return saved;
    }
}