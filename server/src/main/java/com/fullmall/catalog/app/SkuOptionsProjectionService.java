package com.fullmall.catalog.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 规格投影重建（ADR-003 第 3 步）：以**关联表为真相**，把 `sku.options_json` 重算一遍。
 *
 * <p>为什么保留 JSON：商品详情/前台已有读取路径依赖它；把它降级为**投影**，
 * 由关联表单向生成，就不会出现"两份真相不一致"。
 */
@Service
public class SkuOptionsProjectionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbc;

    public SkuOptionsProjectionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 重建某商品下所有 SKU 的 options_json；返回更新条数。 */
    @Transactional
    public int rebuild(UUID productId) {
        List<UUID> skuIds = jdbc.query("SELECT id FROM sku WHERE product_id = ?",
                (rs, rowNum) -> rs.getObject("id", UUID.class), productId);
        int updated = 0;
        for (UUID skuId : skuIds) {
            Map<String, String> options = new LinkedHashMap<>();
            jdbc.query("""
                    SELECT o.name AS option_name, ov.name AS option_value
                      FROM sku_option_value v
                      JOIN product_option o ON o.id = v.option_id
                      JOIN product_option_value ov ON ov.id = v.option_value_id
                     WHERE v.sku_id = ?
                     ORDER BY o.sort_order, o.name
                    """, rs -> {
                options.put(rs.getString("option_name"), rs.getString("option_value"));
            }, skuId);
            String json;
            try {
                json = MAPPER.writeValueAsString(options);
            } catch (Exception error) {
                throw new IllegalStateException("规格投影序列化失败：sku=" + skuId, error);
            }
            updated += jdbc.update("UPDATE sku SET options_json = ?::jsonb WHERE id = ?", json, skuId);
        }
        return updated;
    }
}
