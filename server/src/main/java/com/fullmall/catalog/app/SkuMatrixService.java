package com.fullmall.catalog.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * SKU 规格矩阵批量生成。
 *
 * <p>规则：
 * <ul>
 *   <li>编码规则 `商品前缀-规格值`（大写、空格转连字符），**同一商品下唯一**；</li>
 *   <li>幂等：已存在的编码跳过并计入 `skipped`，不报错也不覆盖价格；</li>
 *   <li>至少一个规格值；价格必须 &gt; 0；规格值不可重复；</li>
 *   <li>生成后仍需人工核对库存与图片（不自动生成库存）。</li>
 * </ul>
 */
@Service
public class SkuMatrixService {

    private final JdbcTemplate jdbc;

    public SkuMatrixService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Result(int created, int skipped, List<String> codes) {
    }

    @Transactional
    public Result generate(UUID productId, String prefix, String optionName,
                           List<String> values, long priceAmount, String currency) {
        if (prefix == null || prefix.isBlank()) {
            throw ApiException.badRequest("SKU_PREFIX_REQUIRED", "SKU 前缀不能为空");
        }
        if (optionName == null || optionName.isBlank()) {
            throw ApiException.badRequest("SKU_OPTION_REQUIRED", "规格名不能为空");
        }
        if (values == null || values.isEmpty()) {
            throw ApiException.badRequest("SKU_VALUES_REQUIRED", "至少需要一个规格值");
        }
        if (priceAmount <= 0) {
            throw ApiException.badRequest("SKU_PRICE_INVALID", "价格必须大于 0");
        }
        // 币种从已有 SKU 推断（product 表没有 currency 列）
        var currencies = jdbc.query("SELECT price_currency FROM sku WHERE product_id = ? LIMIT 1",
                (rs, rowNum) -> rs.getString(1), productId);
        if (currencies.isEmpty()) {
            throw ApiException.badRequest("SKU_PRODUCT_EMPTY",
                    "该商品还没有任何 SKU，无法推断币种：" + productId);
        }
        String productCurrency = currencies.get(0);
        String effectiveCurrency = currency == null || currency.isBlank()
                ? productCurrency : currency.trim().toUpperCase(Locale.ROOT);

        List<String> seen = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        for (String raw : values) {
            String value = raw == null ? "" : raw.trim();
            if (value.isEmpty()) {
                throw ApiException.badRequest("SKU_VALUE_EMPTY", "规格值不能为空");
            }
            String code = (prefix.trim() + "-" + value.replaceAll("\\s+", "-"))
                    .toUpperCase(Locale.ROOT);
            if (seen.contains(code)) {
                throw ApiException.badRequest("SKU_CODE_DUPLICATED", "规格值重复导致编码重复：" + code);
            }
            seen.add(code);
            codes.add(code);
            int inserted = jdbc.update("""
                    INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency, options_json)
                    VALUES (?, ?, ?, ?, ?, jsonb_build_object(?, ?))
                    ON CONFLICT (sku_code) DO NOTHING
                    """, UUID.randomUUID(), productId, code, priceAmount, effectiveCurrency,
                    optionName.trim(), value);
            if (inserted == 1) {
                created++;
            } else {
                skipped++;
            }
        }
        return new Result(created, skipped, codes);
    }
}
