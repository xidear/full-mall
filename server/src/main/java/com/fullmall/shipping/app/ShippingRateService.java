package com.fullmall.shipping.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * 运费计算（市场 × 目的地 × 重量档）。
 *
 * <p>规则顺序：先找"指定目的地"的档位，再找该市场的"其它目的地"默认档。
 * 两者都没有时：
 * <ul>
 *   <li>目的地 = 市场所属国家 → 免运费（现状 FLAT_SHIPPING_FEE=0 的等价物，已落成数据）；</li>
 *   <li>跨国 → **明确报错**：跨境运费必须由运营配置，绝不默认免运费（那等于白送）。</li>
 * </ul>
 */
@Service
public class ShippingRateService {

    private final JdbcTemplate jdbcTemplate;

    public ShippingRateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Fee(Money amount, String source) {
    }

    /**
     * 订单总净重（克）。缺失重量的 SKU 按 0 计——报关单导出会把这些行标出来，
     * 不让"没填重量"看起来像"重量为 0 的合法申报"。
     */
    public int totalWeightGrams(java.util.Map<java.util.UUID, Integer> quantities) {
        if (quantities == null || quantities.isEmpty()) {
            return 0;
        }
        var rows = jdbcTemplate.query("""
                SELECT id, net_weight_grams FROM sku WHERE id = ANY(?)
                """, (rs, rowNum) -> new Object[] {
                        rs.getObject("id", java.util.UUID.class), rs.getObject("net_weight_grams") },
                (Object) quantities.keySet().toArray(new java.util.UUID[0]));
        int total = 0;
        for (Object[] row : rows) {
            Integer weight = (Integer) row[1];
            Integer quantity = quantities.get((java.util.UUID) row[0]);
            if (weight != null && quantity != null) {
                total += weight * quantity;
            }
        }
        return total;
    }

    /**
     * 是否存在可用的运费规则（不抛异常）。跨境判断口径与 {@link #fee} 一致：
     * 目的地档位或该市场默认档两者之一即可；都没有时只有"本国目的地"算免运费。
     */
    public boolean hasRule(String marketCode, String destinationCountry) {
        String market = marketCode == null ? null : marketCode.trim().toUpperCase(Locale.ROOT);
        if (market == null || market.isBlank()) {
            return false;
        }
        String destination = destinationCountry == null ? null
                : destinationCountry.trim().toUpperCase(Locale.ROOT);
        // 门禁只看"有没有档位"，不看重量：重量档位是计费明细，缺某个重量段应在计费时报
        if (!anyBand(market, destination)) {
            return false;
        }
        return true;
    }

    private boolean anyBand(String marketCode, String destinationCountry) {
        if (destinationCountry != null) {
            Long exact = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM shipping_rate WHERE market_code = ? AND destination_country = ?
                    """, Long.class, marketCode, destinationCountry);
            if (exact != null && exact > 0) {
                return true;
            }
        }
        Long fallback = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM shipping_rate WHERE market_code = ? AND destination_country IS NULL
                """, Long.class, marketCode);
        if (fallback != null && fallback > 0) {
            return true;
        }
        String country = marketCountry(marketCode);
        return destinationCountry != null && destinationCountry.equals(country);
    }

    /** 管理端视图：一条运费档位。 */
    public record RateRow(String id, String marketCode, String destinationCountry, int minWeightGrams,
                         Integer maxWeightGrams, long feeAmount, String currency,
                         Long freeOverAmount) {
    }

    public List<RateRow> list(String marketCode) {
        String sql = marketCode == null || marketCode.isBlank()
                ? """
                  SELECT id, market_code, destination_country, min_weight_grams, max_weight_grams,
                         fee_amount, currency, free_over_amount
                    FROM shipping_rate ORDER BY market_code, destination_country NULLS FIRST, min_weight_grams
                  """
                : """
                  SELECT id, market_code, destination_country, min_weight_grams, max_weight_grams,
                         fee_amount, currency, free_over_amount
                    FROM shipping_rate WHERE market_code = ?
                   ORDER BY destination_country NULLS FIRST, min_weight_grams
                  """;
        Object[] args = marketCode == null || marketCode.isBlank()
                ? new Object[] {}
                : new Object[] {normalize(marketCode)};
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RateRow(
                rs.getString("id"), rs.getString("market_code"), rs.getString("destination_country"),
                rs.getInt("min_weight_grams"),
                rs.getObject("max_weight_grams") == null ? null : rs.getInt("max_weight_grams"),
                rs.getLong("fee_amount"), rs.getString("currency"),
                rs.getObject("free_over_amount") == null ? null : rs.getLong("free_over_amount")), args);
    }

    /**
     * 写入档位。币种强制取市场币种（不允许运营给某市场配一个别的币种的运费）。
     * 目标国家留空 = 该市场"其它目的地"的默认档。
     */
    public void upsert(String id, String marketCode, String destinationCountry, int minWeightGrams,
                       Integer maxWeightGrams, long feeAmount, Long freeOverAmount) {
        String market = normalize(marketCode);
        String currency = marketCurrency(market);
        String destination = destinationCountry == null || destinationCountry.isBlank()
                ? null : destinationCountry.trim().toUpperCase(Locale.ROOT);
        if (maxWeightGrams != null && maxWeightGrams <= minWeightGrams) {
            throw ApiException.badRequest("SHIPPING_BAND_INVALID",
                    "重量上限必须大于下限，实际 " + minWeightGrams + ".." + maxWeightGrams);
        }
        // 同一 市场×目的地×重量档 只允许一条（唯一索引）：已有就更新它，
        // 否则"再存一次"会撞唯一索引报错，运营会以为保存失败、其实只是重复。
        List<String> existing = jdbcTemplate.queryForList("""
                SELECT id FROM shipping_rate
                 WHERE market_code = ? AND COALESCE(destination_country, '*') = COALESCE(?, '*')
                   AND min_weight_grams = ?
                """, String.class, market, destination, minWeightGrams);
        if (existing.size() > 1) {
            throw ApiException.conflict("SHIPPING_RATE_DUPLICATED",
                    "同一市场/目的地/重量档存在多条规则（" + existing.size() + "），请先清理");
        }
        String target = existing.isEmpty() ? id : existing.get(0);
        jdbcTemplate.update("""
                INSERT INTO shipping_rate (id, market_code, destination_country, min_weight_grams,
                                          max_weight_grams, fee_amount, currency, free_over_amount)
                VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    market_code = EXCLUDED.market_code,
                    destination_country = EXCLUDED.destination_country,
                    min_weight_grams = EXCLUDED.min_weight_grams,
                    max_weight_grams = EXCLUDED.max_weight_grams,
                    fee_amount = EXCLUDED.fee_amount,
                    currency = EXCLUDED.currency,
                    free_over_amount = EXCLUDED.free_over_amount
                """, target, market, destination, minWeightGrams, maxWeightGrams, feeAmount, currency,
                freeOverAmount);
    }

    public void delete(String id) {
        int removed = jdbcTemplate.update("DELETE FROM shipping_rate WHERE id = ?::uuid", id);
        if (removed == 0) {
            throw ApiException.notFound("SHIPPING_RATE_NOT_FOUND", "运费档位不存在: " + id);
        }
    }

    /**
     * @param totalWeightGrams 订单总净重（缺失重量按 0 计，已在计算前汇总）
     * @param subtotal        商品小计，用于"满额免运费"
     */
    public Fee fee(String marketCode, String destinationCountry, int totalWeightGrams, Money subtotal) {
        String market = normalize(marketCode);
        String destination = destinationCountry == null ? null
                : destinationCountry.trim().toUpperCase(Locale.ROOT);
        int weight = Math.max(totalWeightGrams, 0);

        List<Band> exact = bands(market, destination, weight);
        if (!exact.isEmpty()) {
            return freeOver(exact.get(0), subtotal);
        }
        if (destination != null) {
            List<Band> fallback = bands(market, null, weight);
            if (!fallback.isEmpty()) {
                return freeOver(fallback.get(0), subtotal);
            }
        }
        String marketCountry = marketCountry(market);
        if (destination != null && destination.equals(marketCountry)) {
            return new Fee(Money.zero(subtotal.currency()), "DOMESTIC_DEFAULT");
        }
        throw ApiException.conflict("SHIPPING_RULE_MISSING",
                "市场 " + market + " 到目的地 " + destination + " 没有配置运费规则，无法下单");
    }

    /** 满额免运费：门槛按**商品小计**比较（不含税与运费），达到即免。 */
    private Fee freeOver(Band band, Money subtotal) {
        if (band.freeOverAmount() != null && subtotal.amount() >= band.freeOverAmount()) {
            return new Fee(Money.zero(subtotal.currency()), "FREE_OVER");
        }
        return new Fee(band.amount(), "RATE");
    }

    /** 命中的档位及其满额门槛（门槛与金额同表，避免再查一次）。 */
    private record Band(Money amount, Long freeOverAmount) {
    }

    private List<Band> bands(String marketCode, String destinationCountry, int weight) {
        String sql = destinationCountry == null
                ? """
                  SELECT fee_amount, currency, free_over_amount FROM shipping_rate
                   WHERE market_code = ? AND destination_country IS NULL
                     AND min_weight_grams <= ?
                     AND (max_weight_grams IS NULL OR max_weight_grams > ?)
                   ORDER BY min_weight_grams DESC LIMIT 1
                  """
                : """
                  SELECT fee_amount, currency, free_over_amount FROM shipping_rate
                   WHERE market_code = ? AND destination_country = ?
                     AND min_weight_grams <= ?
                     AND (max_weight_grams IS NULL OR max_weight_grams > ?)
                   ORDER BY min_weight_grams DESC LIMIT 1
                  """;
        Object[] args = destinationCountry == null
                ? new Object[] {marketCode, weight, weight}
                : new Object[] {marketCode, destinationCountry, weight, weight};
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Band(
                new Money(rs.getLong("fee_amount"), rs.getString("currency")),
                rs.getObject("free_over_amount") == null ? null : rs.getLong("free_over_amount")), args)
                .stream().toList();
    }

    /** 运费币种强制取市场币种：允许别的币种会让"运费金额"与订单币种对不上。 */
    private String marketCurrency(String marketCode) {
        List<String> rows = jdbcTemplate.queryForList(
                "SELECT currency FROM market WHERE code = ?", String.class, marketCode);
        if (rows.isEmpty()) {
            throw ApiException.notFound("MARKET_NOT_FOUND", "市场不存在: " + marketCode);
        }
        return rows.get(0);
    }

    private String marketCountry(String marketCode) {
        List<String> rows = jdbcTemplate.queryForList(
                "SELECT country_code FROM market WHERE code = ?", String.class, marketCode);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String normalize(String marketCode) {
        if (marketCode == null || marketCode.isBlank()) {
            throw ApiException.badRequest("MARKET_CODE_REQUIRED", "缺少市场码");
        }
        return marketCode.trim().toUpperCase(Locale.ROOT);
    }
}
