package com.fullmall.distribution.infra;

import com.fullmall.distribution.api.DistributionDtos.CommissionView;
import com.fullmall.distribution.api.DistributionDtos.ReferralBindingView;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class DistributionRepository {

    private final JdbcTemplate jdbcTemplate;

    public DistributionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deactivateBindings(String phone) {
        jdbcTemplate.update("UPDATE referral_binding SET active = false WHERE customer_phone = ? AND active = true", phone);
    }

    public void insertBinding(String phone, UUID promoterId, String mode, String source, Instant expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO referral_binding (id, customer_phone, promoter_staff_id, mode, source, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), phone, promoterId, mode, source,
                expiresAt == null ? null : Timestamp.from(expiresAt));
    }

    public ReferralBindingView findActiveBinding(String phone) {
        List<ReferralBindingView> rows = jdbcTemplate.query("""
                SELECT id, customer_phone, promoter_staff_id, mode, source, active, created_at, expires_at
                FROM referral_binding
                WHERE customer_phone = ? AND active = true
                  AND (expires_at IS NULL OR expires_at > now())
                ORDER BY CASE mode WHEN 'PERMANENT' THEN 1 WHEN 'REGISTER' THEN 2 WHEN 'VISIT' THEN 3 ELSE 4 END,
                         created_at DESC
                LIMIT 1
                """, (rs, rowNum) -> new ReferralBindingView(
                rs.getObject("id", UUID.class), rs.getString("customer_phone"),
                rs.getObject("promoter_staff_id", UUID.class), rs.getString("mode"),
                rs.getString("source"), rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant()), phone);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<ReferralBindingView> listBindings(String phone) {
        return jdbcTemplate.query("""
                SELECT id, customer_phone, promoter_staff_id, mode, source, active, created_at, expires_at
                FROM referral_binding WHERE customer_phone = ?
                ORDER BY created_at DESC
                """, (rs, rowNum) -> new ReferralBindingView(
                rs.getObject("id", UUID.class), rs.getString("customer_phone"),
                rs.getObject("promoter_staff_id", UUID.class), rs.getString("mode"),
                rs.getString("source"), rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant()), phone);
    }

    public void attachOrder(UUID orderId, UUID promoterId, String mode) {
        jdbcTemplate.update("""
                UPDATE orders SET referrer_staff_id = ?, referral_mode = ? WHERE id = ?
                """, promoterId, mode, orderId);
    }

    public UUID orderReferrer(UUID orderId) {
        List<UUID> rows = jdbcTemplate.query("""
                SELECT referrer_staff_id FROM orders WHERE id = ?
                """, (rs, rowNum) -> rs.getObject("referrer_staff_id", UUID.class), orderId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public String orderReferralMode(UUID orderId) {
        List<String> rows = jdbcTemplate.query("""
                SELECT referral_mode FROM orders WHERE id = ?
                """, (rs, rowNum) -> rs.getString("referral_mode"), orderId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertCommission(UUID orderId, UUID promoterId, long orderAmount, long commission,
                                 String currency) {
        jdbcTemplate.update("""
                INSERT INTO distribution_commission (id, order_id, promoter_staff_id, order_amount,
                                                     commission_amount, currency, status)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING')
                ON CONFLICT (order_id, promoter_staff_id) DO NOTHING
                """, UUID.randomUUID(), orderId, promoterId, orderAmount, commission, currency);
    }

    public List<CommissionView> listCommissions(UUID promoterId, String status) {
        String filter = status == null ? "" : status.trim().toUpperCase();
        List<Object> args = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.order_id, o.order_no, c.promoter_staff_id, u.display_name AS promoter_name,
                       c.order_amount, c.commission_amount, c.currency, c.status, c.created_at, c.settled_at
                FROM distribution_commission c
                JOIN orders o ON o.id = c.order_id
                LEFT JOIN staff_user u ON u.id = c.promoter_staff_id
                WHERE (? = '' OR c.status = ?)
                """);
        args.add(filter);
        args.add(filter);
        if (promoterId != null) {
            sql.append(" AND c.promoter_staff_id = ?");
            args.add(promoterId);
        }
        sql.append(" ORDER BY c.created_at DESC LIMIT 200");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new CommissionView(
                rs.getObject("id", UUID.class), rs.getObject("order_id", UUID.class),
                rs.getString("order_no"), rs.getObject("promoter_staff_id", UUID.class),
                rs.getString("promoter_name"),
                new Money(rs.getLong("order_amount"), rs.getString("currency")),
                new Money(rs.getLong("commission_amount"), rs.getString("currency")),
                rs.getString("status"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("settled_at") == null ? null : rs.getTimestamp("settled_at").toInstant()),
                args.toArray());
    }

    public void settle(UUID commissionId) {
        jdbcTemplate.update("""
                UPDATE distribution_commission SET status = 'SETTLED', settled_at = now()
                WHERE id = ? AND status = 'PENDING'
                """, commissionId);
    }
}
