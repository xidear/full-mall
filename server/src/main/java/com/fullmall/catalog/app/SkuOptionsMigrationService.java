package com.fullmall.catalog.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 把既有 `sku.options_json` 回填到关联表（ADR-003 第 2 步）。
 *
 * <p>幂等：维度/取值按名称复用，SKU 关联冲突则跳过；可反复执行。
 * 回填后**不改动** options_json（双写阶段仍以 JSON 为准），待读写切换完成再删。
 */
@Service
public class SkuOptionsMigrationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbc;

    public SkuOptionsMigrationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Result(int skus, int dimensions, int values) {
    }

    @Transactional
    public Result backfillProduct(UUID productId) {
        List<Map<String, Object>> skus = jdbc.query("""
                SELECT id, options_json::text AS options FROM sku
                 WHERE product_id = ? AND options_json <> '{}'::jsonb
                 ORDER BY sku_code
                """, (rs, rowNum) -> Map.of(
                "id", rs.getObject("id", UUID.class),
                "options", rs.getString("options")), productId);

        int dimensions = 0;
        int values = 0;
        for (Map<String, Object> row : skus) {
            UUID skuId = (UUID) row.get("id");
            Map<String, String> options;
            try {
                options = MAPPER.readValue((String) row.get("options"),
                        new TypeReference<LinkedHashMap<String, String>>() {
                        });
            } catch (Exception error) {
                throw new IllegalStateException("解析 options_json 失败：sku=" + skuId, error);
            }
            List<UUID> valueIds = new ArrayList<>();
            for (var entry : options.entrySet()) {
                UUID optionId = ensureOption(productId, entry.getKey());
                dimensions++;
                UUID valueId = ensureValue(optionId, entry.getValue());
                values++;
                valueIds.add(valueId);
                jdbc.update("""
                        INSERT INTO sku_option_value (sku_id, option_id, option_value_id)
                        VALUES (?, ?, ?) ON CONFLICT (sku_id, option_id) DO NOTHING
                        """, skuId, optionId, valueId);
            }
            valueIds.sort(UUID::compareTo);
            StringBuilder key = new StringBuilder();
            for (UUID valueId : valueIds) {
                key.append(valueId);
            }
            jdbc.update("UPDATE sku SET combination_key = ? WHERE id = ? AND combination_key IS NULL",
                    key.toString(), skuId);
        }
        return new Result(skus.size(), dimensions, values);
    }

    private UUID ensureOption(UUID productId, String name) {
        var ids = jdbc.query("SELECT id FROM product_option WHERE product_id = ? AND name = ?",
                (rs, rowNum) -> rs.getObject("id", UUID.class), productId, name);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO product_option (id, product_id, name) VALUES (?, ?, ?)",
                id, productId, name);
        return id;
    }

    private UUID ensureValue(UUID optionId, String name) {
        var ids = jdbc.query("SELECT id FROM product_option_value WHERE option_id = ? AND name = ?",
                (rs, rowNum) -> rs.getObject("id", UUID.class), optionId, name);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO product_option_value (id, option_id, name) VALUES (?, ?, ?)",
                id, optionId, name);
        return id;
    }
}
