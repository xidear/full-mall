package com.fullmall.catalog.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.catalog.domain.InventoryTrackingLevel;
import com.fullmall.catalog.domain.Product;
import com.fullmall.catalog.domain.ProductStatus;
import com.fullmall.catalog.domain.ShelfLifePolicy;
import com.fullmall.catalog.domain.Sku;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class CatalogRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CatalogRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Product> findActive(String locale, String keyword, UUID categoryId, int limit, int offset) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String pattern = "%" + normalizedKeyword + "%";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.id, COALESCE(t.title, p.title) AS title,
                       COALESCE(t.description, p.description) AS description,
                       p.category_id, p.status, p.inventory_tracking_level, p.shelf_life_managed
                FROM product p
                LEFT JOIN product_translation t ON t.product_id = p.id AND t.locale = ?
                WHERE p.status = 'ACTIVE'
                  AND (CAST(? AS uuid) IS NULL OR p.category_id = CAST(? AS uuid))
                  AND (? = '' OR COALESCE(t.title, p.title) ILIKE ?)
                ORDER BY p.created_at DESC
                LIMIT ? OFFSET ?
                """, locale, categoryId, categoryId, normalizedKeyword, pattern, limit, offset);

        return rows.stream().map(this::mapProduct).toList();
    }

    public long countActive(String locale, String keyword, UUID categoryId) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String pattern = "%" + normalizedKeyword + "%";
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM product p
                LEFT JOIN product_translation t ON t.product_id = p.id AND t.locale = ?
                WHERE p.status = 'ACTIVE'
                  AND (CAST(? AS uuid) IS NULL OR p.category_id = CAST(? AS uuid))
                  AND (? = '' OR COALESCE(t.title, p.title) ILIKE ?)
                """, Long.class, locale, categoryId, categoryId, normalizedKeyword, pattern);
        if (count == null) {
            throw new IllegalStateException("Product count query returned null");
        }
        return count;
    }

    public List<Product> findAll(String locale, String keyword, int limit, int offset) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String pattern = "%" + normalizedKeyword + "%";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.id, COALESCE(t.title, p.title) AS title,
                       COALESCE(t.description, p.description) AS description,
                       p.category_id, p.status, p.inventory_tracking_level, p.shelf_life_managed
                FROM product p
                LEFT JOIN product_translation t ON t.product_id = p.id AND t.locale = ?
                WHERE (? = '' OR COALESCE(t.title, p.title) ILIKE ?)
                ORDER BY p.created_at DESC
                LIMIT ? OFFSET ?
                """, locale, normalizedKeyword, pattern, limit, offset);
        return rows.stream().map(this::mapProduct).toList();
    }

    public long countAll(String locale, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String pattern = "%" + normalizedKeyword + "%";
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM product p
                LEFT JOIN product_translation t ON t.product_id = p.id AND t.locale = ?
                WHERE (? = '' OR COALESCE(t.title, p.title) ILIKE ?)
                """, Long.class, locale, normalizedKeyword, pattern);
        if (count == null) {
            throw new IllegalStateException("Product count query returned null");
        }
        return count;
    }

    public void updateStatus(UUID productId, ProductStatus status) {
        int updated = jdbcTemplate.update("UPDATE product SET status = ? WHERE id = ?", status.name(), productId);
        if (updated != 1) {
            throw new IllegalStateException("Product not found: " + productId);
        }
    }

    public Product findProduct(UUID productId, String locale) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.id, COALESCE(t.title, p.title) AS title,
                       COALESCE(t.description, p.description) AS description,
                       p.category_id, p.status, p.inventory_tracking_level, p.shelf_life_managed
                FROM product p
                LEFT JOIN product_translation t ON t.product_id = p.id AND t.locale = ?
                WHERE p.id = ?
                """, locale, productId);
        return rows.stream().findFirst().map(this::mapProduct).orElse(null);
    }

    public Sku findSku(UUID skuId) {
        List<Sku> rows = jdbcTemplate.query("""
                SELECT id, product_id, sku_code, barcode, price_amount, price_currency,
                       compare_at_amount, compare_at_currency, options_json, image_url, shelf_life_policy_json
                FROM sku
                WHERE id = ?
                """, (rs, rowNum) -> mapSku(rs), skuId);
        return rows.stream().findFirst().orElse(null);
    }

    public List<Sku> findSkus(UUID productId) {
        return jdbcTemplate.query("""
                SELECT id, product_id, sku_code, barcode, price_amount, price_currency,
                       compare_at_amount, compare_at_currency, options_json, image_url, shelf_life_policy_json
                FROM sku
                WHERE product_id = ?
                ORDER BY sku_code
                """, (rs, rowNum) -> mapSku(rs), productId);
    }

    public void insertProduct(Product product) {
        jdbcTemplate.update("""
                INSERT INTO product (id, title, description, category_id, status,
                                     inventory_tracking_level, shelf_life_managed, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, now())
                """,
                product.id(),
                product.title(),
                product.description(),
                product.categoryId(),
                product.status().name(),
                product.inventoryTrackingLevel().name(),
                product.shelfLifeManaged());
    }

    public void insertSku(Sku sku) {
        jdbcTemplate.update("""
                INSERT INTO sku (id, product_id, sku_code, barcode, price_amount, price_currency,
                                 compare_at_amount, compare_at_currency, options_json, image_url,
                                 shelf_life_policy_json, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::jsonb, now())
                """,
                sku.id(),
                sku.productId(),
                sku.skuCode(),
                sku.barcode(),
                sku.price().amount(),
                sku.price().currency(),
                sku.compareAtPrice() == null ? null : sku.compareAtPrice().amount(),
                sku.compareAtPrice() == null ? null : sku.compareAtPrice().currency(),
                writeJson(sku.options()),
                sku.imageUrl(),
                writeJson(sku.shelfLifePolicy()));
    }

    private Product mapProduct(Map<String, Object> row) {
        UUID id = (UUID) row.get("id");
        UUID categoryId = (UUID) row.get("category_id");
        return new Product(
                id,
                (String) row.get("title"),
                (String) row.get("description"),
                categoryId,
                ProductStatus.valueOf((String) row.get("status")),
                InventoryTrackingLevel.valueOf((String) row.get("inventory_tracking_level")),
                (Boolean) row.get("shelf_life_managed"),
                findSkus(id));
    }

    private Sku mapSku(java.sql.ResultSet rs) throws java.sql.SQLException {
        Long compareAtAmount = rs.getObject("compare_at_amount", Long.class);
        String compareAtCurrency = rs.getString("compare_at_currency");
        Money compareAt = compareAtAmount == null ? null : new Money(compareAtAmount, compareAtCurrency);
        return new Sku(
                rs.getObject("id", UUID.class),
                rs.getObject("product_id", UUID.class),
                rs.getString("sku_code"),
                rs.getString("barcode"),
                new Money(rs.getLong("price_amount"), rs.getString("price_currency")),
                compareAt,
                readMap(rs.getString("options_json")),
                rs.getString("image_url"),
                readPolicy(rs.getString("shelf_life_policy_json")));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize catalog JSON", exception);
        }
    }

    private Map<String, String> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse SKU options JSON", exception);
        }
    }

    private ShelfLifePolicy readPolicy(String json) {
        if (json == null || json.isBlank()) {
            return ShelfLifePolicy.none();
        }
        try {
            return objectMapper.readValue(json, ShelfLifePolicy.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse shelf life policy JSON", exception);
        }
    }
}