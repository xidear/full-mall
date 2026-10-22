package com.fullmall.catalog.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 多维度规格矩阵（颜色 × 内存 × 存储）。
 *
 * <p>规则：
 * <ul>
 *   <li>维度按传入顺序参与编码：`前缀-值1-值2-值3`（大写、空格转连字符）；</li>
 *   <li>`options_json` 存**全部维度**（不是只存一个键）；</li>
 *   <li>组合禁用：`disabled` 中列出的组合跳过生成（例如"512G 只有黑色"）；</li>
 *   <li>幂等：编码已存在则跳过，不覆盖价格；</li>
 *   <li>上限：维度数 ≤ 4、组合数 ≤ 500，超出明确拒绝（防止误操作生成上万 SKU）。</li>
 * </ul>
 */
@Service
public class SkuMatrixMultiService {

    private static final int MAX_DIMENSIONS = 4;
    private static final int MAX_COMBINATIONS = 500;

    private final JdbcTemplate jdbc;

    public SkuMatrixMultiService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Result(int created, int skipped, int disabled, List<String> codes) {
    }

    @Transactional
    public Result generate(UUID productId, String prefix,
                           Map<String, List<String>> dimensions,
                           List<Map<String, String>> disabled,
                           long priceAmount) {
        if (prefix == null || prefix.isBlank()) {
            throw ApiException.badRequest("SKU_PREFIX_REQUIRED", "SKU 前缀不能为空");
        }
        if (dimensions == null || dimensions.isEmpty()) {
            throw ApiException.badRequest("SKU_DIMENSIONS_REQUIRED", "至少需要一个规格维度");
        }
        if (dimensions.size() > MAX_DIMENSIONS) {
            throw ApiException.badRequest("SKU_TOO_MANY_DIMENSIONS",
                    "规格维度最多 " + MAX_DIMENSIONS + " 个，当前 " + dimensions.size());
        }
        if (priceAmount <= 0) {
            throw ApiException.badRequest("SKU_PRICE_INVALID", "价格必须大于 0");
        }
        var currencies = jdbc.query("SELECT price_currency FROM sku WHERE product_id = ? LIMIT 1",
                (rs, rowNum) -> rs.getString(1), productId);
        if (currencies.isEmpty()) {
            throw ApiException.badRequest("SKU_PRODUCT_EMPTY",
                    "该商品还没有任何 SKU，无法推断币种：" + productId);
        }
        String currency = currencies.get(0);

        List<String> names = new ArrayList<>(dimensions.keySet());
        List<List<String>> valueLists = new ArrayList<>();
        long totalCombinations = 1;
        for (String name : names) {
            List<String> values = dimensions.get(name);
            if (values == null || values.isEmpty()) {
                throw ApiException.badRequest("SKU_DIMENSION_VALUES_REQUIRED",
                        "规格维度「" + name + "」没有取值");
            }
            List<String> cleaned = new ArrayList<>();
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    throw ApiException.badRequest("SKU_VALUE_EMPTY",
                            "规格维度「" + name + "」存在空值");
                }
                String trimmed = value.trim();
                if (cleaned.contains(trimmed)) {
                    throw ApiException.badRequest("SKU_VALUE_DUPLICATED",
                            "规格维度「" + name + "」取值重复：" + trimmed);
                }
                cleaned.add(trimmed);
            }
            valueLists.add(cleaned);
            totalCombinations *= cleaned.size();
        }
        if (totalCombinations > MAX_COMBINATIONS) {
            throw ApiException.badRequest("SKU_TOO_MANY_COMBINATIONS",
                    "组合数 " + totalCombinations + " 超过上限 " + MAX_COMBINATIONS);
        }

        List<Map<String, String>> combos = new ArrayList<>();
        buildCombos(names, valueLists, 0, new LinkedHashMap<>(), combos);

        int created = 0;
        int skipped = 0;
        int disabledCount = 0;
        List<String> codes = new ArrayList<>();
        for (Map<String, String> combo : combos) {
            if (isDisabled(combo, disabled)) {
                disabledCount++;
                continue;
            }
            StringBuilder code = new StringBuilder(prefix.trim().toUpperCase(Locale.ROOT));
            for (String name : names) {
                code.append('-').append(combo.get(name).replaceAll("\\s+", "-").toUpperCase(Locale.ROOT));
            }
            String skuCode = code.toString();
            codes.add(skuCode);
            int inserted = jdbc.update("""
                    INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency, options_json)
                    VALUES (?, ?, ?, ?, ?, ?::jsonb)
                    ON CONFLICT (sku_code) DO NOTHING
                    """, UUID.randomUUID(), productId, skuCode, priceAmount, currency,
                    toJson(combo));
            if (inserted == 1) {
                created++;
            } else {
                skipped++;
            }
        }
        return new Result(created, skipped, disabledCount, codes);
    }

    private void buildCombos(List<String> names, List<List<String>> values, int index,
                             Map<String, String> current, List<Map<String, String>> out) {
        if (index == names.size()) {
            out.add(new LinkedHashMap<>(current));
            return;
        }
        for (String value : values.get(index)) {
            current.put(names.get(index), value);
            buildCombos(names, values, index + 1, current, out);
        }
        current.remove(names.get(index));
    }

    private boolean isDisabled(Map<String, String> combo, List<Map<String, String>> disabled) {
        if (disabled == null || disabled.isEmpty()) {
            return false;
        }
        for (Map<String, String> rule : disabled) {
            if (rule == null || rule.isEmpty()) {
                continue;
            }
            boolean matches = rule.entrySet().stream()
                    .allMatch(entry -> entry.getValue() != null
                            && entry.getValue().equals(combo.get(entry.getKey())));
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private String toJson(Map<String, String> combo) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (var entry : combo.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(escape(entry.getKey())).append("\":\"")
                    .append(escape(entry.getValue())).append('"');
        }
        return json.append('}').toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
