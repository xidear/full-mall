package com.fullmall.supplier.infra;

import com.fullmall.supplier.api.SupplierCredentialView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class SupplierRepository {

    private final JdbcTemplate jdbcTemplate;

    public SupplierRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CredentialRow findCredential(String accessKey) {
        List<CredentialRow> rows = jdbcTemplate.query("""
                SELECT supplier_id, secret_encrypted, active
                FROM supplier_credential
                WHERE access_key = ?
                """, (rs, rowNum) -> new CredentialRow(
                rs.getObject("supplier_id", UUID.class),
                rs.getString("secret_encrypted"),
                rs.getBoolean("active")), accessKey);
        return rows.stream().findFirst().orElse(null);
    }

    public void upsertCredential(UUID supplierId, String accessKey, String secretEncrypted) {
        jdbcTemplate.update("""
                INSERT INTO supplier_credential (id, supplier_id, access_key, secret_encrypted, active, rotated_at)
                VALUES (?, ?, ?, ?, TRUE, now())
                ON CONFLICT (supplier_id) DO UPDATE
                SET access_key = EXCLUDED.access_key,
                    secret_encrypted = EXCLUDED.secret_encrypted,
                    active = TRUE,
                    rotated_at = now()
                """, UUID.randomUUID(), supplierId, accessKey, secretEncrypted);
    }

    public List<SupplierCredentialView> listSuppliers() {
        return jdbcTemplate.query("""
                SELECT s.id, s.code, s.name, s.active, c.access_key, c.rotated_at
                FROM supplier s
                LEFT JOIN supplier_credential c ON c.supplier_id = s.id
                ORDER BY s.name
                """, (rs, rowNum) -> new SupplierCredentialView(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getBoolean("active"),
                rs.getString("access_key"),
                rs.getString("access_key") != null,
                instant(rs, "rotated_at")));
    }

    public String supplierName(UUID supplierId) {
        List<String> names = jdbcTemplate.query("""
                SELECT name FROM supplier WHERE id = ?
                """, (rs, rowNum) -> rs.getString("name"), supplierId);
        return names.isEmpty() ? null : names.get(0);
    }

    public boolean supplierExists(UUID supplierId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM supplier WHERE id = ? AND active = true
                """, Integer.class, supplierId);
        return count != null && count > 0;
    }

    public List<OrderHeader> findOrders(UUID supplierId, String status) {
        String effectiveStatus = status == null ? "" : status.trim().toUpperCase();
        return jdbcTemplate.query("""
                SELECT f.id AS fulfillment_id, f.order_id, f.status, f.created_at, f.supplier_accepted_at,
                       f.carrier_code, f.tracking_no, f.supplier_note,
                       o.order_no, o.contact_name, o.contact_phone, o.address_json,
                       o.total_amount, o.currency
                FROM fulfillment_order f
                JOIN orders o ON o.id = f.order_id
                WHERE f.type = 'SUPPLIER_DROPSHIP'
                  AND f.source_id = ?
                  AND (? = '' OR f.status = ?)
                ORDER BY f.created_at DESC
                LIMIT 100
                """, (rs, rowNum) -> new OrderHeader(
                rs.getObject("fulfillment_id", UUID.class),
                rs.getObject("order_id", UUID.class),
                rs.getString("order_no"),
                rs.getString("status"),
                instant(rs, "created_at"),
                instant(rs, "supplier_accepted_at"),
                rs.getString("carrier_code"),
                rs.getString("tracking_no"),
                rs.getString("contact_name"),
                rs.getString("contact_phone"),
                rs.getString("address_json"),
                rs.getLong("total_amount"),
                rs.getString("currency"),
                rs.getString("supplier_note")), supplierId, effectiveStatus, effectiveStatus);
    }

    public List<OrderLineRow> findOrderLines(UUID fulfillmentId, UUID supplierId) {
        return jdbcTemplate.query("""
                SELECT fl.sku_id, ol.title_snapshot, ol.sku_code_snapshot, fl.quantity,
                       ol.unit_price_amount, ol.currency, ss.supplier_sku_code
                FROM fulfillment_line fl
                JOIN order_line ol ON ol.id = fl.order_line_id
                LEFT JOIN supplier_sku ss ON ss.supplier_id = ? AND ss.sku_id = fl.sku_id
                WHERE fl.fulfillment_order_id = ?
                ORDER BY fl.id
                """, (rs, rowNum) -> new OrderLineRow(
                rs.getObject("sku_id", UUID.class),
                rs.getString("title_snapshot"),
                rs.getString("sku_code_snapshot"),
                rs.getString("supplier_sku_code"),
                rs.getInt("quantity"),
                rs.getLong("unit_price_amount"),
                rs.getString("currency")), supplierId, fulfillmentId);
    }

    public SupplierSkuRow findSupplierSku(UUID supplierId, String supplierSkuCode) {
        List<SupplierSkuRow> rows = jdbcTemplate.query("""
                SELECT ss.sku_id, ss.supplier_sku_code, ss.active
                FROM supplier_sku ss
                WHERE ss.supplier_id = ? AND ss.supplier_sku_code = ?
                """, (rs, rowNum) -> new SupplierSkuRow(
                rs.getObject("sku_id", UUID.class),
                rs.getString("supplier_sku_code"),
                rs.getBoolean("active")), supplierId, supplierSkuCode);
        return rows.stream().findFirst().orElse(null);
    }

    public void updateSupplierSkuPrice(UUID supplierId, UUID skuId, long priceAmount, String currency) {
        jdbcTemplate.update("""
                UPDATE supplier_sku SET price_amount = ?, price_currency = ?
                WHERE supplier_id = ? AND sku_id = ?
                """, priceAmount, currency, supplierId, skuId);
    }

    public void upsertStockFeed(UUID supplierId, UUID skuId, String supplierSkuCode, int quantity,
                                Long priceAmount, String priceCurrency) {
        jdbcTemplate.update("""
                INSERT INTO supplier_stock_feed (id, supplier_id, sku_id, supplier_sku_code,
                                                 available_quantity, price_amount, price_currency, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (supplier_id, sku_id) DO UPDATE
                SET supplier_sku_code = EXCLUDED.supplier_sku_code,
                    available_quantity = EXCLUDED.available_quantity,
                    price_amount = EXCLUDED.price_amount,
                    price_currency = EXCLUDED.price_currency,
                    updated_at = now()
                """, UUID.randomUUID(), supplierId, skuId, supplierSkuCode, quantity, priceAmount, priceCurrency);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    public record CredentialRow(UUID supplierId, String secretEncrypted, boolean active) {
    }

    public record OrderHeader(
            UUID fulfillmentId,
            UUID orderId,
            String orderNo,
            String status,
            Instant createdAt,
            Instant acceptedAt,
            String carrierCode,
            String trackingNo,
            String contactName,
            String contactPhone,
            String addressJson,
            long totalAmount,
            String currency,
            String note
    ) {
    }

    public record OrderLineRow(
            UUID skuId,
            String title,
            String skuCode,
            String supplierSkuCode,
            int quantity,
            long unitPrice,
            String currency
    ) {
    }

    public record SupplierSkuRow(UUID skuId, String supplierSkuCode, boolean active) {
    }
}