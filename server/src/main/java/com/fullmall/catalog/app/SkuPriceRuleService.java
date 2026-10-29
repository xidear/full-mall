package com.fullmall.catalog.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 按规格筛选批量改价（先预览、后应用）：**作用域永远是"一个商品内的规格组合"**。
 *
 * <p>为什么按商品：价格是"某商品某 SKU"的事实。按"存储=256G"跨商品改价会把
 * 完全不相干的 200 个商品一起改掉（2026-10-28 用户指出的设计问题）。
 *
 * <p>筛选语义（与后台"规格改价"弹窗一一对应）：
 * <ul>
 *   <li>维度之间是 **AND**：选了「颜色」和「存储」时，只有同时满足两者的组合命中；</li>
 *   <li>同一维度的多个值是 **OR**：勾选「128」「256」即两者都命中；</li>
 *   <li>只勾了维度没勾值 = 该维度不参与筛选；一个值都没勾 → 明确报错，不做全量改价。</li>
 * </ul>
 *
 * <p>模式：ADD（加/减，结果必须 &gt; 0）、SET（直接设值，必须 &gt; 0）。
 * 先预览后应用：改价影响毛利与促销叠加，盲改最容易出事。
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

    /** 单值兼容入口（老接口/老测试）：等价于该维度只有一个取值。 */
    // 单值入口保留给老调用方与既有测试：把单个取值包成单元素列表后复用多值实现，
    // 保证两条路径的筛选语义与校验永远一致（删掉它会让老客户端 404）。
    public List<Change> preview(UUID productId, Map<String, String> match, Mode mode, long value) {
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        if (match != null) {
            for (var entry : match.entrySet()) {
                normalized.put(entry.getKey(),
                        entry.getValue() == null ? List.of() : List.of(entry.getValue()));
            }
        }
        return previewMulti(productId, normalized, mode, value);
    }

    public int apply(UUID productId, Map<String, String> match, Mode mode, long value) {
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        if (match != null) {
            for (var entry : match.entrySet()) {
                normalized.put(entry.getKey(),
                        entry.getValue() == null ? List.of() : List.of(entry.getValue()));
            }
        }
        return applyMulti(productId, normalized, mode, value);
    }

    public List<Change> previewMulti(UUID productId, Map<String, List<String>> match, Mode mode,
                                     long value) {
        Map<String, List<String>> filter = validateAndNormalize(match, mode, value);
        requireOptionsMigrated(productId);

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT s.id, s.sku_code, s.price_amount
                  FROM sku s
                 WHERE s.product_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(productId);
        for (var entry : filter.entrySet()) {
            sql.append("""
                      AND EXISTS (
                            SELECT 1 FROM sku_option_value v
                              JOIN product_option o ON o.id = v.option_id
                              JOIN product_option_value ov ON ov.id = v.option_value_id
                             WHERE v.sku_id = s.id AND o.name = ? AND ov.name IN (""");
            args.add(entry.getKey());
            for (int i = 0; i < entry.getValue().size(); i += 1) {
                sql.append(i == 0 ? "?" : ", ?");
                args.add(entry.getValue().get(i));
            }
            sql.append("))");
        }
        sql.append(" ORDER BY s.sku_code");

        List<Change> changes = new ArrayList<>();
        jdbc.query(sql.toString(), rs -> {
            long before = rs.getLong("price_amount");
            long after = mode == Mode.SET ? value : before + value;
            if (after <= 0) {
                throw ApiException.badRequest("SKU_PRICE_RESULT_INVALID",
                        "改价后价格必须大于 0（" + rs.getString("sku_code")
                                + "：" + before + " → " + after + "）");
            }
            changes.add(new Change(rs.getObject("id", UUID.class), rs.getString("sku_code"),
                    before, after));
        }, args.toArray());
        if (changes.isEmpty()) {
            throw ApiException.notFound("SKU_FILTER_NO_MATCH",
                    "没有匹配该规格条件的 SKU：" + describe(filter));
        }
        return changes;
    }

    /**
     * 应用改价：先按同一套筛选算出全部 {@code Change}，再逐条 UPDATE，整个方法一个事务。
     *
     * <p>先预览再写的好处是"校验失败一条都不写"：改价后价格 ≤ 0 会在写库前抛出，
     * 不留下"改了一半"的商品；返回的是实际改动条数，不是匹配条数。
     */
    @Transactional
    public int applyMulti(UUID productId, Map<String, List<String>> match, Mode mode, long value) {
        List<Change> changes = previewMulti(productId, match, mode, value);
        for (Change change : changes) {
            jdbc.update("UPDATE sku SET price_amount = ? WHERE id = ?",
                    change.after(), change.skuId());
        }
        return changes.size();
    }

    /** 商品的规格维度与取值（弹窗左侧/右侧用；只返回真实存在的组合，不做前端臆造）。 */
    public LinkedHashMap<String, List<String>> dimensions(UUID productId) {
        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT o.name AS dimension, ov.name AS value
                  FROM product_option o
                  JOIN product_option_value ov ON ov.option_id = o.id
                  JOIN sku_option_value v ON v.option_value_id = ov.id
                  JOIN sku s ON s.id = v.sku_id
                 WHERE o.product_id = ?
                 GROUP BY o.name, o.sort_order, ov.name, ov.sort_order
                 ORDER BY o.sort_order, o.name, ov.sort_order, ov.name
                """, rs -> {
            result.computeIfAbsent(rs.getString("dimension"), key -> new ArrayList<>())
                    .add(rs.getString("value"));
        }, productId);
        return result;
    }

    private Map<String, List<String>> validateAndNormalize(Map<String, List<String>> match,
                                                          Mode mode, long value) {
        if (mode == null) {
            throw ApiException.badRequest("SKU_PRICE_MODE_REQUIRED", "必须指定改价模式");
        }
        if (mode == Mode.SET && value <= 0) {
            throw ApiException.badRequest("SKU_PRICE_INVALID", "设定价格必须大于 0");
        }
        if (mode == Mode.ADD && value == 0) {
            throw ApiException.badRequest("SKU_PRICE_NO_CHANGE", "调整金额不能为 0");
        }
        LinkedHashMap<String, List<String>> normalized = new LinkedHashMap<>();
        if (match != null) {
            for (var entry : match.entrySet()) {
                String dimension = entry.getKey() == null ? "" : entry.getKey().trim();
                if (dimension.isEmpty()) {
                    continue;
                }
                LinkedHashSet<String> values = new LinkedHashSet<>();
                if (entry.getValue() != null) {
                    for (String raw : entry.getValue()) {
                        if (raw != null && !raw.isBlank()) {
                            values.add(raw.trim());
                        }
                    }
                }
                if (!values.isEmpty()) {
                    normalized.put(dimension, List.copyOf(values));
                }
            }
        }
        if (normalized.isEmpty()) {
            // 一个值都没勾时不做"全商品改价"：那是最容易误伤的路径
            throw ApiException.badRequest("SKU_FILTER_REQUIRED",
                    "请至少勾选一个规格值（例如 存储 128G），不允许无差别改价");
        }
        return normalized;
    }

    // 筛选 SQL 只认规格关联表；老商品可能只有 options_json 没有关联行。
    // 这种情况必须明确 409 让运营先回填，否则"匹配不到"会被误读成"没有这个规格"。
    private void requireOptionsMigrated(UUID productId) {
        Integer relationalRows = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sku_option_value v
                  JOIN sku s ON s.id = v.sku_id
                 WHERE s.product_id = ?
                """, Integer.class, productId);
        if (relationalRows != null && relationalRows > 0) {
            return;
        }
        Integer jsonRows = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sku WHERE product_id = ? AND options_json <> '{}'::jsonb
                """, Integer.class, productId);
        if (jsonRows != null && jsonRows > 0) {
            throw ApiException.conflict("SKU_OPTIONS_NOT_MIGRATED",
                    "该商品规格尚未迁移到关联表，请先执行回填（/admin/v1/products/{id}/sku-options/backfill）");
        }
    }

    private String describe(Map<String, List<String>> filter) {
        StringBuilder text = new StringBuilder();
        for (var entry : filter.entrySet()) {
            if (!text.isEmpty()) {
                text.append(" 且 ");
            }
            text.append(entry.getKey()).append('=').append(String.join("/", entry.getValue()));
        }
        return text.toString();
    }
}
