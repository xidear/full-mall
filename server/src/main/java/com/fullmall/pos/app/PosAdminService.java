package com.fullmall.pos.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.Money;
import com.fullmall.platform.util.Ids;
import com.fullmall.pos.api.AdjustPointsRequest;
import com.fullmall.pos.api.CreateCouponRequest;
import com.fullmall.pos.api.CreateMemberRequest;
import com.fullmall.pos.api.MemberAdminView;
import com.fullmall.pos.api.TopUpStoredValueRequest;
import com.fullmall.pos.api.UpdateMemberLevelRequest;
import com.fullmall.pos.domain.CouponDiscountType;
import com.fullmall.pos.domain.CouponStatus;
import com.fullmall.pos.domain.MemberCardEntryType;
import com.fullmall.pos.domain.MemberLevel;
import com.fullmall.pos.domain.PointsEntryType;
import com.fullmall.pos.domain.PosCoupon;
import com.fullmall.pos.domain.PosMember;
import com.fullmall.pos.infra.PosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PosAdminService {

    private static final String DEFAULT_CURRENCY = "CNY";
    private static final Set<String> TIERS = Set.of("STANDARD", "SILVER", "GOLD", "PLATINUM");

    private final PosRepository repository;

    public PosAdminService(PosRepository repository) {
        this.repository = repository;
    }

    public List<MemberAdminView> listMembers(String query) {
        return repository.listMembers(query, 200);
    }

    @Transactional
    public PosMember createMember(CreateMemberRequest request) {
        String phone = request.phone().trim();
        if (repository.findMemberByQuery(phone) != null) {
            throw ApiException.conflict("POS_MEMBER_PHONE_EXISTS", "手机号已注册会员：" + phone);
        }
        String tier = request.tier() == null || request.tier().isBlank()
                ? "STANDARD"
                : request.tier().trim().toUpperCase(Locale.ROOT);
        if (!TIERS.contains(tier)) {
            throw ApiException.badRequest("POS_MEMBER_TIER_INVALID",
                    "会员等级必须是 " + TIERS + " 之一，实际为 " + tier);
        }
        String memberNo = "M" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
        PosMember member = new PosMember(
                Ids.newId(),
                memberNo,
                request.name().trim(),
                phone,
                tier,
                request.birthday(),
                0,
                null,
                0,
                new Money(0, DEFAULT_CURRENCY));
        repository.insertMember(member);
        return repository.findMember(member.id());
    }

    @Transactional
    public PosMember adjustPoints(UUID memberId, AdjustPointsRequest request) {
        PosMember member = requireMember(memberId);
        long points = request.points();
        if (points == 0) {
            throw ApiException.badRequest("POS_POINTS_ADJUST_ZERO", "积分调整值不能为 0");
        }
        if (!repository.addPoints(memberId, points)) {
            throw ApiException.conflict("POS_INSUFFICIENT_POINTS", "会员积分余额不足，不能扣减 " + (-points));
        }
        long balanceAfter = repository.pointsBalance(memberId);
        repository.insertPointsLedger(memberId, null, PointsEntryType.ADMIN_ADJUST, points, balanceAfter,
                request.reason().trim(), "ADMIN_POINTS:" + UUID.randomUUID());
        return repository.findMember(member.id());
    }

    @Transactional
    public PosMember topUpStoredValue(UUID memberId, TopUpStoredValueRequest request) {
        PosMember member = requireMember(memberId);
        repository.addStoredValue(memberId, request.amount());
        long balanceAfter = repository.storedValueBalance(memberId);
        repository.insertCardLedger(memberId, null, MemberCardEntryType.TOPUP, request.amount(),
                balanceAfter, member.storedValue().currency(), request.reason().trim(),
                "ADMIN_TOPUP:" + UUID.randomUUID());
        return repository.findMember(member.id());
    }

    public List<MemberLevel> listMemberLevels() {
        return repository.listMemberLevels();
    }

    @Transactional
    public MemberLevel updateMemberLevel(String code, UpdateMemberLevelRequest request) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        repository.updateMemberLevel(normalized, request.discountRate(),
                request.pointsMultiplier(), request.birthdayPoints());
        return repository.findMemberLevel(normalized);
    }

    public List<PosCoupon> listCoupons() {
        return repository.listCoupons(200);
    }

    @Transactional
    public PosCoupon createCoupon(CreateCouponRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (repository.findCouponForUpdate(code) != null) {
            throw ApiException.conflict("POS_COUPON_CODE_EXISTS", "优惠券编码已存在：" + code);
        }
        CouponDiscountType type;
        try {
            type = CouponDiscountType.valueOf(request.discountType().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("POS_COUPON_TYPE_INVALID",
                    "优惠券类型必须是 FIXED 或 PERCENT，实际为 " + request.discountType());
        }
        if (type == CouponDiscountType.PERCENT && request.discountValue() > 100) {
            throw ApiException.badRequest("POS_COUPON_PERCENT_INVALID", "折扣百分比不能超过 100");
        }
        if (!request.validFrom().isBefore(request.validTo())) {
            throw ApiException.badRequest("POS_COUPON_VALIDITY_INVALID", "生效时间必须早于失效时间");
        }
        if (request.memberId() != null) {
            requireMember(request.memberId());
        }
        String currency = request.currency() == null || request.currency().isBlank()
                ? DEFAULT_CURRENCY
                : request.currency().trim().toUpperCase(Locale.ROOT);
        Long maxDiscount = request.maxDiscountAmount();
        if (maxDiscount != null && maxDiscount <= 0) {
            throw ApiException.badRequest("POS_COUPON_MAX_DISCOUNT_INVALID", "最高优惠金额必须大于 0");
        }
        PosCoupon coupon = new PosCoupon(
                Ids.newId(),
                code,
                request.name().trim(),
                type,
                request.discountValue(),
                new Money(request.minSpendAmount(), currency),
                maxDiscount == null ? null : new Money(maxDiscount, currency),
                currency,
                CouponStatus.ISSUED,
                request.memberId(),
                request.validFrom(),
                request.validTo());
        repository.insertCoupon(coupon, request.memberId());
        return coupon;
    }

    private PosMember requireMember(UUID memberId) {
        PosMember member = repository.findMember(memberId);
        if (member == null) {
            throw ApiException.notFound("POS_MEMBER_NOT_FOUND", "会员不存在：" + memberId);
        }
        return member;
    }
}