package com.fullmall.store.infra;

import com.fullmall.store.domain.Store;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class StoreRepository {

    /** 列清单只写一次：新增字段时漏改某个 SELECT 会在 map() 里抛"列不存在"，比静默少字段安全。 */
    private static final String COLUMNS = """
            id, code, market_code, name, address, latitude, longitude,
            pickup_enabled, active, phone, opening_hours, created_at
            """;

    private static final RowMapper<Store> MAPPER = (rs, rowNum) -> new Store(
            rs.getObject("id", UUID.class),
            rs.getString("code"),
            rs.getString("market_code"),
            rs.getString("name"),
            rs.getString("address"),
            rs.getBigDecimal("latitude"),
            rs.getBigDecimal("longitude"),
            rs.getBoolean("pickup_enabled"),
            rs.getBoolean("active"),
            rs.getString("phone"),
            rs.getString("opening_hours"),
            rs.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbcTemplate;

    public StoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Store> findPickupStores() {
        return jdbcTemplate.query("SELECT " + COLUMNS + """
                FROM store
                WHERE pickup_enabled = true AND active = true
                ORDER BY name
                """, MAPPER);
    }

    public List<Store> findPickupStoresWithStock(UUID skuId, LocalDate minimumExpiry) {
        return jdbcTemplate.query("""
                SELECT s.id, s.code, s.market_code, s.name, s.address, s.latitude, s.longitude,
                       s.pickup_enabled, s.active, s.phone, s.opening_hours, s.created_at
                FROM store s
                WHERE s.pickup_enabled = true AND s.active = true
                  AND EXISTS (
                    SELECT 1 FROM inventory_batch b
                    WHERE b.location_type = 'STORE'
                      AND b.location_id = s.id
                      AND b.sku_id = ?
                      AND b.status IN ('AVAILABLE', 'NEAR_EXPIRY')
                      AND b.quantity > b.reserved_quantity
                      AND (b.expiry_date IS NULL OR b.expiry_date >= ?)
                  )
                ORDER BY s.name
                """, MAPPER, skuId, minimumExpiry);
    }

    public List<Store> findAll() {
        return findAll(false);
    }

    /** 后台列表需要看到已停用门店；默认只返回营业中的门店，避免门店选择器出现停用项。 */
    public List<Store> findAll(boolean includeInactive) {
        return jdbcTemplate.query("SELECT " + COLUMNS + """
                FROM store
                WHERE (?::boolean OR active = true)
                ORDER BY active DESC, name
                """, MAPPER, includeInactive);
    }

    public Store findById(UUID storeId) {
        return findById(storeId, false);
    }

    public Store findById(UUID storeId, boolean includeInactive) {
        List<Store> stores = jdbcTemplate.query("SELECT " + COLUMNS + """
                FROM store
                WHERE id = ? AND (?::boolean OR active = true)
                """, MAPPER, storeId, includeInactive);
        return stores.stream().findFirst().orElse(null);
    }

    public boolean codeExists(String code, UUID excludeId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM store
                 WHERE code = ? AND (?::uuid IS NULL OR id <> ?::uuid)
                """, Integer.class, code, excludeId, excludeId);
        return count != null && count > 0;
    }

    public boolean marketExists(String marketCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM market WHERE code = ?", Integer.class, marketCode);
        return count != null && count > 0;
    }

    public void insert(Store store) {
        jdbcTemplate.update("""
                INSERT INTO store (id, code, market_code, name, address, latitude, longitude,
                                   pickup_enabled, active, phone, opening_hours, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """, store.id(), store.code(), store.marketCode(), store.name(), store.address(),
                store.latitude(), store.longitude(), store.pickupEnabled(), store.active(),
                store.phone(), store.openingHours(), Timestamp.from(store.createdAt()));
    }

    /** 只改可编辑字段：编码唯一性由 DB 约束兜底，服务层先给出可读错误。 */
    public int update(Store store) {
        return jdbcTemplate.update("""
                UPDATE store
                   SET code = ?, market_code = ?, name = ?, address = ?, latitude = ?,
                       longitude = ?, pickup_enabled = ?, phone = ?, opening_hours = ?, updated_at = now()
                 WHERE id = ?
                """, store.code(), store.marketCode(), store.name(), store.address(),
                store.latitude(), store.longitude(), store.pickupEnabled(), store.phone(),
                store.openingHours(), store.id());
    }

    public int updateActive(UUID storeId, boolean active) {
        return jdbcTemplate.update(
                "UPDATE store SET active = ?, updated_at = now() WHERE id = ?", active, storeId);
    }
}