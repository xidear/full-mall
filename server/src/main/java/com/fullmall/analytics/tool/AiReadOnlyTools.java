package com.fullmall.analytics.tool;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * AI 只读工具集（N2）。
 *
 * <p>设计要点：
 * <ul>
 *   <li>**白名单**：只有登记过的工具名可调用，参数逐个校验；</li>
 *   <li>**只读**：全部是 SELECT，任何写操作都不在此层开放；</li>
 *   <li>不依赖模型：模型接入后由它选择工具，本层负责"安全执行"；</li>
 *   <li>返回结构化数据，便于模型引用与前端展示。</li>
 * </ul>
 */
@Service
public class AiReadOnlyTools {

    private final JdbcTemplate jdbc;

    public AiReadOnlyTools(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ToolSpec(String name, String description, List<String> args) {
    }

    public List<ToolSpec> list() {
        return List.of(
                new ToolSpec("order.status", "查询订单状态与金额", List.of("orderNo")),
                new ToolSpec("sku.availability", "查询 SKU 可售数量", List.of("skuCode")),
                new ToolSpec("sku.price", "查询 SKU 价格", List.of("skuCode")));
    }

    public Map<String, Object> invoke(String name, Map<String, String> args) {
        String tool = name == null ? "" : name.trim();
        Map<String, String> safeArgs = args == null ? Map.of() : args;
        return switch (tool) {
            case "order.status" -> orderStatus(require(safeArgs, "orderNo"));
            case "sku.availability" -> availability(require(safeArgs, "skuCode"));
            case "sku.price" -> price(require(safeArgs, "skuCode"));
            default -> throw ApiException.badRequest("AI_TOOL_NOT_ALLOWED",
                    "未登记的工具：" + name + "（只允许 " + list().stream().map(ToolSpec::name).toList() + "）");
        };
    }

    private String require(Map<String, String> args, String key) {
        String value = args.get(key);
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("AI_TOOL_ARG_REQUIRED", "缺少参数：" + key);
        }
        return value.trim();
    }

    private Map<String, Object> orderStatus(String orderNo) {
        var rows = jdbc.query("""
                SELECT order_no, status, total_amount, currency, created_at
                  FROM orders WHERE order_no = ?
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderNo", rs.getString("order_no"));
            row.put("status", rs.getString("status"));
            row.put("totalAmount", rs.getLong("total_amount"));
            row.put("currency", rs.getString("currency"));
            row.put("createdAt", rs.getTimestamp("created_at").toInstant().toString());
            return row;
        }, orderNo);
        if (rows.isEmpty()) {
            throw ApiException.notFound("ORDER_NOT_FOUND", "订单不存在：" + orderNo);
        }
        return rows.get(0);
    }

    private Map<String, Object> availability(String skuCode) {
        var rows = jdbc.query("""
                SELECT s.product_id, s.sku_code,
                       COALESCE(SUM(b.quantity - b.reserved_quantity), 0) AS available
                  FROM sku s
                  LEFT JOIN inventory_batch b ON b.sku_id = s.id
                                             AND b.status IN ('AVAILABLE', 'NEAR_EXPIRY')
                 WHERE s.sku_code = ?
                 GROUP BY s.product_id, s.sku_code
                """, (rs, rowNum) -> Map.<String, Object>of(
                "productId", rs.getObject("product_id", java.util.UUID.class).toString(),
                "skuCode", rs.getString("sku_code"),
                "available", rs.getLong("available")), skuCode);
        if (rows.isEmpty()) {
            throw ApiException.notFound("SKU_NOT_FOUND", "SKU 不存在：" + skuCode);
        }
        // SKU 编码只在商品内唯一：多个商品同码时要问清楚，不能随便挑一个回答
        if (rows.size() > 1) {
            throw ApiException.conflict("SKU_CODE_AMBIGUOUS",
                    "SKU 编码在 " + rows.size() + " 个商品下重复，请补充商品名：" + skuCode);
        }
        return rows.get(0);
    }

    private Map<String, Object> price(String skuCode) {
        var rows = jdbc.query("""
                SELECT product_id, sku_code, price_amount, price_currency
                  FROM sku WHERE sku_code = ?
                """, (rs, rowNum) -> Map.<String, Object>of(
                "productId", rs.getObject("product_id", java.util.UUID.class).toString(),
                "skuCode", rs.getString("sku_code"),
                "priceAmount", rs.getLong("price_amount"),
                "currency", rs.getString("price_currency")), skuCode);
        if (rows.isEmpty()) {
            throw ApiException.notFound("SKU_NOT_FOUND", "SKU 不存在：" + skuCode);
        }
        if (rows.size() > 1) {
            throw ApiException.conflict("SKU_CODE_AMBIGUOUS",
                    "SKU 编码在 " + rows.size() + " 个商品下重复，请补充商品名：" + skuCode);
        }
        return rows.get(0);
    }
}
