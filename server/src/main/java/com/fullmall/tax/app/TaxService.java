package com.fullmall.tax.app;

import com.fullmall.platform.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 计税（ADR-0014 §决定矩阵：Destination 计税、Market 定申报主体与税率表）。
 *
 * <p>查表顺序：**目的地国家**的规则优先（目的地税，如 EU OSS / US ship-to nexus），
 * 没有再回退到**市场的税务辖区**（申报主体的默认税率）。
 *
 * <p>两者都没有时不收税，并打一条 WARN：这既不是"免税"也不是"算过了"，
 * 而是配置缺口——报关单导出与管理端可以看到缺失，避免"看起来有税其实没算"。
 */
@Service
public class TaxService {

    private static final Logger log = LoggerFactory.getLogger(TaxService.class);
    /** 基点刻度：10000 bps = 100%。写成常量而不是散落的魔法数字，避免再次出现分母写错的数量级 bug。 */
    private static final long BPS_SCALE = 10_000L;

    private final JdbcTemplate jdbcTemplate;

    public TaxService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 订单税行快照：税率、税基、税额都在下单时固定，事后改税率不影响历史订单。 */
    public record TaxLine(String jurisdiction, String name, int rateBps, boolean priceIncludesTax,
                         long taxableAmount, long taxAmount) {
    }

    /** 该 市场 × 目的地 是否有可用税率（门禁/展示用，不抛异常）。 */
    public boolean hasRule(String marketCode, String destinationCountry) {
        String destination = destinationCountry == null || destinationCountry.isBlank()
                ? null : destinationCountry.trim().toUpperCase(Locale.ROOT);
        if (destination != null && find(destination).isPresent()) {
            return true;
        }
        return marketJurisdiction(marketCode).flatMap(this::find).isPresent();
    }

    public Optional<TaxLine> calculate(String marketCode, String destinationCountry, Money subtotal) {
        String destination = destinationCountry == null || destinationCountry.isBlank()
                ? null : destinationCountry.trim().toUpperCase(Locale.ROOT);
        Optional<Rule> rule = destination == null ? Optional.empty() : find(destination);
        if (rule.isEmpty()) {
            rule = marketJurisdiction(marketCode).flatMap(this::find);
        }
        if (rule.isEmpty()) {
            log.warn("未配置税率，按不收税处理 market={} destination={} subtotal={} {}",
                    marketCode, destination, subtotal.amount(), subtotal.currency());
            return Optional.empty();
        }
        Rule found = rule.get();
        long tax;
        long taxable;
        // 基点分母是 10000（1 bps = 0.01%，1300 bps = 13%），不是 100000
        if (found.priceIncludesTax()) {
            // 价内含税：税基 = 价 / (1 + 税率)，税额 = 价 − 税基（整数运算避免浮点误差）
            taxable = BigDecimal.valueOf(subtotal.amount())
                    .multiply(BigDecimal.valueOf(BPS_SCALE))
                    .divide(BigDecimal.valueOf(BPS_SCALE + found.rateBps()), 0, RoundingMode.HALF_UP)
                    .longValueExact();
            tax = subtotal.amount() - taxable;
        } else {
            taxable = subtotal.amount();
            tax = BigDecimal.valueOf(taxable)
                    .multiply(BigDecimal.valueOf(found.rateBps()))
                    .divide(BigDecimal.valueOf(BPS_SCALE), 0, RoundingMode.HALF_UP)
                    .longValueExact();
        }
        return Optional.of(new TaxLine(found.jurisdiction(), found.name(), found.rateBps(),
                found.priceIncludesTax(), taxable, tax));
    }

    /** 管理端视图：一条税率规则。 */
    public record RuleRow(String id, String jurisdiction, String name, int rateBps,
                          boolean priceIncludesTax, boolean active) {
    }

    public List<RuleRow> list() {
        return jdbcTemplate.query("""
                SELECT id, jurisdiction, name, rate_bps, price_includes_tax, active
                  FROM tax_rule ORDER BY jurisdiction
                """, (rs, rowNum) -> new RuleRow(rs.getString("id"), rs.getString("jurisdiction"),
                rs.getString("name"), rs.getInt("rate_bps"), rs.getBoolean("price_includes_tax"),
                rs.getBoolean("active")));
    }

    /**
     * 写入税率。同一辖区只保留一条启用规则（部分唯一索引兜底），
     * 否则"按辖区查税"会随机命中其中一条，税额不可复现。
     */
    public void upsert(String id, String jurisdiction, String name, int rateBps,
                       boolean priceIncludesTax, boolean active) {
        if (rateBps < 0 || rateBps > (int) BPS_SCALE) {
            throw com.fullmall.platform.error.ApiException.badRequest("TAX_RATE_INVALID",
                    "税率基点必须在 0..10000 之间（10000 = 100%），实际=" + rateBps);
        }
        String key = jurisdiction == null ? "" : jurisdiction.trim();
        if (key.isEmpty()) {
            throw com.fullmall.platform.error.ApiException.badRequest("TAX_JURISDICTION_REQUIRED",
                    "税务辖区不能为空（国家码或市场 tax_jurisdiction）");
        }
        // 同辖区的启用规则唯一（部分唯一索引）：已有就更新它，而不是让"再存一次"撞唯一索引报错。
        // 多行同时启用属脏数据，要说出来而不是随便挑一条改。
        List<String> activeIds = jdbcTemplate.queryForList(
                "SELECT id FROM tax_rule WHERE jurisdiction = ? AND active", String.class, key);
        if (activeIds.size() > 1) {
            throw com.fullmall.platform.error.ApiException.conflict("TAX_RULE_DUPLICATED",
                    "辖区 " + key + " 存在多条启用规则（" + activeIds.size() + "），请先清理");
        }
        String target = activeIds.isEmpty() ? id : activeIds.get(0);
        jdbcTemplate.update("""
                INSERT INTO tax_rule (id, jurisdiction, name, rate_bps, price_includes_tax, active)
                VALUES (?::uuid, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    jurisdiction = EXCLUDED.jurisdiction,
                    name = EXCLUDED.name,
                    rate_bps = EXCLUDED.rate_bps,
                    price_includes_tax = EXCLUDED.price_includes_tax,
                    active = EXCLUDED.active,
                    updated_at = now()
                """, target, key, name, rateBps, priceIncludesTax, active);
    }

    public void delete(String id) {
        int removed = jdbcTemplate.update("DELETE FROM tax_rule WHERE id = ?::uuid", id);
        if (removed == 0) {
            throw com.fullmall.platform.error.ApiException.notFound("TAX_RULE_NOT_FOUND",
                    "税率规则不存在: " + id);
        }
    }

    private record Rule(String jurisdiction, String name, int rateBps, boolean priceIncludesTax) {
    }

    private Optional<Rule> find(String jurisdiction) {
        return jdbcTemplate.query("""
                SELECT jurisdiction, name, rate_bps, price_includes_tax
                  FROM tax_rule WHERE jurisdiction = ? AND active
                """, (rs, rowNum) -> new Rule(rs.getString("jurisdiction"), rs.getString("name"),
                rs.getInt("rate_bps"), rs.getBoolean("price_includes_tax")), jurisdiction)
                .stream().findFirst();
    }

    private Optional<String> marketJurisdiction(String marketCode) {
        if (marketCode == null || marketCode.isBlank()) {
            return Optional.empty();
        }
        List<String> rows = jdbcTemplate.queryForList(
                "SELECT tax_jurisdiction FROM market WHERE code = ?",
                String.class, marketCode.trim().toUpperCase(Locale.ROOT));
        return rows.isEmpty() || rows.get(0) == null || rows.get(0).isBlank()
                ? Optional.empty() : Optional.of(rows.get(0));
    }
}
