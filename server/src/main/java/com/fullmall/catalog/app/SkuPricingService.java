package com.fullmall.catalog.app;

import com.fullmall.catalog.domain.Sku;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 按市场取价（ADR-0013 的下游能力）。
 *
 * <p>规则（价格表覆盖模型）：
 * <ul>
 *   <li>`sku_price` 有该市场的覆盖价 → 用它；</li>
 *   <li>没有覆盖价 → 用 SKU 基础价，**但币种必须与该市场币种一致**；</li>
 *   <li>币种不一致 → 明确报错，绝不把人民币价当港币卖（这是最常见的跨境算错钱方式）；</li>
 *   <li>覆盖价自身币种与市场币种不一致 → 同样报错（配置错误，不静默接受）。</li>
 * </ul>
 */
@Service
public class SkuPricingService {

    private final JdbcTemplate jdbcTemplate;

    public SkuPricingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 该市场下的价格与划线价。 */
    public record MarketPrice(Money price, Money compareAt) {
    }

    /** 管理端视图：某 SKU 在各市场的覆盖价（不等同于最终售价，最终售价还要看是否有覆盖）。 */
    public record PriceRow(String marketCode, String currency, long amount, Long compareAtAmount,
                          boolean active) {
    }

    public List<PriceRow> list(UUID skuId) {
        return jdbcTemplate.query("""
                SELECT market_code, currency, amount, compare_at_amount, active
                  FROM sku_price WHERE sku_id = ? ORDER BY market_code
                """, (rs, rowNum) -> new PriceRow(
                rs.getString("market_code"), rs.getString("currency"), rs.getLong("amount"),
                rs.getObject("compare_at_amount") == null ? null : rs.getLong("compare_at_amount"),
                rs.getBoolean("active")), skuId);
    }

    /**
     * 写入某市场的覆盖价。币种必须等于该市场币种：允许写入不一致的币种等于允许"标错币种的价"，
     * 最终会以错误金额收款。
     */
    public void upsert(UUID skuId, String marketCode, String currency, long amount, Long compareAtAmount) {
        String market = normalize(marketCode);
        String marketCurrency = marketCurrency(market);
        if (!marketCurrency.equalsIgnoreCase(currency == null ? "" : currency.trim())) {
            throw ApiException.badRequest("SKU_PRICE_CURRENCY_MISMATCH",
                    "该市场币种是 " + marketCurrency + "，覆盖价币种必须是同一个，实际=" + currency);
        }
        Long exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sku WHERE id = ?", Long.class, skuId);
        if (exists == null || exists == 0) {
            throw ApiException.notFound("SKU_NOT_FOUND", "SKU 不存在: " + skuId);
        }
        jdbcTemplate.update("""
                INSERT INTO sku_price (sku_id, market_code, currency, amount, compare_at_amount, active)
                VALUES (?, ?, ?, ?, ?, TRUE)
                ON CONFLICT (sku_id, market_code) DO UPDATE SET
                    currency = EXCLUDED.currency,
                    amount = EXCLUDED.amount,
                    compare_at_amount = EXCLUDED.compare_at_amount,
                    active = TRUE,
                    updated_at = now()
                """, skuId, market, marketCurrency, amount, compareAtAmount);
    }

    /** 删除覆盖价 = 回到基础价（不是"价格变 0"）。 */
    public void remove(UUID skuId, String marketCode) {
        int removed = jdbcTemplate.update(
                "DELETE FROM sku_price WHERE sku_id = ? AND market_code = ?",
                skuId, normalize(marketCode));
        if (removed == 0) {
            throw ApiException.notFound("SKU_PRICE_NOT_FOUND", "该市场没有覆盖价: " + marketCode);
        }
    }

    public MarketPrice priceFor(Sku sku, String marketCode) {
        String currency = marketCurrency(marketCode);
        List<MarketPrice> overrides = jdbcTemplate.query("""
                SELECT currency, amount, compare_at_amount
                  FROM sku_price
                 WHERE sku_id = ? AND market_code = ? AND active
                """, (rs, rowNum) -> new MarketPrice(
                new Money(rs.getLong("amount"), rs.getString("currency")),
                rs.getObject("compare_at_amount") == null ? null
                        : new Money(rs.getLong("compare_at_amount"), rs.getString("currency"))),
                sku.id(), normalize(marketCode)).stream().toList();
        if (!overrides.isEmpty()) {
            MarketPrice override = overrides.get(0);
            if (!override.price().currency().equals(currency)) {
                throw ApiException.conflict("SKU_PRICE_CURRENCY_MISMATCH",
                        "商品 " + sku.skuCode() + " 在市场 " + marketCode + " 的定价币种 "
                                + override.price().currency() + " 与市场币种 " + currency + " 不一致");
            }
            return override;
        }
        if (!sku.price().currency().equals(currency)) {
            throw ApiException.conflict("SKU_PRICE_MISSING",
                    "商品 " + sku.skuCode() + " 在市场 " + marketCode + " 未定价（基础价币种 "
                            + sku.price().currency() + "，市场币种 " + currency + "）");
        }
        return new MarketPrice(sku.price(), sku.compareAtPrice());
    }

    /** 市场币种是所有"这单该收多少"判断的锚点；缺市场行属配置错误。 */
    private String marketCurrency(String marketCode) {
        List<String> rows = jdbcTemplate.queryForList(
                "SELECT currency FROM market WHERE code = ?", String.class, normalize(marketCode));
        if (rows.isEmpty()) {
            throw ApiException.notFound("MARKET_NOT_FOUND", "市场不存在: " + marketCode);
        }
        return rows.get(0);
    }

    private String normalize(String marketCode) {
        if (marketCode == null || marketCode.isBlank()) {
            throw ApiException.badRequest("MARKET_CODE_REQUIRED", "缺少市场码");
        }
        return marketCode.trim().toUpperCase(Locale.ROOT);
    }
}
