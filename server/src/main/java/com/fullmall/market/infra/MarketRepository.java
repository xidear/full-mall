package com.fullmall.market.infra;

import com.fullmall.market.domain.Market;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 市场及其三轴关联表的读写。SQL 直写，避免为 5 张窄表引入 ORM 映射。 */
@Repository
public class MarketRepository {

    private static final String COLUMNS =
            "code, name, currency, country_code, tax_jurisdiction, price_includes_tax, timezone,"
                    + " legal_entity_name, active, sort";

    private final JdbcTemplate jdbcTemplate;

    public MarketRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Market map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Market(
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("currency"),
                rs.getString("country_code"),
                rs.getString("tax_jurisdiction"),
                rs.getObject("price_includes_tax", Boolean.class),
                rs.getString("timezone"),
                rs.getString("legal_entity_name"),
                rs.getBoolean("active"),
                rs.getInt("sort"));
    }

    public List<Market> findAll(boolean onlyActive) {
        String sql = "SELECT " + COLUMNS + " FROM market"
                + (onlyActive ? " WHERE active" : "") + " ORDER BY sort, code";
        return jdbcTemplate.query(sql, this::map);
    }

    public Optional<Market> findByCode(String code) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM market WHERE code = ?", this::map, code)
                .stream().findFirst();
    }

    public void upsert(Market market) {
        jdbcTemplate.update("""
                INSERT INTO market (code, name, currency, country_code, tax_jurisdiction,
                                    price_includes_tax, timezone, legal_entity_name, active, sort)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (code) DO UPDATE SET
                    name = EXCLUDED.name,
                    currency = EXCLUDED.currency,
                    country_code = EXCLUDED.country_code,
                    tax_jurisdiction = EXCLUDED.tax_jurisdiction,
                    price_includes_tax = EXCLUDED.price_includes_tax,
                    timezone = EXCLUDED.timezone,
                    legal_entity_name = EXCLUDED.legal_entity_name,
                    active = EXCLUDED.active,
                    sort = EXCLUDED.sort
                """, market.code(), market.name(), market.currency(), market.countryCode(),
                market.taxJurisdiction(), market.priceIncludesTax(), market.timezone(),
                market.legalEntityName(), market.active(), market.sort());
    }

    /** 语言候选（按主语言优先排序），界面语言只是候选之一，用户可自选。 */
    public List<String> locales(String marketCode) {
        return jdbcTemplate.queryForList("""
                SELECT locale FROM market_locale WHERE market_code = ?
                 ORDER BY is_primary DESC, locale
                """, String.class, marketCode);
    }

    /**
     * 整体替换语言候选。先删后插在同一事务里（调用方负责），
     * 避免"旧主语言还在 + 新主语言也设了"撞上部分唯一索引。
     */
    public void replaceLocales(String marketCode, List<String> locales, String primaryLocale) {
        jdbcTemplate.update("DELETE FROM market_locale WHERE market_code = ?", marketCode);
        for (String locale : locales) {
            jdbcTemplate.update("""
                    INSERT INTO market_locale (market_code, locale, is_primary) VALUES (?, ?, ?)
                    """, marketCode, locale, locale.equals(primaryLocale));
        }
    }

    /** 单条市场级设置；没有该行返回 null（调用方据此决定"回退全局"还是"缺配置"）。 */
    public String setting(String marketCode, String key) {
        List<String> rows = jdbcTemplate.queryForList(
                "SELECT setting_value FROM market_setting WHERE market_code = ? AND setting_key = ?",
                String.class, marketCode, key);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, String> settings(String marketCode) {
        Map<String, String> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT setting_key, setting_value FROM market_setting
                 WHERE market_code = ? ORDER BY setting_key
                """, rs -> {
                    result.put(rs.getString("setting_key"), rs.getString("setting_value"));
                }, marketCode);
        return result;
    }

    public void putSettings(String marketCode, Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            jdbcTemplate.update("""
                    INSERT INTO market_setting (market_code, setting_key, setting_value, updated_at)
                    VALUES (?, ?, ?, now())
                    ON CONFLICT (market_code, setting_key) DO UPDATE
                        SET setting_value = EXCLUDED.setting_value, updated_at = now()
                    """, marketCode, entry.getKey(), entry.getValue());
        }
    }
}
