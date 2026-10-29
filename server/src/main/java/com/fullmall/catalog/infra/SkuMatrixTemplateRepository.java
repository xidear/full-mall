package com.fullmall.catalog.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 规格矩阵模板仓储（ADR-006）。
 *
 * <p>模板只保存**维度 + 取值 + 禁用组合**，不保存价格：模板尚未与商品绑定，
 * 价格属于"某商品某 SKU"的事实，写在模板上会让运营误以为价格会跟着模板走。
 */
@Repository
public class SkuMatrixTemplateRepository {

    public record Row(UUID id, String code, String name, String note,
                      LinkedHashMap<String, List<String>> dimensions,
                      List<Map<String, String>> disabled,
                      Instant updatedAt) {
    }

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public SkuMatrixTemplateRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<Row> findAll() {
        List<Row> rows = jdbc.query("""
                SELECT id, code, name, note, updated_at
                  FROM sku_matrix_template
                 ORDER BY updated_at DESC, name
                """, (rs, rowNum) -> new Row(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("note"),
                new LinkedHashMap<>(),
                List.of(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toInstant()));
        return rows.stream().map(row -> withDimensions(row)).toList();
    }

    public Optional<Row> find(UUID id) {
        List<Row> rows = jdbc.query("""
                SELECT id, code, name, note, updated_at
                  FROM sku_matrix_template WHERE id = ?
                """, (rs, rowNum) -> new Row(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("note"),
                new LinkedHashMap<>(),
                List.of(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toInstant()), id);
        return rows.stream().findFirst().map(row -> withDimensions(row));
    }

    private Row withDimensions(Row row) {
        LinkedHashMap<String, List<String>> dimensions = new LinkedHashMap<>();
        jdbc.query("""
                SELECT id, name FROM sku_matrix_dimension
                 WHERE template_id = ? ORDER BY sort_order, name
                """, rs -> {
            String dimensionId = rs.getString("id");
            String name = rs.getString("name");
            List<String> values = jdbc.query("""
                    SELECT name FROM sku_matrix_dimension_value
                     WHERE dimension_id = ? ORDER BY sort_order, name
                    """, (valueRs, valueRow) -> valueRs.getString("name"), UUID.fromString(dimensionId));
            dimensions.put(name, new ArrayList<>(values));
        }, row.id());

        List<Map<String, String>> disabled = jdbc.query("""
                SELECT combination_json FROM sku_matrix_disabled WHERE template_id = ?
                """, (rs, rowNum) -> readCombination(rs.getString("combination_json")), row.id());
        return new Row(row.id(), row.code(), row.name(), row.note(), dimensions, disabled, row.updatedAt());
    }

    public boolean existsByCode(String code, UUID excludeId) {
        Integer count = excludeId == null
                ? jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sku_matrix_template WHERE lower(code) = lower(?)",
                        Integer.class, code)
                : jdbc.queryForObject("""
                        SELECT COUNT(*) FROM sku_matrix_template WHERE lower(code) = lower(?) AND id <> ?
                        """, Integer.class, code, excludeId);
        return count != null && count > 0;
    }

    public void insert(UUID id, String code, String name, String note,
                       LinkedHashMap<String, List<String>> dimensions,
                       List<Map<String, String>> disabled) {
        jdbc.update("""
                INSERT INTO sku_matrix_template (id, code, name, note)
                VALUES (?, ?, ?, ?)
                """, id, code, name, note);
        writeDimensions(id, dimensions);
        writeDisabled(id, disabled);
    }

    public void update(UUID id, String code, String name, String note,
                       LinkedHashMap<String, List<String>> dimensions,
                       List<Map<String, String>> disabled) {
        int updated = jdbc.update("""
                UPDATE sku_matrix_template SET code = ?, name = ?, note = ?, updated_at = now()
                 WHERE id = ?
                """, code, name, note, id);
        if (updated != 1) {
            throw new IllegalStateException("规格矩阵模板更新失败: id=" + id + "，影响行数=" + updated);
        }
        jdbc.update("DELETE FROM sku_matrix_dimension WHERE template_id = ?", id);
        jdbc.update("DELETE FROM sku_matrix_disabled WHERE template_id = ?", id);
        writeDimensions(id, dimensions);
        writeDisabled(id, disabled);
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM sku_matrix_template WHERE id = ?", id);
    }

    /** 被多少商品引用：删除前必须告知，不能"静默解绑"。 */
    public int productCount(UUID id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM product WHERE sku_matrix_template_id = ?", Integer.class, id);
        return count == null ? 0 : count;
    }

    public void bindProduct(UUID productId, UUID templateId) {
        int updated = jdbc.update(
                "UPDATE product SET sku_matrix_template_id = ? WHERE id = ?", templateId, productId);
        if (updated != 1) {
            throw new IllegalStateException(
                    "商品绑定规格矩阵失败: product=" + productId + "，影响行数=" + updated);
        }
    }

    public Optional<UUID> productTemplate(UUID productId) {
        List<UUID> rows = jdbc.query(
                "SELECT sku_matrix_template_id FROM product WHERE id = ? AND sku_matrix_template_id IS NOT NULL",
                (rs, rowNum) -> rs.getObject("sku_matrix_template_id", UUID.class), productId);
        return rows.stream().findFirst();
    }

    private void writeDimensions(UUID templateId, LinkedHashMap<String, List<String>> dimensions) {
        int dimensionOrder = 0;
        for (Map.Entry<String, List<String>> entry : dimensions.entrySet()) {
            UUID dimensionId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO sku_matrix_dimension (id, template_id, name, sort_order)
                    VALUES (?, ?, ?, ?)
                    """, dimensionId, templateId, entry.getKey(), dimensionOrder++);
            int valueOrder = 0;
            for (String value : entry.getValue()) {
                jdbc.update("""
                        INSERT INTO sku_matrix_dimension_value (id, dimension_id, name, sort_order)
                        VALUES (?, ?, ?, ?)
                        """, UUID.randomUUID(), dimensionId, value, valueOrder++);
            }
        }
    }

    private void writeDisabled(UUID templateId, List<Map<String, String>> disabled) {
        if (disabled == null) {
            return;
        }
        for (Map<String, String> combination : disabled) {
            jdbc.update("""
                    INSERT INTO sku_matrix_disabled (template_id, combination_json)
                    VALUES (?, ?::jsonb) ON CONFLICT DO NOTHING
                    """, templateId, writeCombination(combination));
        }
    }

    private String writeCombination(Map<String, String> combination) {
        try {
            return objectMapper.writeValueAsString(combination);
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException("禁用组合序列化失败: " + combination, error);
        }
    }

    private Map<String, String> readCombination(String json) {
        try {
            Map<String, String> parsed = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() { });
            return new LinkedHashMap<>(parsed);
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException("禁用组合解析失败: " + json, error);
        }
    }

    /** 供服务层去重：同一组合只保留一份（大小写无关的键值对）。 */
    public static List<Map<String, String>> distinct(List<Map<String, String>> combinations) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> combination : combinations) {
            LinkedHashMap<String, String> sorted = new LinkedHashMap<>(combination);
            String key = sorted.toString();
            if (seen.add(key)) {
                result.add(sorted);
            }
        }
        return result;
    }
}
