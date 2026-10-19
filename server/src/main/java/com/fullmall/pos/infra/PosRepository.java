package com.fullmall.pos.infra;

import com.fullmall.platform.money.Money;
import com.fullmall.pos.api.MemberAdminView;
import com.fullmall.pos.api.PosProductView;
import com.fullmall.pos.api.PosTenderTotal;
import com.fullmall.pos.domain.CouponDiscountType;
import com.fullmall.pos.domain.CouponStatus;
import com.fullmall.pos.domain.MemberCardEntryType;
import com.fullmall.pos.domain.PointsEntryType;
import com.fullmall.pos.domain.PosCoupon;
import com.fullmall.pos.domain.MemberLevel;
import com.fullmall.pos.domain.PosMember;
import com.fullmall.pos.domain.PosSale;
import com.fullmall.pos.domain.PosSaleAllocation;
import com.fullmall.pos.domain.PosSaleLine;
import com.fullmall.pos.domain.PosSaleStatus;
import com.fullmall.pos.domain.PosShift;
import com.fullmall.pos.domain.PosShiftStatus;
import com.fullmall.pos.domain.PosTender;
import com.fullmall.pos.domain.RefundType;
import com.fullmall.pos.domain.TenderStatus;
import com.fullmall.pos.domain.TenderType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class PosRepository {

    private final JdbcTemplate jdbcTemplate;

    public PosRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------------------------ shifts

    public void lockIdempotency(String key) {
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(hashtext(?))",
                (org.springframework.jdbc.core.PreparedStatementCallback<Void>) statement -> {
                    statement.setString(1, key);
                    statement.execute();
                    return null;
                });
    }

    public void lockSale(UUID saleId) {
        jdbcTemplate.queryForList("SELECT id FROM pos_sale WHERE id = ? FOR UPDATE", saleId);
    }

    public void insertShift(PosShift shift) {
        jdbcTemplate.update("""
                INSERT INTO pos_shift (id, store_id, cashier_id, cashier_name, status, currency,
                                       opening_float_amount, opened_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                shift.id(), shift.storeId(), shift.cashierId(), shift.cashierName(), shift.status().name(),
                shift.openingFloat().currency(), shift.openingFloat().amount(),
                Timestamp.from(shift.openedAt()));
    }

    public PosShift findShift(UUID shiftId) {
        List<PosShift> rows = jdbcTemplate.query("""
                SELECT id, store_id, cashier_id, cashier_name, status, currency, opening_float_amount,
                       expected_cash_amount, actual_cash_amount, variance_amount, variance_reason,
                       opened_at, closed_at
                FROM pos_shift WHERE id = ?
                """, (rs, rowNum) -> mapShift(rs), shiftId);
        return rows.stream().findFirst().orElse(null);
    }

    public PosShift findOpenShift(UUID storeId, String cashierName) {
        List<PosShift> rows = jdbcTemplate.query("""
                SELECT id, store_id, cashier_id, cashier_name, status, currency, opening_float_amount,
                       expected_cash_amount, actual_cash_amount, variance_amount, variance_reason,
                       opened_at, closed_at
                FROM pos_shift
                WHERE store_id = ? AND cashier_name = ? AND status = 'OPEN'
                ORDER BY opened_at DESC
                LIMIT 1
                """, (rs, rowNum) -> mapShift(rs), storeId, cashierName);
        return rows.stream().findFirst().orElse(null);
    }

    public List<PosShift> findShifts(UUID storeId, LocalDate date) {
        return jdbcTemplate.query("""
                SELECT id, store_id, cashier_id, cashier_name, status, currency, opening_float_amount,
                       expected_cash_amount, actual_cash_amount, variance_amount, variance_reason,
                       opened_at, closed_at
                FROM pos_shift
                WHERE store_id = ? AND opened_at::date = ?
                ORDER BY opened_at
                """, (rs, rowNum) -> mapShift(rs), storeId, date);
    }

    public void closeShift(UUID shiftId, long expectedCash, long actualCash, long variance,
                           String reason, Instant closedAt) {
        jdbcTemplate.update("""
                UPDATE pos_shift
                SET status = 'CLOSED', expected_cash_amount = ?, actual_cash_amount = ?, variance_amount = ?,
                    variance_reason = ?, closed_at = ?
                WHERE id = ? AND status = 'OPEN'
                """, expectedCash, actualCash, variance, reason, Timestamp.from(closedAt), shiftId);
    }

    public void insertCashMovement(UUID shiftId, UUID saleId, String type, long amount, String currency,
                                   String reason, String operatorName) {
        jdbcTemplate.update("""
                INSERT INTO pos_cash_movement (id, shift_id, sale_id, movement_type, amount_amount,
                                               currency, reason, operator_name)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), shiftId, saleId, type, amount, currency, reason, operatorName);
    }

    public long cashTotal(UUID shiftId) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_amount), 0) FROM pos_cash_movement WHERE shift_id = ?
                """, Long.class, shiftId);
        if (total == null) {
            throw new IllegalStateException("Cash total query returned null for shift " + shiftId);
        }
        return total;
    }

    public long cashTotalByType(UUID shiftId, String type) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_amount), 0)
                FROM pos_cash_movement WHERE shift_id = ? AND movement_type = ?
                """, Long.class, shiftId, type);
        if (total == null) {
            throw new IllegalStateException("Cash total query returned null for shift " + shiftId + " type " + type);
        }
        return total;
    }

    public int countShiftSales(UUID shiftId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pos_sale WHERE shift_id = ? AND status IN ('COMPLETED', 'REFUNDED')
                """, Integer.class, shiftId);
        if (count == null) {
            throw new IllegalStateException("Shift sale count query returned null for shift " + shiftId);
        }
        return count;
    }

    // ------------------------------------------------------------------ sales

    public void insertSale(PosSale sale, String idempotencyKey, String requestHash) {
        jdbcTemplate.update("""
                INSERT INTO pos_sale (id, sale_no, store_id, shift_id, member_id, currency,
                                      subtotal_amount, promotion_discount_amount, coupon_discount_amount,
                                      member_discount_amount, points_discount_amount, payable_amount,
                                      cash_tendered_amount, change_amount, status, coupon_id, points_redeemed,
                                      points_earned, refunded_amount, operator_name, idempotency_key,
                                      request_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                sale.id(), sale.saleNo(), sale.storeId(), sale.shiftId(), sale.memberId(), sale.currency(),
                sale.subtotal().amount(), sale.promotionDiscount().amount(), sale.couponDiscount().amount(),
                sale.memberDiscount().amount(), sale.pointsDiscount().amount(), sale.payable().amount(),
                sale.cashTendered().amount(), sale.change().amount(), sale.status().name(), sale.couponId(),
                sale.pointsRedeemed(), sale.pointsEarned(), sale.refunded().amount(), sale.operatorName(),
                idempotencyKey, requestHash, Timestamp.from(sale.createdAt()));
    }

    public void insertSaleLine(UUID saleId, PosSaleLine line, String currency) {
        jdbcTemplate.update("""
                INSERT INTO pos_sale_line (id, sale_id, sku_id, title_snapshot, sku_code_snapshot, quantity,
                                           unit_price_amount, line_total_amount, refunded_quantity, currency)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                line.id(), saleId, line.skuId(), line.title(), line.skuCode(), line.quantity(),
                line.unitPrice().amount(), line.lineTotal().amount(), line.refundedQuantity(), currency);
    }

    public void insertAllocation(UUID saleLineId, UUID batchId, int quantity, Money unitCost, String currency) {
        jdbcTemplate.update("""
                INSERT INTO pos_sale_allocation (id, sale_line_id, batch_id, quantity, unit_cost_amount, currency)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), saleLineId, batchId, quantity,
                unitCost == null ? null : unitCost.amount(), currency);
    }

    public PosSale findSale(UUID saleId) {
        List<PosSale> rows = jdbcTemplate.query("""
                SELECT s.id, s.sale_no, s.store_id, s.shift_id, s.member_id, s.currency,
                       s.subtotal_amount, s.promotion_discount_amount, s.coupon_discount_amount,
                       s.member_discount_amount, s.points_discount_amount, s.payable_amount,
                       s.cash_tendered_amount, s.change_amount, s.status, s.coupon_id, c.code AS coupon_code,
                       s.points_redeemed, s.points_earned, s.points_reversed, s.refunded_amount, s.operator_name,
                       s.created_at, s.paid_at
                FROM pos_sale s
                LEFT JOIN coupon c ON c.id = s.coupon_id
                WHERE s.id = ?
                """, (rs, rowNum) -> mapSale(rs), saleId);
        if (rows.isEmpty()) {
            return null;
        }
        return withChildren(rows.get(0));
    }

    public PosSale findSaleByKey(UUID storeId, String idempotencyKey) {
        List<PosSale> rows = jdbcTemplate.query("""
                SELECT s.id, s.sale_no, s.store_id, s.shift_id, s.member_id, s.currency,
                       s.subtotal_amount, s.promotion_discount_amount, s.coupon_discount_amount,
                       s.member_discount_amount, s.points_discount_amount, s.payable_amount,
                       s.cash_tendered_amount, s.change_amount, s.status, s.coupon_id, c.code AS coupon_code,
                       s.points_redeemed, s.points_earned, s.points_reversed, s.refunded_amount, s.operator_name,
                       s.created_at, s.paid_at
                FROM pos_sale s
                LEFT JOIN coupon c ON c.id = s.coupon_id
                WHERE s.store_id = ? AND s.idempotency_key = ?
                """, (rs, rowNum) -> mapSale(rs), storeId, idempotencyKey);
        if (rows.isEmpty()) {
            return null;
        }
        return withChildren(rows.get(0));
    }

    public String findSaleRequestHash(UUID saleId) {
        List<String> hashes = jdbcTemplate.query("""
                SELECT request_hash FROM pos_sale WHERE id = ?
                """, (rs, rowNum) -> rs.getString("request_hash"), saleId);
        return hashes.isEmpty() ? null : hashes.get(0);
    }

    public List<PosSale> findShiftSales(UUID shiftId, int limit) {
        List<PosSale> rows = jdbcTemplate.query("""
                SELECT s.id, s.sale_no, s.store_id, s.shift_id, s.member_id, s.currency,
                       s.subtotal_amount, s.promotion_discount_amount, s.coupon_discount_amount,
                       s.member_discount_amount, s.points_discount_amount, s.payable_amount,
                       s.cash_tendered_amount, s.change_amount, s.status, s.coupon_id, c.code AS coupon_code,
                       s.points_redeemed, s.points_earned, s.points_reversed, s.refunded_amount, s.operator_name,
                       s.created_at, s.paid_at
                FROM pos_sale s
                LEFT JOIN coupon c ON c.id = s.coupon_id
                WHERE s.shift_id = ?
                ORDER BY s.created_at DESC
                LIMIT ?
                """, (rs, rowNum) -> mapSale(rs), shiftId, limit);
        return rows.stream().map(this::withChildren).toList();
    }

    public List<PosSale> findStoreSales(UUID storeId, LocalDate date, int limit) {
        List<PosSale> rows = jdbcTemplate.query("""
                SELECT s.id, s.sale_no, s.store_id, s.shift_id, s.member_id, s.currency,
                       s.subtotal_amount, s.promotion_discount_amount, s.coupon_discount_amount,
                       s.member_discount_amount, s.points_discount_amount, s.payable_amount,
                       s.cash_tendered_amount, s.change_amount, s.status, s.coupon_id, c.code AS coupon_code,
                       s.points_redeemed, s.points_earned, s.points_reversed, s.refunded_amount, s.operator_name,
                       s.created_at, s.paid_at
                FROM pos_sale s
                LEFT JOIN coupon c ON c.id = s.coupon_id
                WHERE s.store_id = ? AND s.created_at::date = ?
                ORDER BY s.created_at DESC
                LIMIT ?
                """, (rs, rowNum) -> mapSale(rs), storeId, date, limit);
        return rows.stream().map(this::withChildren).toList();
    }

    public void updateSaleBenefits(UUID saleId, UUID couponId, long couponDiscount, long pointsRedeemed,
                                   long pointsDiscount, long payable) {
        jdbcTemplate.update("""
                UPDATE pos_sale
                SET coupon_id = ?, coupon_discount_amount = ?, points_redeemed = ?, points_discount_amount = ?,
                    payable_amount = ?
                WHERE id = ? AND status = 'DRAFT'
                """, couponId, couponDiscount, pointsRedeemed, pointsDiscount, payable, saleId);
    }

    public void updateSaleMember(UUID saleId, UUID memberId) {
        jdbcTemplate.update("""
                UPDATE pos_sale SET member_id = ? WHERE id = ? AND status = 'DRAFT'
                """, memberId, saleId);
    }

    public void updateSaleCash(UUID saleId, long cashTendered, long change) {
        jdbcTemplate.update("""
                UPDATE pos_sale SET cash_tendered_amount = ?, change_amount = ?
                WHERE id = ? AND status = 'DRAFT'
                """, cashTendered, change, saleId);
    }

    public void markSaleCompleted(UUID saleId, long pointsEarned, Instant paidAt) {
        jdbcTemplate.update("""
                UPDATE pos_sale SET status = 'COMPLETED', points_earned = ?, paid_at = ?
                WHERE id = ? AND status = 'DRAFT'
                """, pointsEarned, Timestamp.from(paidAt), saleId);
    }

    public void markSaleVoided(UUID saleId, Instant voidedAt) {
        jdbcTemplate.update("""
                UPDATE pos_sale SET status = 'VOIDED', voided_at = ?
                WHERE id = ? AND status = 'DRAFT'
                """, Timestamp.from(voidedAt), saleId);
    }

    public void markSaleRefunded(UUID saleId, long refundedAmount, long pointsReversed) {
        jdbcTemplate.update("""
                UPDATE pos_sale
                SET status = 'REFUNDED', refunded_amount = ?, points_reversed = ?
                WHERE id = ? AND status = 'COMPLETED'
                """, refundedAmount, pointsReversed, saleId);
    }

    public List<PosTender> findTenders(UUID saleId) {
        return jdbcTemplate.query("""
                SELECT id, tender_type, amount_amount, currency, provider, provider_trade_no, status,
                       cash_tendered_amount, change_amount, refunded_amount, reversal_status, paid_at
                FROM pos_tender WHERE sale_id = ? ORDER BY created_at, id
                """, (rs, rowNum) -> mapTender(rs), saleId);
    }

    public long successfulTenderTotal(UUID saleId) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_amount), 0) FROM pos_tender
                WHERE sale_id = ? AND status = 'SUCCESS'
                """, Long.class, saleId);
        if (total == null) {
            throw new IllegalStateException("Tender total query returned null for sale " + saleId);
        }
        return total;
    }

    public void insertTender(UUID saleId, PosTender tender, String idempotencyKey) {
        jdbcTemplate.update("""
                INSERT INTO pos_tender (id, sale_id, tender_type, amount_amount, currency, provider,
                                        provider_trade_no, status, cash_tendered_amount, change_amount,
                                        idempotency_key, paid_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                tender.id(), saleId, tender.type().name(), tender.amount().amount(), tender.amount().currency(),
                tender.provider(), tender.providerTradeNo(), tender.status().name(),
                tender.cashTendered() == null ? null : tender.cashTendered().amount(),
                tender.change() == null ? null : tender.change().amount(),
                idempotencyKey, tender.paidAt() == null ? null : Timestamp.from(tender.paidAt()));
    }

    public PosTender findTenderByKey(UUID saleId, String idempotencyKey) {
        List<PosTender> rows = jdbcTemplate.query("""
                SELECT id, tender_type, amount_amount, currency, provider, provider_trade_no, status,
                       cash_tendered_amount, change_amount, refunded_amount, reversal_status, paid_at
                FROM pos_tender WHERE sale_id = ? AND idempotency_key = ?
                """, (rs, rowNum) -> mapTender(rs), saleId, idempotencyKey);
        return rows.stream().findFirst().orElse(null);
    }

    public void markTenderReversed(UUID tenderId) {
        jdbcTemplate.update("""
                UPDATE pos_tender SET status = 'REVERSED', reversal_status = 'REVERSED'
                WHERE id = ? AND status = 'SUCCESS'
                """, tenderId);
    }

    public List<PosSaleAllocation> findAllocations(UUID saleId) {
        return jdbcTemplate.query("""
                SELECT a.id, a.sale_line_id, a.batch_id, a.quantity, a.refunded_quantity,
                       a.unit_cost_amount, a.currency
                FROM pos_sale_allocation a
                JOIN pos_sale_line l ON l.id = a.sale_line_id
                WHERE l.sale_id = ?
                ORDER BY a.batch_id
                """, (rs, rowNum) -> {
            Long unitCost = rs.getObject("unit_cost_amount", Long.class);
            String currency = rs.getString("currency");
            return new PosSaleAllocation(
                    rs.getObject("id", UUID.class),
                    rs.getObject("sale_line_id", UUID.class),
                    rs.getObject("batch_id", UUID.class),
                    rs.getInt("quantity"),
                    rs.getInt("refunded_quantity"),
                    unitCost == null ? null : new Money(unitCost, currency));
        }, saleId);
    }

    public void updateLineRefunded(UUID saleId) {
        jdbcTemplate.update("""
                UPDATE pos_sale_line
                SET refunded_quantity = quantity, refunded_amount = line_total_amount
                WHERE sale_id = ?
                """, saleId);
    }

    // ------------------------------------------------------------------ members

    public PosMember findMember(UUID memberId) {
        List<PosMember> rows = jdbcTemplate.query("""
                SELECT id, member_no, name, phone, tier, birthday, growth_value, birthday_gift_year, points_balance, stored_value_balance, currency
                FROM member WHERE id = ?
                """, (rs, rowNum) -> mapMember(rs), memberId);
        return rows.stream().findFirst().orElse(null);
    }

    public PosMember findMemberByQuery(String query) {
        List<PosMember> rows = jdbcTemplate.query("""
                SELECT id, member_no, name, phone, tier, birthday, growth_value, birthday_gift_year, points_balance, stored_value_balance, currency
                FROM member
                WHERE phone = ? OR member_no = ?
                """, (rs, rowNum) -> mapMember(rs), query, query);
        return rows.stream().findFirst().orElse(null);
    }

    public List<MemberAdminView> listMembers(String query, int limit) {
        String like = "%" + (query == null ? "" : query.trim()) + "%";
        return jdbcTemplate.query("""
                SELECT m.id, m.member_no, m.name, m.phone, m.tier, m.birthday, m.growth_value, m.birthday_gift_year, m.points_balance,
                       m.stored_value_balance, m.currency, m.created_at,
                       COUNT(s.id) FILTER (WHERE s.status IN ('COMPLETED', 'PARTIALLY_REFUNDED', 'REFUNDED'))
                           AS sale_count,
                       COALESCE(SUM(GREATEST(s.payable_amount - s.refunded_amount, 0))
                                FILTER (WHERE s.status IN ('COMPLETED', 'PARTIALLY_REFUNDED', 'REFUNDED')), 0)
                           AS total_spent
                FROM member m
                LEFT JOIN pos_sale s ON s.member_id = m.id
                WHERE m.phone ILIKE ? OR m.member_no ILIKE ? OR m.name ILIKE ?
                GROUP BY m.id
                ORDER BY m.created_at DESC
                LIMIT ?
                """, (rs, rowNum) -> new MemberAdminView(
                mapMember(rs),
                rs.getInt("sale_count"),
                new Money(rs.getLong("total_spent"), rs.getString("currency")),
                instant(rs, "created_at")), like, like, like, limit);
    }

    public void insertMember(PosMember member) {
        jdbcTemplate.update("""
                INSERT INTO member (id, member_no, name, phone, tier, birthday, growth_value, birthday_gift_year,
                                    points_balance, stored_value_balance, currency, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """, member.id(), member.memberNo(), member.name(), member.phone(), member.tier(),
                member.birthday(), member.growthValue(), member.birthdayGiftYear(), member.points(),
                member.storedValue().amount(), member.storedValue().currency());
    }

    public boolean addPoints(UUID memberId, long points) {
        int updated = jdbcTemplate.update("""
                UPDATE member SET points_balance = points_balance + ?
                WHERE id = ? AND points_balance + ? >= 0
                """, points, memberId, points);
        return updated == 1;
    }

    public void addStoredValue(UUID memberId, long amount) {
        jdbcTemplate.update("""
                UPDATE member SET stored_value_balance = stored_value_balance + ? WHERE id = ?
                """, amount, memberId);
    }

    public List<PosCoupon> listCoupons(int limit) {
        return jdbcTemplate.query("""
                SELECT id, code, name, discount_type, discount_value, min_spend_amount, max_discount_amount,
                       member_id, currency, status, valid_from, valid_to
                FROM coupon
                ORDER BY created_at DESC
                LIMIT ?
                """, (rs, rowNum) -> mapCoupon(rs), limit);
    }

    public void insertCoupon(PosCoupon coupon, UUID memberId) {
        jdbcTemplate.update("""
                INSERT INTO coupon (id, code, name, discount_type, discount_value, min_spend_amount,
                                    max_discount_amount, member_id, currency, status, valid_from, valid_to)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ISSUED', ?, ?)
                """, coupon.id(), coupon.code(), coupon.name(), coupon.discountType().name(),
                coupon.discountValue(), coupon.minSpend().amount(),
                coupon.maxDiscount() == null ? null : coupon.maxDiscount().amount(),
                memberId, coupon.currency(), Timestamp.from(coupon.validFrom()),
                Timestamp.from(coupon.validTo()));
    }

    public MemberLevel findMemberLevel(String code) {
        List<MemberLevel> rows = jdbcTemplate.query("""
                SELECT code, name, discount_rate, points_multiplier, free_shipping,
                       birthday_points, min_growth, sort_order
                FROM member_level WHERE code = ?
                """, (rs, rowNum) -> mapLevel(rs), code);
        return rows.stream().findFirst().orElse(null);
    }

    public List<MemberLevel> listMemberLevels() {
        return jdbcTemplate.query("""
                SELECT code, name, discount_rate, points_multiplier, free_shipping,
                       birthday_points, min_growth, sort_order
                FROM member_level ORDER BY sort_order, min_growth
                """, (rs, rowNum) -> mapLevel(rs));
    }

    public void updateMemberLevel(String code, int discountRate, int pointsMultiplier,
                                  long birthdayPoints) {
        int updated = jdbcTemplate.update("""
                UPDATE member_level
                SET discount_rate = ?, points_multiplier = ?, birthday_points = ?
                WHERE code = ?
                """, discountRate, pointsMultiplier, birthdayPoints, code);
        if (updated != 1) {
            throw new IllegalStateException("Member level not found: " + code);
        }
    }

    public long addGrowth(UUID memberId, long growth) {
        jdbcTemplate.update("""
                UPDATE member SET growth_value = growth_value + ? WHERE id = ?
                """, growth, memberId);
        Long balance = jdbcTemplate.queryForObject("""
                SELECT growth_value FROM member WHERE id = ?
                """, Long.class, memberId);
        if (balance == null) {
            throw new IllegalStateException("Growth query returned null for member " + memberId);
        }
        return balance;
    }

    public void updateMemberTier(UUID memberId, String tier) {
        jdbcTemplate.update("UPDATE member SET tier = ? WHERE id = ?", tier, memberId);
    }

    public void insertGrowthLedger(UUID memberId, UUID saleId, long growth, long balanceAfter,
                                   String reason, String businessKey) {
        jdbcTemplate.update("""
                INSERT INTO growth_ledger (id, member_id, sale_id, entry_type, growth,
                                           balance_after, reason, business_key)
                VALUES (?, ?, ?, 'EARN', ?, ?, ?, ?)
                """, UUID.randomUUID(), memberId, saleId, growth, balanceAfter, reason, businessKey);
    }

    public void updateBirthdayGiftYear(UUID memberId, int year) {
        jdbcTemplate.update("UPDATE member SET birthday_gift_year = ? WHERE id = ?", year, memberId);
    }

    public void updateSaleMemberBenefits(UUID saleId, UUID memberId, long memberDiscount,
                                         long couponDiscount, long pointsDiscount, long payable) {
        jdbcTemplate.update("""
                UPDATE pos_sale
                SET member_id = ?, member_discount_amount = ?, coupon_discount_amount = ?,
                    points_discount_amount = ?, payable_amount = ?
                WHERE id = ? AND status = 'DRAFT'
                """, memberId, memberDiscount, couponDiscount, pointsDiscount, payable, saleId);
    }

    private MemberLevel mapLevel(ResultSet rs) throws SQLException {
        return new MemberLevel(
                rs.getString("code"), rs.getString("name"), rs.getInt("discount_rate"),
                rs.getInt("points_multiplier"), rs.getBoolean("free_shipping"),
                rs.getLong("birthday_points"), rs.getLong("min_growth"), rs.getInt("sort_order"));
    }

    public boolean decrementPoints(UUID memberId, long points) {
        int updated = jdbcTemplate.update("""
                UPDATE member SET points_balance = points_balance - ?
                WHERE id = ? AND points_balance >= ?
                """, points, memberId, points);
        return updated == 1;
    }

    public void incrementPoints(UUID memberId, long points) {
        jdbcTemplate.update("""
                UPDATE member SET points_balance = points_balance + ? WHERE id = ?
                """, points, memberId);
    }

    public long pointsBalance(UUID memberId) {
        Long balance = jdbcTemplate.queryForObject("""
                SELECT points_balance FROM member WHERE id = ?
                """, Long.class, memberId);
        if (balance == null) {
            throw new IllegalStateException("Member points query returned null for " + memberId);
        }
        return balance;
    }

    public boolean decrementStoredValue(UUID memberId, long amount) {
        int updated = jdbcTemplate.update("""
                UPDATE member SET stored_value_balance = stored_value_balance - ?
                WHERE id = ? AND stored_value_balance >= ?
                """, amount, memberId, amount);
        return updated == 1;
    }

    public void incrementStoredValue(UUID memberId, long amount) {
        jdbcTemplate.update("""
                UPDATE member SET stored_value_balance = stored_value_balance + ? WHERE id = ?
                """, amount, memberId);
    }

    public long storedValueBalance(UUID memberId) {
        Long balance = jdbcTemplate.queryForObject("""
                SELECT stored_value_balance FROM member WHERE id = ?
                """, Long.class, memberId);
        if (balance == null) {
            throw new IllegalStateException("Member stored value query returned null for " + memberId);
        }
        return balance;
    }

    public void insertPointsLedger(UUID memberId, UUID saleId, PointsEntryType type, long points,
                                   long balanceAfter, String reason, String businessKey) {
        jdbcTemplate.update("""
                INSERT INTO points_ledger (id, member_id, sale_id, entry_type, points, balance_after,
                                           reason, business_key)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), memberId, saleId, type.name(), points, balanceAfter, reason, businessKey);
    }

    public void insertCardLedger(UUID memberId, UUID saleId, MemberCardEntryType type, long amount,
                                 long balanceAfter, String currency, String reason, String businessKey) {
        jdbcTemplate.update("""
                INSERT INTO member_card_ledger (id, member_id, sale_id, entry_type, amount_amount,
                                                balance_after_amount, currency, reason, business_key)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), memberId, saleId, type.name(), amount, balanceAfter,
                currency, reason, businessKey);
    }

    // ------------------------------------------------------------------ coupons

    public PosCoupon findCouponForUpdate(String code) {
        List<PosCoupon> rows = jdbcTemplate.query("""
                SELECT id, code, name, discount_type, discount_value, min_spend_amount, max_discount_amount,
                       member_id, currency, status, valid_from, valid_to
                FROM coupon WHERE code = ? FOR UPDATE
                """, (rs, rowNum) -> mapCoupon(rs), code);
        return rows.stream().findFirst().orElse(null);
    }

    public PosCoupon findCoupon(UUID couponId) {
        List<PosCoupon> rows = jdbcTemplate.query("""
                SELECT id, code, name, discount_type, discount_value, min_spend_amount, max_discount_amount,
                       member_id, currency, status, valid_from, valid_to
                FROM coupon WHERE id = ?
                """, (rs, rowNum) -> mapCoupon(rs), couponId);
        return rows.stream().findFirst().orElse(null);
    }

    public List<PosCoupon> findUsableCoupons(UUID memberId) {
        return jdbcTemplate.query("""
                SELECT id, code, name, discount_type, discount_value, min_spend_amount, max_discount_amount,
                       member_id, currency, status, valid_from, valid_to
                FROM coupon
                WHERE status = 'ISSUED' AND valid_from <= now() AND valid_to >= now()
                  AND (member_id IS NULL OR member_id = ?)
                ORDER BY valid_to, code
                """, (rs, rowNum) -> mapCoupon(rs), memberId);
    }

    public boolean lockCoupon(UUID couponId, UUID saleId, Instant lockedAt) {
        int updated = jdbcTemplate.update("""
                UPDATE coupon SET status = 'LOCKED', sale_id = ?, locked_at = ?
                WHERE id = ? AND status = 'ISSUED'
                """, saleId, Timestamp.from(lockedAt), couponId);
        return updated == 1;
    }

    public boolean markCouponUsed(UUID couponId, UUID saleId, Instant usedAt) {
        int updated = jdbcTemplate.update("""
                UPDATE coupon SET status = 'USED', sale_id = ?, used_at = ?
                WHERE id = ? AND status = 'LOCKED'
                """, saleId, Timestamp.from(usedAt), couponId);
        return updated == 1;
    }

    public void restoreUsedCoupon(UUID couponId) {
        jdbcTemplate.update("""
                UPDATE coupon
                SET status = CASE WHEN valid_to < now() THEN 'EXPIRED' ELSE 'ISSUED' END,
                    sale_id = NULL, locked_at = NULL, used_at = NULL
                WHERE id = ? AND status = 'USED'
                """, couponId);
    }

    public void releaseCoupon(UUID couponId) {
        jdbcTemplate.update("""
                UPDATE coupon SET status = 'ISSUED', sale_id = NULL, locked_at = NULL
                WHERE id = ? AND status = 'LOCKED'
                """, couponId);
    }

    public void insertCouponRedemption(UUID couponId, UUID saleId, long amount, String operatorName) {
        jdbcTemplate.update("""
                INSERT INTO coupon_redemption (id, coupon_id, sale_id, amount_amount, operator_name)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), couponId, saleId, amount, operatorName);
    }

    public void rollbackCouponRedemption(UUID saleId, Instant rolledBackAt) {
        jdbcTemplate.update("""
                UPDATE coupon_redemption SET rolled_back_at = ?
                WHERE sale_id = ? AND rolled_back_at IS NULL
                """, Timestamp.from(rolledBackAt), saleId);
    }

    // ------------------------------------------------------------------ refunds

    public void insertRefund(UUID refundId, String refundNo, UUID saleId, long amount, String currency,
                             String reason, String operatorName, RefundType refundType) {
        jdbcTemplate.update("""
                INSERT INTO pos_refund (id, refund_no, sale_id, amount_amount, currency, reason,
                                        operator_name, status, refund_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'COMPLETED', ?)
                """, refundId, refundNo, saleId, amount, currency, reason, operatorName, refundType.name());
    }

    public void insertRefundTender(UUID refundId, UUID tenderId, TenderType type, long amount,
                                   String provider, String providerTradeNo) {
        jdbcTemplate.update("""
                INSERT INTO pos_refund_tender (id, refund_id, tender_id, tender_type, amount_amount,
                                               provider, provider_trade_no, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'SUCCESS')
                """, UUID.randomUUID(), refundId, tenderId, type.name(), amount, provider, providerTradeNo);
    }

    public void insertRefundLine(UUID refundId, UUID saleLineId, int quantity, long amount, String currency) {
        jdbcTemplate.update("""
                INSERT INTO pos_refund_line (id, refund_id, sale_line_id, quantity, amount_amount, currency)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), refundId, saleLineId, quantity, amount, currency);
    }

    public UUID findRefundLineId(UUID refundId, UUID saleLineId) {
        List<UUID> ids = jdbcTemplate.query("""
                SELECT id FROM pos_refund_line
                WHERE refund_id = ? AND sale_line_id = ?
                ORDER BY created_at DESC
                LIMIT 1
                """, (rs, rowNum) -> rs.getObject("id", UUID.class), refundId, saleLineId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public void insertRefundAllocation(UUID refundLineId, UUID batchId, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO pos_refund_allocation (id, refund_line_id, batch_id, quantity)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), refundLineId, batchId, quantity);
    }

    public boolean addLineRefund(UUID saleLineId, int quantity, long amount) {
        int updated = jdbcTemplate.update("""
                UPDATE pos_sale_line
                SET refunded_quantity = refunded_quantity + ?,
                    refunded_amount = refunded_amount + ?
                WHERE id = ? AND refunded_quantity + ? <= quantity
                  AND refunded_amount + ? <= line_total_amount
                """, quantity, amount, saleLineId, quantity, amount);
        return updated == 1;
    }

    public boolean addAllocationRefund(UUID allocationId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE pos_sale_allocation
                SET refunded_quantity = refunded_quantity + ?
                WHERE id = ? AND refunded_quantity + ? <= quantity
                """, quantity, allocationId, quantity);
        return updated == 1;
    }

    public void addTenderRefund(UUID tenderId, long amount) {
        jdbcTemplate.update("""
                UPDATE pos_tender
                SET refunded_amount = refunded_amount + ?,
                    reversal_status = CASE WHEN refunded_amount + ? >= amount_amount
                                           THEN 'REFUNDED' ELSE 'PARTIAL' END
                WHERE id = ? AND refunded_amount + ? <= amount_amount
                """, amount, amount, tenderId, amount);
    }

    public void markSalePartiallyRefunded(UUID saleId, long additionalRefund, long pointsReversed) {
        jdbcTemplate.update("""
                UPDATE pos_sale
                SET status = 'PARTIALLY_REFUNDED',
                    refunded_amount = refunded_amount + ?,
                    points_reversed = points_reversed + ?
                WHERE id = ? AND status IN ('COMPLETED', 'PARTIALLY_REFUNDED')
                """, additionalRefund, pointsReversed, saleId);
    }

    public void markSaleFullyRefunded(UUID saleId, long additionalRefund, long pointsReversed) {
        jdbcTemplate.update("""
                UPDATE pos_sale
                SET status = 'REFUNDED',
                    refunded_amount = refunded_amount + ?,
                    points_reversed = points_reversed + ?
                WHERE id = ? AND status IN ('COMPLETED', 'PARTIALLY_REFUNDED')
                """, additionalRefund, pointsReversed, saleId);
    }

    // ------------------------------------------------------------------ catalog

    public List<PosProductView> searchProducts(UUID storeId, String keyword, int limit) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query("""
                SELECT s.id, s.sku_code, s.barcode, s.image_url, s.price_amount, s.price_currency,
                       COALESCE(pt.title, p.title) AS title,
                       COALESCE(SUM(CASE WHEN b.location_type = 'STORE' AND b.location_id = ?
                                          AND b.status IN ('AVAILABLE', 'NEAR_EXPIRY')
                                         THEN b.quantity - b.reserved_quantity ELSE 0 END), 0) AS available
                FROM sku s
                JOIN product p ON p.id = s.product_id
                LEFT JOIN product_translation pt ON pt.product_id = p.id AND pt.locale = 'zh-CN'
                LEFT JOIN inventory_batch b ON b.sku_id = s.id
                WHERE p.status = 'ACTIVE'
                  AND (s.sku_code ILIKE ? OR s.barcode ILIKE ? OR COALESCE(pt.title, p.title) ILIKE ?)
                GROUP BY s.id, s.sku_code, s.barcode, s.image_url, s.price_amount, s.price_currency,
                         p.title, pt.title
                ORDER BY available DESC, title
                LIMIT ?
                """, (rs, rowNum) -> new PosProductView(
                        rs.getObject("id", UUID.class),
                        rs.getString("sku_code"),
                        rs.getString("barcode"),
                        rs.getString("title"),
                        new Money(rs.getLong("price_amount"), rs.getString("price_currency")),
                        Math.max(rs.getInt("available"), 0),
                        rs.getString("image_url")), storeId, like, like, like, limit);
    }

    // ------------------------------------------------------------------ reports

    public DailyTotals dailyTotals(UUID storeId, LocalDate date) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FILTER (WHERE status IN ('COMPLETED', 'REFUNDED')) AS sales_count,
                       COALESCE(SUM(subtotal_amount) FILTER (WHERE status IN ('COMPLETED', 'REFUNDED')), 0) AS gross,
                       COALESCE(SUM(promotion_discount_amount + coupon_discount_amount
                                    + member_discount_amount + points_discount_amount)
                                FILTER (WHERE status IN ('COMPLETED', 'REFUNDED')), 0) AS discounts,
                       COALESCE(SUM(payable_amount) FILTER (WHERE status IN ('COMPLETED', 'REFUNDED')), 0) AS net,
                       COALESCE(SUM(points_discount_amount) FILTER (WHERE status IN ('COMPLETED', 'REFUNDED')), 0)
                           AS points_discount,
                       COALESCE(SUM(coupon_discount_amount) FILTER (WHERE status IN ('COMPLETED', 'REFUNDED')), 0)
                           AS coupon_discount,
                       COALESCE(SUM(points_redeemed) FILTER (WHERE status IN ('COMPLETED', 'REFUNDED')), 0)
                           AS points_redeemed,
                       COALESCE(SUM(points_earned) FILTER (WHERE status IN ('COMPLETED', 'REFUNDED')), 0)
                           AS points_earned,
                       COUNT(*) FILTER (WHERE coupon_id IS NOT NULL AND status IN ('COMPLETED', 'REFUNDED'))
                           AS coupons_used
                FROM pos_sale
                WHERE store_id = ? AND created_at::date = ?
                """, (rs, rowNum) -> new DailyTotals(
                rs.getInt("sales_count"),
                rs.getLong("gross"),
                rs.getLong("discounts"),
                rs.getLong("net"),
                rs.getLong("points_discount"),
                rs.getLong("coupon_discount"),
                rs.getLong("points_redeemed"),
                rs.getLong("points_earned"),
                rs.getLong("coupons_used")), storeId, date);
    }

    public long refundTotal(UUID storeId, LocalDate date) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(r.amount_amount), 0)
                FROM pos_refund r
                JOIN pos_sale s ON s.id = r.sale_id
                WHERE s.store_id = ? AND r.created_at::date = ? AND r.status = 'COMPLETED'
                """, Long.class, storeId, date);
        if (total == null) {
            throw new IllegalStateException("Refund total query returned null for store " + storeId);
        }
        return total;
    }

    public int refundCount(UUID storeId, LocalDate date) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pos_refund r
                JOIN pos_sale s ON s.id = r.sale_id
                WHERE s.store_id = ? AND r.created_at::date = ? AND r.status = 'COMPLETED'
                """, Integer.class, storeId, date);
        if (count == null) {
            throw new IllegalStateException("Refund count query returned null for store " + storeId);
        }
        return count;
    }

    public List<PosTenderTotal> tenderTotals(UUID storeId, LocalDate date) {
        return jdbcTemplate.query("""
                SELECT t.tender_type, MAX(t.currency) AS currency, SUM(t.amount_amount) AS amount
                FROM pos_tender t
                JOIN pos_sale s ON s.id = t.sale_id
                WHERE s.store_id = ? AND s.created_at::date = ? AND t.status IN ('SUCCESS', 'REVERSED')
                GROUP BY t.tender_type
                ORDER BY t.tender_type
                """, (rs, rowNum) -> new PosTenderTotal(
                rs.getString("tender_type"),
                new Money(rs.getLong("amount"), rs.getString("currency"))), storeId, date);
    }

    public long refundTotalByTender(UUID storeId, LocalDate date, String tenderType) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(rt.amount_amount), 0)
                FROM pos_refund_tender rt
                JOIN pos_refund r ON r.id = rt.refund_id
                JOIN pos_sale s ON s.id = r.sale_id
                WHERE s.store_id = ? AND r.created_at::date = ? AND r.status = 'COMPLETED'
                  AND rt.tender_type = ? AND rt.status = 'SUCCESS'
                """, Long.class, storeId, date, tenderType);
        if (total == null) {
            throw new IllegalStateException("Refund tender total query returned null for " + tenderType);
        }
        return total;
    }

    public long cashVariance(UUID storeId, LocalDate date) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(variance_amount), 0)
                FROM pos_shift
                WHERE store_id = ? AND status IN ('CLOSED', 'APPROVED') AND closed_at::date = ?
                """, Long.class, storeId, date);
        if (total == null) {
            throw new IllegalStateException("Cash variance query returned null for store " + storeId);
        }
        return total;
    }

    // ------------------------------------------------------------------ mapping

    private PosSale withChildren(PosSale sale) {
        return new PosSale(
                sale.id(), sale.saleNo(), sale.storeId(), sale.shiftId(), sale.memberId(), sale.currency(),
                sale.subtotal(), sale.promotionDiscount(), sale.couponDiscount(), sale.memberDiscount(),
                sale.pointsDiscount(), sale.payable(), sale.cashTendered(), sale.change(), sale.status(),
                sale.couponId(), sale.couponCode(), sale.pointsRedeemed(), sale.pointsEarned(), sale.pointsReversed(), sale.refunded(),
                sale.operatorName(), sale.createdAt(), sale.paidAt(), findLines(sale.id()), findTenders(sale.id()));
    }

    private List<PosSaleLine> findLines(UUID saleId) {
        return jdbcTemplate.query("""
                SELECT l.id, l.sku_id, l.title_snapshot, l.sku_code_snapshot, l.quantity,
                       l.unit_price_amount, l.line_total_amount, l.refunded_quantity,
                       l.refunded_amount, l.currency,
                       COALESCE(string_agg(b.batch_no, ', ' ORDER BY b.batch_no), '') AS batch_summary
                FROM pos_sale_line l
                LEFT JOIN pos_sale_allocation a ON a.sale_line_id = l.id
                LEFT JOIN inventory_batch b ON b.id = a.batch_id
                WHERE l.sale_id = ?
                GROUP BY l.id
                ORDER BY l.title_snapshot
                """, (rs, rowNum) -> new PosSaleLine(
                rs.getObject("id", UUID.class),
                rs.getObject("sku_id", UUID.class),
                rs.getString("title_snapshot"),
                rs.getString("sku_code_snapshot"),
                rs.getInt("quantity"),
                new Money(rs.getLong("unit_price_amount"), rs.getString("currency")),
                new Money(rs.getLong("line_total_amount"), rs.getString("currency")),
                rs.getInt("refunded_quantity"),
                new Money(rs.getLong("refunded_amount"), rs.getString("currency")),
                rs.getString("batch_summary")), saleId);
    }

    private PosSale mapSale(ResultSet rs) throws SQLException {
        String currency = rs.getString("currency");
        return new PosSale(
                rs.getObject("id", UUID.class),
                rs.getString("sale_no"),
                rs.getObject("store_id", UUID.class),
                rs.getObject("shift_id", UUID.class),
                rs.getObject("member_id", UUID.class),
                currency,
                new Money(rs.getLong("subtotal_amount"), currency),
                new Money(rs.getLong("promotion_discount_amount"), currency),
                new Money(rs.getLong("coupon_discount_amount"), currency),
                new Money(rs.getLong("member_discount_amount"), currency),
                new Money(rs.getLong("points_discount_amount"), currency),
                new Money(rs.getLong("payable_amount"), currency),
                new Money(rs.getLong("cash_tendered_amount"), currency),
                new Money(rs.getLong("change_amount"), currency),
                PosSaleStatus.valueOf(rs.getString("status")),
                rs.getObject("coupon_id", UUID.class),
                rs.getString("coupon_code"),
                rs.getLong("points_redeemed"),
                rs.getLong("points_earned"),
                rs.getLong("points_reversed"),
                new Money(rs.getLong("refunded_amount"), currency),
                rs.getString("operator_name"),
                instant(rs, "created_at"),
                instant(rs, "paid_at"),
                List.of(),
                List.of());
    }

    private PosShift mapShift(ResultSet rs) throws SQLException {
        String currency = rs.getString("currency");
        return new PosShift(
                rs.getObject("id", UUID.class),
                rs.getObject("store_id", UUID.class),
                rs.getObject("cashier_id", UUID.class),
                rs.getString("cashier_name"),
                PosShiftStatus.valueOf(rs.getString("status")),
                new Money(rs.getLong("opening_float_amount"), currency),
                nullableMoney(rs, "expected_cash_amount", currency),
                nullableMoney(rs, "actual_cash_amount", currency),
                nullableMoney(rs, "variance_amount", currency),
                rs.getString("variance_reason"),
                instant(rs, "opened_at"),
                instant(rs, "closed_at"));
    }

    private PosTender mapTender(ResultSet rs) throws SQLException {
        String currency = rs.getString("currency");
        return new PosTender(
                rs.getObject("id", UUID.class),
                TenderType.valueOf(rs.getString("tender_type")),
                new Money(rs.getLong("amount_amount"), currency),
                rs.getString("provider"),
                rs.getString("provider_trade_no"),
                TenderStatus.valueOf(rs.getString("status")),
                nullableMoney(rs, "cash_tendered_amount", currency),
                nullableMoney(rs, "change_amount", currency),
                nullableMoney(rs, "refunded_amount", currency),
                rs.getString("reversal_status"),
                instant(rs, "paid_at"));
    }

    private PosMember mapMember(ResultSet rs) throws SQLException {
        return new PosMember(
                rs.getObject("id", UUID.class),
                rs.getString("member_no"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("tier"),
                rs.getDate("birthday") == null ? null : rs.getDate("birthday").toLocalDate(),
                rs.getLong("growth_value"),
                rs.getObject("birthday_gift_year", Integer.class),
                rs.getLong("points_balance"),
                new Money(rs.getLong("stored_value_balance"), rs.getString("currency")));
    }

    private PosCoupon mapCoupon(ResultSet rs) throws SQLException {
        String currency = rs.getString("currency");
        Long maxDiscount = rs.getObject("max_discount_amount", Long.class);
        return new PosCoupon(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                CouponDiscountType.valueOf(rs.getString("discount_type")),
                rs.getLong("discount_value"),
                new Money(rs.getLong("min_spend_amount"), currency),
                maxDiscount == null ? null : new Money(maxDiscount, currency),
                currency,
                CouponStatus.valueOf(rs.getString("status")),
                rs.getObject("member_id", UUID.class),
                instant(rs, "valid_from"),
                instant(rs, "valid_to"));
    }

    private Money nullableMoney(ResultSet rs, String column, String currency) throws SQLException {
        Long amount = rs.getObject(column, Long.class);
        return amount == null ? null : new Money(amount, currency);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    public record DailyTotals(
            int salesCount,
            long gross,
            long discounts,
            long net,
            long pointsDiscount,
            long couponDiscount,
            long pointsRedeemed,
            long pointsEarned,
            long couponsUsed
    ) {
    }
}