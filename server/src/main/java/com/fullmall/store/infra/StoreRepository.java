package com.fullmall.store.infra;

import com.fullmall.store.domain.Store;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class StoreRepository {

    private final JdbcTemplate jdbcTemplate;

    public StoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Store> findPickupStores() {
        return jdbcTemplate.query("""
                SELECT id, name, address, latitude, longitude, pickup_enabled, phone, opening_hours
                FROM store
                WHERE pickup_enabled = true AND active = true
                ORDER BY name
                """, (rs, rowNum) -> map(rs));
    }

    public List<Store> findPickupStoresWithStock(UUID skuId, LocalDate minimumExpiry) {
        return jdbcTemplate.query("""
                SELECT s.id, s.name, s.address, s.latitude, s.longitude, s.pickup_enabled, s.phone, s.opening_hours
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
                """, (rs, rowNum) -> map(rs), skuId, minimumExpiry);
    }

    public Store findById(UUID storeId) {
        List<Store> stores = jdbcTemplate.query("""
                SELECT id, name, address, latitude, longitude, pickup_enabled, phone, opening_hours
                FROM store
                WHERE id = ? AND active = true
                """, (rs, rowNum) -> map(rs), storeId);
        return stores.stream().findFirst().orElse(null);
    }

    private Store map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Store(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("address"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getBoolean("pickup_enabled"),
                rs.getString("phone"),
                rs.getString("opening_hours"));
    }
}