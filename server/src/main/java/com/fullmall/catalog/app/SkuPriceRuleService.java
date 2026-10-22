package com.fullmall.catalog.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 按规格筛选批量改价（先预览、后应用）。
 *
 * <p>模式：
 * <ul>
 *   <li>`ADD`：在原价上加 `value`（可为负，但不允许结果为 0 或负）；</li>
 *   <li>`SET`：直接设为 `value`。</li>
 * </ul>
 *
 * <p>筛选：`match` 是"维度=值"的等值条件（如 `{"存储":"256G"}`），
 * 用 `options_json @>` 匹配，**只改命中的 SKU**，不动其它规格。
 *
 * <p>为什么强制先预览：改价影响毛利与促销叠加，盲改最容易出事。
 */
@Service
public class SkuPriceRuleService {

    private final JdbcTemplate jdbc;

    public SkuPriceRuleService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public enum Mode { ADD, SET }

    public record Change(UUID skuId, String skuCode, long before, long after) {
    }

    public List<Change> preview(UUID productId, Map<String, String> match, Mode mode, long value) {
        validate(match, mode, value);
        String matchJson = toJson(match);
        List<Change> changes = new ArrayList<>();
        jdbc.query("""
                SELECT id, sku_code, price_amount
                  FROM sku
                 WHERE product_id = ? AND options_json @> ?::jsonb
                 ORDER BY sku_code
                """, rs -> {
            long before = rs.getLong("price_amount");
            long after = mode == Mode.SET ? value : before + value;
            if (after <= 0) {
                throw ApiException.badRequest("SKU_PRICE_RESULT_INVALID",
                        "改价后价格必须大于 0（" + rs.getString("sku_code")
                                + "：" + before + " → " + after + "）");
            }
            changes.add(new Change(rs.getObject("id", UUID.class), rs.getString("sku_code"),
                    before, after));
        }, productId, matchJson);
        if (changes.isEmpty()) {
            throw ApiException.notFound("SKU_FILTER_NO_MATCH",
                    "没有匹配该规格条件的 SKU：" + matchJson);
        }
        return changes;
    }

    @Transactional
    public int apply(UUID productId, Map<String, String> match, Mode mode, long value) {
        List<Change> changes = preview(productId, match, mode, value);
        for (Change change : changes) {
            jdbc.update("UPDATE sku SET price_amount = ? WHERE id = ?",
                    change.after(), change.skuId());
        }
        return changes.size();
    }

    private void validate(Map<String, String> match, Mode mode, long value) {
        if (match == null || match.isEmpty()) {
            // 空筛选等于"全商品改价"，风险太高，必须显式传维度条件
            throw ApiException.badRequest("SKU_FILTER_REQUIRED",
                    "必须至少指定一个规格条件（如 存储=256G），不允许无差别改价");
        }
        if (mode == null) {
            throw ApiException.badRequest("SKU_PRICE_MODE_REQUIRED", "必须指定改价模式");
        }
        if (mode == Mode.SET && value <= 0) {
            throw ApiException.badRequest("SKU_PRICE_INVALID", "设定价格必须大于 0");
        }
        if (mode == Mode.ADD && value == 0) {
            throw ApiException.badRequest("SKU_PRICE_NO_CHANGE", "调整金额不能为 0");
        }
    }

    private String toJson(Map<String, String> match) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (var entry : new LinkedHashMap<>(match).entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(entry.getKey().replace("\"", "")).append("\":\"")
                    .append(entry.getValue() == null ? "" : entry.getValue().replace("\"", ""))
                    .append('"');
        }
        return json.append('}').toString();
    }
}
