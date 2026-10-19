package com.fullmall.promotion.infra;

import com.fullmall.platform.money.Money;
import com.fullmall.promotion.domain.GroupBuy;
import com.fullmall.promotion.domain.GroupBuyMember;
import com.fullmall.promotion.domain.Promotion;
import com.fullmall.promotion.domain.PromotionClaim;
import com.fullmall.promotion.domain.PromotionStatus;
import com.fullmall.promotion.domain.PromotionType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PromotionRepository {

    private final JdbcTemplate jdbcTemplate;

    public PromotionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Promotion> findActive(Instant now) {
        return jdbcTemplate.query("""
                SELECT id, code, type, title, description, status, starts_at, ends_at, sku_id,
                       promo_price_amount, gift_sku_id, buy_quantity, gift_quantity, max_gift_per_order,
                       stock_limit, sold_quantity, per_user_limit, group_size, group_duration_hours,
                       priority, created_at
                FROM promotion
                WHERE status = 'ACTIVE' AND starts_at <= ? AND ends_at > ?
                ORDER BY priority DESC, starts_at
                """, this::mapPromotion, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
    }

    public List<Promotion> findAll() {
        return jdbcTemplate.query("""
                SELECT id, code, type, title, description, status, starts_at, ends_at, sku_id,
                       promo_price_amount, gift_sku_id, buy_quantity, gift_quantity, max_gift_per_order,
                       stock_limit, sold_quantity, per_user_limit, group_size, group_duration_hours,
                       priority, created_at
                FROM promotion
                ORDER BY created_at DESC
                """, this::mapPromotion);
    }

    public Optional<Promotion> find(UUID id) {
        return jdbcTemplate.query("""
                SELECT id, code, type, title, description, status, starts_at, ends_at, sku_id,
                       promo_price_amount, gift_sku_id, buy_quantity, gift_quantity, max_gift_per_order,
                       stock_limit, sold_quantity, per_user_limit, group_size, group_duration_hours,
                       priority, created_at
                FROM promotion
                WHERE id = ?
                """, this::mapPromotion, id).stream().findFirst();
    }

    public List<Promotion> findActiveByType(PromotionType type, Instant now) {
        return jdbcTemplate.query("""
                SELECT id, code, type, title, description, status, starts_at, ends_at, sku_id,
                       promo_price_amount, gift_sku_id, buy_quantity, gift_quantity, max_gift_per_order,
                       stock_limit, sold_quantity, per_user_limit, group_size, group_duration_hours,
                       priority, created_at
                FROM promotion
                WHERE type = ? AND status = 'ACTIVE' AND starts_at <= ? AND ends_at > ?
                ORDER BY priority DESC, starts_at
                """, this::mapPromotion, type.name(),
                java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
    }

    public void insert(Promotion promotion) {
        jdbcTemplate.update("""
                INSERT INTO promotion (id, code, type, title, description, status, starts_at, ends_at,
                                       sku_id, promo_price_amount, gift_sku_id, buy_quantity, gift_quantity,
                                       max_gift_per_order, stock_limit, sold_quantity, per_user_limit,
                                       group_size, group_duration_hours, priority, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, now())
                """,
                promotion.id(),
                promotion.code(),
                promotion.type().name(),
                promotion.title(),
                promotion.description(),
                promotion.status().name(),
                java.sql.Timestamp.from(promotion.startsAt()),
                java.sql.Timestamp.from(promotion.endsAt()),
                promotion.skuId(),
                promotion.promoPriceAmount(),
                promotion.giftSkuId(),
                promotion.buyQuantity(),
                promotion.giftQuantity(),
                promotion.maxGiftPerOrder(),
                promotion.stockLimit(),
                promotion.perUserLimit(),
                promotion.groupSize(),
                promotion.groupDurationHours(),
                promotion.priority());
    }

    public void updateStatus(UUID id, PromotionStatus status) {
        int updated = jdbcTemplate.update("UPDATE promotion SET status = ? WHERE id = ?", status.name(), id);
        if (updated != 1) {
            throw new IllegalStateException("Promotion not found while updating status: " + id);
        }
    }

    public void lockGroup(UUID groupBuyId) {
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(hashtext(?))",
                (org.springframework.jdbc.core.PreparedStatementCallback<Void>) statement -> {
                    statement.setString(1, "group-buy:" + groupBuyId);
                    statement.execute();
                    return null;
                });
    }

    public void lockGroupCode(String code) {
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(hashtext(?))",
                (org.springframework.jdbc.core.PreparedStatementCallback<Void>) statement -> {
                    statement.setString(1, "group-buy-code:" + code);
                    statement.execute();
                    return null;
                });
    }

    public void lockUser(UUID promotionId, String phone) {
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(hashtext(?))",
                (org.springframework.jdbc.core.PreparedStatementCallback<Void>) statement -> {
                    statement.setString(1, "promotion-user:" + promotionId + ":" + phone);
                    statement.execute();
                    return null;
                });
    }

    public int countUserRedemptions(UUID promotionId, String phone) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(sale_quantity), 0)
                FROM promotion_redemption
                WHERE promotion_id = ? AND member_phone = ?
                """, Integer.class, promotionId, phone);
        return count == null ? 0 : count;
    }

    public void claimStock(UUID promotionId, int quantity, int stockLimit) {
        int updated = jdbcTemplate.update("""
                UPDATE promotion
                SET sold_quantity = sold_quantity + ?
                WHERE id = ? AND sold_quantity + ? <= ?
                """, quantity, promotionId, quantity, stockLimit);
        if (updated != 1) {
            throw new com.fullmall.platform.error.ApiException(org.springframework.http.HttpStatus.CONFLICT,
                    "PROMOTION_SOLD_OUT",
                    "促销名额已用完，请刷新活动后重试");
        }
    }

    public void claimUnlimited(UUID promotionId, int quantity) {
        jdbcTemplate.update("""
                UPDATE promotion SET sold_quantity = sold_quantity + ? WHERE id = ?
                """, quantity, promotionId);
    }

    public void insertRedemption(UUID promotionId, UUID orderId, String phone,
                                 int saleQuantity, int giftQuantity) {
        jdbcTemplate.update("""
                INSERT INTO promotion_redemption (id, promotion_id, order_id, member_phone,
                                                  sale_quantity, gift_quantity, created_at)
                VALUES (?, ?, ?, ?, ?, ?, now())
                """, UUID.randomUUID(), promotionId, orderId, phone, saleQuantity, giftQuantity);
    }

    public UUID insertGroupBuy(GroupBuy groupBuy) {
        jdbcTemplate.update("""
                INSERT INTO group_buy (id, code, promotion_id, sku_id, price_amount, currency,
                                       group_size, creator_phone, status, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """,
                groupBuy.id(),
                groupBuy.code(),
                groupBuy.promotionId(),
                groupBuy.skuId(),
                groupBuy.price().amount(),
                groupBuy.price().currency(),
                groupBuy.groupSize(),
                groupBuy.creatorPhone(),
                groupBuy.status(),
                java.sql.Timestamp.from(groupBuy.expiresAt()));
        return groupBuy.id();
    }

    public List<UUID> findGroupBuyIds(UUID promotionId) {
        if (promotionId == null) {
            return jdbcTemplate.query("SELECT id FROM group_buy ORDER BY created_at DESC",
                    (rs, rowNum) -> rs.getObject("id", UUID.class));
        }
        return jdbcTemplate.query("SELECT id FROM group_buy WHERE promotion_id = ? ORDER BY created_at DESC",
                (rs, rowNum) -> rs.getObject("id", UUID.class), promotionId);
    }

    public Optional<GroupBuy> findGroupBuy(UUID id) {
        return jdbcTemplate.query("""
                SELECT g.id, g.code, g.promotion_id, g.sku_id, g.price_amount, g.currency,
                       g.group_size, g.creator_phone, g.status, g.expires_at, g.created_at, g.completed_at,
                       (SELECT COUNT(*) FROM group_buy_member m WHERE m.group_buy_id = g.id) AS joined_count,
                       (SELECT COUNT(*) FROM group_buy_member m
                         WHERE m.group_buy_id = g.id AND m.paid_at IS NOT NULL) AS paid_count
                FROM group_buy g
                WHERE g.id = ?
                """, this::mapGroupBuy, id).stream().findFirst();
    }

    public Optional<GroupBuy> findGroupBuyByCode(String code) {
        return jdbcTemplate.query("""
                SELECT g.id, g.code, g.promotion_id, g.sku_id, g.price_amount, g.currency,
                       g.group_size, g.creator_phone, g.status, g.expires_at, g.created_at, g.completed_at,
                       (SELECT COUNT(*) FROM group_buy_member m WHERE m.group_buy_id = g.id) AS joined_count,
                       (SELECT COUNT(*) FROM group_buy_member m
                         WHERE m.group_buy_id = g.id AND m.paid_at IS NOT NULL) AS paid_count
                FROM group_buy g
                WHERE g.code = ?
                """, this::mapGroupBuy, code).stream().findFirst();
    }

    public List<GroupBuyMember> findGroupMembers(UUID groupBuyId) {
        return jdbcTemplate.query("""
                SELECT id, group_buy_id, order_id, phone, role, joined_at, paid_at
                FROM group_buy_member
                WHERE group_buy_id = ?
                ORDER BY joined_at
                """, (rs, rowNum) -> new GroupBuyMember(
                rs.getObject("id", UUID.class),
                rs.getObject("group_buy_id", UUID.class),
                rs.getObject("order_id", UUID.class),
                rs.getString("phone"),
                rs.getString("role"),
                rs.getTimestamp("joined_at").toInstant(),
                rs.getTimestamp("paid_at") == null ? null : rs.getTimestamp("paid_at").toInstant()), groupBuyId);
    }

    public List<PromotionClaim> findUnreleasedClaims(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT r.promotion_id, p.type, r.sale_quantity, r.gift_quantity
                FROM promotion_redemption r
                JOIN promotion p ON p.id = r.promotion_id
                WHERE r.order_id = ? AND r.released_at IS NULL
                """, (rs, rowNum) -> new PromotionClaim(
                rs.getObject("promotion_id", UUID.class),
                PromotionType.valueOf(rs.getString("type")),
                rs.getInt("sale_quantity"),
                rs.getInt("gift_quantity")), orderId);
    }

    public void markClaimsReleased(UUID orderId) {
        jdbcTemplate.update("""
                UPDATE promotion_redemption SET released_at = now()
                WHERE order_id = ? AND released_at IS NULL
                """, orderId);
    }

    public void decreaseSold(UUID promotionId, int quantity) {
        jdbcTemplate.update("""
                UPDATE promotion SET sold_quantity = GREATEST(0, sold_quantity - ?) WHERE id = ?
                """, quantity, promotionId);
    }

    public void deleteUnpaidGroupMember(UUID orderId) {
        jdbcTemplate.update("""
                DELETE FROM group_buy_member WHERE order_id = ? AND paid_at IS NULL
                """, orderId);
    }

    public void insertGroupMember(UUID groupBuyId, UUID orderId, String phone, String role) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO group_buy_member (id, group_buy_id, order_id, phone, role, joined_at)
                    VALUES (?, ?, ?, ?, ?, now())
                    """, UUID.randomUUID(), groupBuyId, orderId, phone, role);
        } catch (DuplicateKeyException exception) {
            throw new com.fullmall.platform.error.ApiException(org.springframework.http.HttpStatus.CONFLICT,
                    "GROUP_BUY_ALREADY_JOINED",
                    "该手机号已经参加此拼团，或订单已绑定其他拼团");
        }
    }

    public Optional<GroupBuyMember> findMemberByOrder(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT id, group_buy_id, order_id, phone, role, joined_at, paid_at
                FROM group_buy_member
                WHERE order_id = ?
                """, (rs, rowNum) -> new GroupBuyMember(
                rs.getObject("id", UUID.class),
                rs.getObject("group_buy_id", UUID.class),
                rs.getObject("order_id", UUID.class),
                rs.getString("phone"),
                rs.getString("role"),
                rs.getTimestamp("joined_at").toInstant(),
                rs.getTimestamp("paid_at") == null ? null : rs.getTimestamp("paid_at").toInstant()),
                orderId).stream().findFirst();
    }

    public void markMemberPaid(UUID orderId) {
        jdbcTemplate.update("""
                UPDATE group_buy_member SET paid_at = COALESCE(paid_at, now()) WHERE order_id = ?
                """, orderId);
    }

    public boolean completeGroupIfFull(UUID groupBuyId) {
        int updated = jdbcTemplate.update("""
                UPDATE group_buy g
                SET status = 'SUCCESS', completed_at = now()
                WHERE g.id = ? AND g.status = 'FORMING'
                  AND (SELECT COUNT(*) FROM group_buy_member m
                       WHERE m.group_buy_id = g.id AND m.paid_at IS NOT NULL) >= g.group_size
                """, groupBuyId);
        return updated == 1;
    }

    public List<UUID> findExpiredFormingGroups(Instant now) {
        return jdbcTemplate.query("""
                SELECT id FROM group_buy
                WHERE status = 'FORMING' AND expires_at <= ?
                """, (rs, rowNum) -> rs.getObject("id", UUID.class), java.sql.Timestamp.from(now));
    }

    public List<UUID> findUnrefundedOrderIds(UUID groupBuyId) {
        return jdbcTemplate.query("""
                SELECT m.order_id
                FROM group_buy_member m
                JOIN group_buy g ON g.id = m.group_buy_id
                WHERE g.id = ? AND g.status = 'FAILED'
                  AND m.paid_at IS NOT NULL AND m.refunded_at IS NULL
                ORDER BY m.joined_at
                """, (rs, rowNum) -> rs.getObject("order_id", UUID.class), groupBuyId);
    }

    public List<UUID> findAllUnrefundedOrderIds() {
        return jdbcTemplate.query("""
                SELECT m.order_id
                FROM group_buy_member m
                JOIN group_buy g ON g.id = m.group_buy_id
                WHERE g.status = 'FAILED'
                  AND m.paid_at IS NOT NULL AND m.refunded_at IS NULL
                ORDER BY m.joined_at
                """, (rs, rowNum) -> rs.getObject("order_id", UUID.class));
    }

    public void markMemberRefunded(UUID orderId) {
        jdbcTemplate.update("""
                UPDATE group_buy_member SET refunded_at = COALESCE(refunded_at, now())
                WHERE order_id = ?
                """, orderId);
    }

    public List<UUID> failGroup(UUID groupBuyId) {
        int updated = jdbcTemplate.update("""
                UPDATE group_buy SET status = 'FAILED', completed_at = now()
                WHERE id = ? AND status = 'FORMING'
                """, groupBuyId);
        if (updated == 0) {
            return List.of();
        }
        return findUnrefundedOrderIds(groupBuyId);
    }

    private Promotion mapPromotion(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Promotion(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                PromotionType.valueOf(rs.getString("type")),
                rs.getString("title"),
                rs.getString("description"),
                PromotionStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("starts_at").toInstant(),
                rs.getTimestamp("ends_at").toInstant(),
                rs.getObject("sku_id", UUID.class),
                rs.getObject("promo_price_amount", Long.class),
                rs.getObject("gift_sku_id", UUID.class),
                rs.getObject("buy_quantity", Integer.class),
                rs.getObject("gift_quantity", Integer.class),
                rs.getObject("max_gift_per_order", Integer.class),
                rs.getObject("stock_limit", Integer.class),
                rs.getInt("sold_quantity"),
                rs.getObject("per_user_limit", Integer.class),
                rs.getObject("group_size", Integer.class),
                rs.getObject("group_duration_hours", Integer.class),
                rs.getInt("priority"),
                rs.getTimestamp("created_at").toInstant());
    }

    private GroupBuy mapGroupBuy(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new GroupBuy(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getObject("promotion_id", UUID.class),
                rs.getObject("sku_id", UUID.class),
                new Money(rs.getLong("price_amount"), rs.getString("currency")),
                rs.getInt("group_size"),
                rs.getString("creator_phone"),
                rs.getString("status"),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                rs.getInt("joined_count"),
                rs.getInt("paid_count"));
    }
}