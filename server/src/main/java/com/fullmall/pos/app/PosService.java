package com.fullmall.pos.app;

import com.fullmall.analytics.app.AnalyticsService;
import com.fullmall.catalog.app.CatalogService;
import com.fullmall.catalog.domain.Product;
import com.fullmall.catalog.domain.ProductStatus;
import com.fullmall.catalog.domain.Sku;
import com.fullmall.inventory.app.InventoryService;
import com.fullmall.inventory.domain.ReservationAllocation;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.Money;
import com.fullmall.platform.util.Ids;
import com.fullmall.pos.api.AddTenderRequest;
import com.fullmall.pos.api.AttachMemberRequest;
import com.fullmall.pos.api.ApplyCouponRequest;
import com.fullmall.pos.api.CashMovementRequest;
import com.fullmall.pos.api.CloseShiftRequest;
import com.fullmall.pos.api.CreatePosSaleRequest;
import com.fullmall.pos.api.OpenShiftRequest;
import com.fullmall.pos.api.PosDailyReport;
import com.fullmall.pos.api.PosMemberView;
import com.fullmall.pos.api.PosProductView;
import com.fullmall.pos.api.PosProviderView;
import com.fullmall.pos.api.PosReconciliationReport;
import com.fullmall.pos.api.PosSaleView;
import com.fullmall.pos.api.PosShiftView;
import com.fullmall.pos.api.PosTenderTotal;
import com.fullmall.pos.api.RedeemPointsRequest;
import com.fullmall.pos.api.RefundPosLinesRequest;
import com.fullmall.pos.api.RefundPosSaleRequest;
import com.fullmall.pos.domain.CashMovementType;
import com.fullmall.pos.domain.CouponStatus;
import com.fullmall.pos.domain.MemberCardEntryType;
import com.fullmall.pos.domain.MemberLevel;
import com.fullmall.pos.domain.PointsEntryType;
import com.fullmall.pos.domain.PosCalculator;
import com.fullmall.pos.domain.PosCoupon;
import com.fullmall.pos.domain.PosMember;
import com.fullmall.pos.domain.PosSale;
import com.fullmall.pos.domain.PosSaleAllocation;
import com.fullmall.pos.domain.PosSaleLine;
import com.fullmall.pos.domain.PosSaleStatus;
import com.fullmall.pos.domain.RefundType;
import com.fullmall.pos.domain.PosShift;
import com.fullmall.pos.domain.PosShiftStatus;
import com.fullmall.pos.domain.PosTender;
import com.fullmall.pos.domain.TenderStatus;
import com.fullmall.pos.domain.TenderType;
import com.fullmall.pos.infra.PosRepository;
import com.fullmall.staff.app.StaffAuthService;
import com.fullmall.staff.domain.StaffUser;
import com.fullmall.store.app.StoreService;
import com.fullmall.store.domain.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class PosService {

    private static final String DEFAULT_CURRENCY = "CNY";
    private static final String INTERNAL_PROVIDER = "internal";
    private static final String DEFAULT_PROVIDER = MockPosPaymentProvider.PROVIDER;

    private final PosRepository repository;
    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final StoreService storeService;
    private final AnalyticsService analyticsService;
    private final StaffAuthService staffAuthService;
    private final boolean requireManagerApproval;
    private final Map<String, PosPaymentProvider> providers;

    public PosService(PosRepository repository,
                      CatalogService catalogService,
                      InventoryService inventoryService,
                      StoreService storeService,
                      AnalyticsService analyticsService,
                      StaffAuthService staffAuthService,
                      @org.springframework.beans.factory.annotation.Value("${app.pos.require-manager-approval:false}")
                      boolean requireManagerApproval,
                      List<PosPaymentProvider> paymentProviders) {
        this.repository = repository;
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.storeService = storeService;
        this.analyticsService = analyticsService;
        this.staffAuthService = staffAuthService;
        this.requireManagerApproval = requireManagerApproval;
        this.providers = new LinkedHashMap<>();
        for (PosPaymentProvider provider : paymentProviders) {
            PosPaymentProvider previous = providers.put(provider.provider(), provider);
            if (previous != null) {
                throw new IllegalStateException("Duplicate POS payment provider: " + provider.provider());
            }
        }
    }

    // ------------------------------------------------------------------ shifts

    @Transactional
    public PosShiftView openShift(OpenShiftRequest request) {
        Store store = storeService.getStore(request.storeId());
        String cashierName = request.cashierName().trim();
        if (repository.findOpenShift(store.id(), cashierName) != null) {
            throw ApiException.conflict("POS_SHIFT_ALREADY_OPEN",
                    "收银员 " + cashierName + " 在该门店已有未关闭的班次");
        }
        String currency = request.currency() == null || request.currency().isBlank()
                ? DEFAULT_CURRENCY
                : request.currency().trim().toUpperCase();
        Instant now = Instant.now();
        PosShift shift = new PosShift(
                Ids.newId(),
                store.id(),
                null,
                cashierName,
                PosShiftStatus.OPEN,
                new Money(request.openingFloat(), currency),
                null,
                null,
                null,
                null,
                now,
                null);
        repository.insertShift(shift);
        repository.insertCashMovement(shift.id(), null, CashMovementType.OPENING_FLOAT.name(),
                request.openingFloat(), currency, "开班备用金", cashierName);
        analyticsService.record("pos_shift_opened", null, null, Map.of(
                "storeId", store.id().toString(),
                "cashierName", cashierName,
                "openingFloat", request.openingFloat()));
        return shiftView(shift);
    }

    public PosShiftView findOpenShift(UUID storeId, String cashierName) {
        PosShift shift = repository.findOpenShift(storeId, cashierName);
        return shift == null ? null : shiftView(shift);
    }

    @Transactional
    public PosShiftView closeShift(UUID shiftId, CloseShiftRequest request) {
        PosShift shift = requireShift(shiftId);
        if (shift.status() != PosShiftStatus.OPEN) {
            throw ApiException.conflict("POS_SHIFT_NOT_OPEN", "班次当前状态为 " + shift.status() + "，不能关班");
        }
        long expected = repository.cashTotal(shiftId);
        long actual = request.actualCash();
        long variance = actual - expected;
        String reason = request.reason() == null ? null : request.reason().trim();
        if (variance != 0 && (reason == null || reason.isEmpty())) {
            throw ApiException.badRequest("POS_CASH_VARIANCE_REASON_REQUIRED",
                    "现金实盘与应有金额相差 " + variance + " 分，请填写差异原因后再关班");
        }
        repository.closeShift(shiftId, expected, actual, variance, reason, Instant.now());
        analyticsService.record("pos_shift_closed", null, null, Map.of(
                "shiftId", shiftId.toString(),
                "expectedCash", expected,
                "actualCash", actual,
                "variance", variance));
        return shiftView(requireShift(shiftId));
    }

    @Transactional
    public PosShiftView cashMovement(UUID shiftId, CashMovementRequest request) {
        PosShift shift = requireShift(shiftId);
        if (shift.status() != PosShiftStatus.OPEN) {
            throw ApiException.conflict("POS_SHIFT_NOT_OPEN", "班次已关闭，不能登记现金收付");
        }
        CashMovementType type;
        try {
            type = CashMovementType.valueOf(request.type().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("POS_CASH_MOVEMENT_TYPE_INVALID",
                    "现金流水类型必须是 CASH_IN 或 CASH_OUT，实际为 " + request.type());
        }
        if (type != CashMovementType.CASH_IN && type != CashMovementType.CASH_OUT) {
            throw ApiException.badRequest("POS_CASH_MOVEMENT_TYPE_INVALID",
                    "现金流水类型必须是 CASH_IN 或 CASH_OUT，实际为 " + type);
        }
        long signed = type == CashMovementType.CASH_IN ? request.amount() : -request.amount();
        String operator = request.operatorName() == null || request.operatorName().isBlank()
                ? shift.cashierName()
                : request.operatorName().trim();
        repository.insertCashMovement(shiftId, null, type.name(), signed,
                shift.openingFloat().currency(), request.reason().trim(), operator);
        return shiftView(requireShift(shiftId));
    }

    public PosShiftView getShift(UUID shiftId) {
        return shiftView(requireShift(shiftId));
    }

    public List<PosSaleView> findShiftSales(UUID shiftId) {
        requireShift(shiftId);
        return repository.findShiftSales(shiftId, 30).stream().map(this::saleView).toList();
    }

    // ------------------------------------------------------------------ catalog and members

    public List<PosProductView> searchProducts(UUID storeId, String keyword) {
        storeService.getStore(storeId);
        return repository.searchProducts(storeId, keyword == null ? "" : keyword, 30);
    }

    public PosMemberView lookupMember(String query) {
        if (query == null || query.isBlank()) {
            throw ApiException.badRequest("POS_MEMBER_QUERY_REQUIRED", "请输入会员手机号或会员编号");
        }
        PosMember member = repository.findMemberByQuery(query.trim());
        if (member == null) {
            throw ApiException.notFound("POS_MEMBER_NOT_FOUND", "未找到会员：" + query.trim());
        }
        return new PosMemberView(member, repository.findUsableCoupons(member.id()));
    }

    public List<PosProviderView> providers() {
        return List.of(
                new PosProviderView("CASH", "现金", false, false),
                new PosProviderView("WECHAT", "微信扫码", false, true),
                new PosProviderView("ALIPAY", "支付宝（含碰一碰）", false, true),
                new PosProviderView("CARD", "银行卡", false, true),
                new PosProviderView("STORED_VALUE", "会员储值", true, false),
                new PosProviderView("OTHER", "其他收款", false, true));
    }

    // ------------------------------------------------------------------ sales

    @Transactional
    public PosSaleView createSale(CreatePosSaleRequest request, String idempotencyKey) {
        PosShift shift = requireShift(request.shiftId());
        if (shift.status() != PosShiftStatus.OPEN) {
            throw ApiException.conflict("POS_SHIFT_NOT_OPEN", "班次已关闭，不能开单");
        }
        if (!shift.storeId().equals(request.storeId())) {
            throw ApiException.badRequest("POS_SHIFT_STORE_MISMATCH", "班次不属于所选门店");
        }
        Store store = storeService.getStore(request.storeId());
        String key = normalizeKey(idempotencyKey);
        String requestHash = key == null ? null : hash(request);
        if (key != null) {
            repository.lockIdempotency("pos-sale:" + store.id() + ":" + key);
            PosSale existing = repository.findSaleByKey(store.id(), key);
            if (existing != null) {
                if (!Objects.equals(repository.findSaleRequestHash(existing.id()), requestHash)) {
                    throw ApiException.conflict("IDEMPOTENCY_KEY_REUSED",
                            "Idempotency-Key 已用于不同的销售单内容");
                }
                return saleView(existing);
            }
        }

        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (CreatePosSaleRequest.Item item : request.items()) {
            if (item.quantity() <= 0) {
                throw ApiException.badRequest("POS_ITEM_QUANTITY_INVALID", "商品数量必须大于 0");
            }
            try {
                quantities.merge(item.skuId(), item.quantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw ApiException.badRequest("POS_ITEM_QUANTITY_OVERFLOW", "商品数量超出可处理范围");
            }
        }

        String currency = null;
        List<ResolvedItem> resolved = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            Sku sku = catalogService.getSku(entry.getKey());
            Product product = catalogService.getProduct(sku.productId(), "zh-CN");
            if (product.status() != ProductStatus.ACTIVE) {
                throw ApiException.conflict("PRODUCT_NOT_ACTIVE", "商品未上架：" + product.title());
            }
            if (currency == null) {
                currency = sku.price().currency();
            } else if (!currency.equals(sku.price().currency())) {
                throw ApiException.badRequest("CURRENCY_MISMATCH", "同一销售单的商品币种必须一致");
            }
            resolved.add(new ResolvedItem(sku, product.title(), entry.getValue()));
        }
        if (currency == null) {
            throw ApiException.badRequest("POS_SALE_EMPTY", "销售单至少需要一个商品");
        }

        long subtotal = 0;
        for (ResolvedItem item : resolved) {
            subtotal = Math.addExact(subtotal, item.sku().price().amount() * (long) item.quantity());
        }

        UUID saleId = Ids.newId();
        String operator = request.cashierName() == null || request.cashierName().isBlank()
                ? shift.cashierName()
                : request.cashierName().trim();
        PosSale sale = new PosSale(
                saleId,
                "POS-" + Ids.orderNo(),
                store.id(),
                shift.id(),
                request.memberId(),
                currency,
                new Money(subtotal, currency),
                Money.zero(currency),
                Money.zero(currency),
                Money.zero(currency),
                Money.zero(currency),
                new Money(subtotal, currency),
                Money.zero(currency),
                Money.zero(currency),
                PosSaleStatus.DRAFT,
                null,
                null,
                0,
                0,
                0,
                Money.zero(currency),
                operator,
                Instant.now(),
                null,
                List.of(),
                List.of());
        repository.insertSale(sale, key, requestHash);

        for (ResolvedItem item : resolved) {
            UUID lineId = Ids.newId();
            long lineTotal = item.sku().price().amount() * (long) item.quantity();
            PosSaleLine line = new PosSaleLine(
                    lineId,
                    item.sku().id(),
                    item.title(),
                    item.sku().skuCode(),
                    item.quantity(),
                    item.sku().price(),
                    new Money(lineTotal, currency),
                    0,
                    Money.zero(currency),
                    null);
            repository.insertSaleLine(saleId, line, currency);
            List<ReservationAllocation> allocations = inventoryService.reserveForPosLine(
                    saleId, lineId, item.sku().id(), item.quantity(), store.id(), item.sku().shelfLifePolicy());
            for (ReservationAllocation allocation : allocations) {
                repository.insertAllocation(lineId, allocation.batchId(), allocation.quantity(), null, currency);
            }
        }

        analyticsService.record("pos_sale_created", saleId, null, Map.of(
                "storeId", store.id().toString(),
                "subtotal", subtotal,
                "currency", currency,
                "itemCount", resolved.size()));
        return saleView(requireSale(saleId));
    }

    @Transactional
    public PosSaleView attachMember(UUID saleId, AttachMemberRequest request) {
        PosSale sale = lockSale(saleId);
        if (sale.status() != PosSaleStatus.DRAFT) {
            throw ApiException.conflict("POS_SALE_NOT_DRAFT", "只有草稿销售单可以绑定会员");
        }
        PosMember member = repository.findMember(request.memberId());
        if (member == null) {
            throw ApiException.notFound("POS_MEMBER_NOT_FOUND", "会员不存在：" + request.memberId());
        }
        if (!member.storedValue().currency().equals(sale.currency())) {
            throw ApiException.conflict("POS_MEMBER_CURRENCY_MISMATCH", "会员币种与销售单币种不一致");
        }
        if (repository.successfulTenderTotal(saleId) > 0) {
            throw ApiException.conflict("POS_SALE_ALREADY_PAYING", "已有收款记录，不能再更换会员");
        }
        if (sale.pointsRedeemed() > 0) {
            throw ApiException.conflict("POS_POINTS_ALREADY_REDEEMED", "已使用积分抵扣，不能再更换会员");
        }
        if (sale.couponId() != null) {
            PosCoupon coupon = repository.findCoupon(sale.couponId());
            if (coupon != null && coupon.memberId() != null && !coupon.memberId().equals(member.id())) {
                throw ApiException.conflict("POS_COUPON_MEMBER_MISMATCH", "已使用的优惠券属于其他会员");
            }
        }
        MemberLevel level = repository.findMemberLevel(member.tier());
        if (level == null) {
            throw ApiException.conflict("POS_MEMBER_LEVEL_NOT_CONFIGURED", "会员等级未配置：" + member.tier());
        }
        long subtotal = sale.subtotal().amount();
        long memberDiscount = (subtotal * (100L - level.discountRate()) + 50) / 100;
        long couponDiscount = 0;
        if (sale.couponId() != null) {
            PosCoupon coupon = repository.findCoupon(sale.couponId());
            if (coupon != null) {
                long base = subtotal - sale.promotionDiscount().amount() - memberDiscount;
                couponDiscount = PosCalculator.couponDiscount(coupon.discountType(), coupon.discountValue(),
                        coupon.maxDiscount() == null ? null : coupon.maxDiscount().amount(), base);
            }
        }
        long payable = PosCalculator.payable(subtotal, sale.promotionDiscount().amount(),
                couponDiscount, memberDiscount, 0);
        repository.updateSaleMemberBenefits(saleId, member.id(), memberDiscount, couponDiscount, 0, payable);
        return saleView(requireSale(saleId));
    }

    @Transactional
    public PosSaleView applyCoupon(UUID saleId, ApplyCouponRequest request) {
        PosSale sale = lockSale(saleId);
        if (sale.status() != PosSaleStatus.DRAFT) {
            throw ApiException.conflict("POS_SALE_NOT_DRAFT", "只有草稿销售单可以使用优惠券");
        }
        if (repository.successfulTenderTotal(saleId) > 0) {
            throw ApiException.conflict("POS_SALE_ALREADY_PAYING", "已有收款记录，不能再修改优惠");
        }
        if (sale.pointsRedeemed() > 0) {
            throw ApiException.conflict("POS_POINTS_ALREADY_REDEEMED", "已使用积分抵扣，不能再更换优惠券");
        }
        PosCoupon coupon = repository.findCouponForUpdate(request.code().trim().toUpperCase());
        if (coupon == null) {
            throw ApiException.notFound("POS_COUPON_NOT_FOUND", "优惠券不存在：" + request.code());
        }
        if (sale.couponId() != null && sale.couponId().equals(coupon.id())) {
            return saleView(sale);
        }
        Instant now = Instant.now();
        if (coupon.status() != CouponStatus.ISSUED) {
            throw ApiException.conflict("POS_COUPON_NOT_AVAILABLE", "优惠券状态为 " + coupon.status());
        }
        if (coupon.validFrom().isAfter(now) || coupon.validTo().isBefore(now)) {
            throw ApiException.conflict("POS_COUPON_NOT_AVAILABLE", "优惠券不在有效期内");
        }
        if (!coupon.minSpend().currency().equals(sale.currency())) {
            throw ApiException.conflict("POS_COUPON_CURRENCY_MISMATCH", "优惠券币种与销售单不一致");
        }
        if (coupon.memberId() != null && !coupon.memberId().equals(sale.memberId())) {
            throw ApiException.conflict("POS_COUPON_MEMBER_MISMATCH", "该优惠券仅限指定会员使用");
        }
        if (sale.subtotal().amount() < coupon.minSpend().amount()) {
            throw ApiException.conflict("POS_COUPON_MIN_SPEND",
                    "未达到优惠券门槛：" + coupon.minSpend().amount() + " 分");
        }
        if (sale.couponId() != null) {
            repository.releaseCoupon(sale.couponId());
        }
        if (!repository.lockCoupon(coupon.id(), saleId, now)) {
            throw ApiException.conflict("POS_COUPON_CONCURRENT_USE", "优惠券已被其他收银台占用");
        }
        long base = sale.subtotal().amount()
                - sale.promotionDiscount().amount()
                - sale.memberDiscount().amount();
        long discount = PosCalculator.couponDiscount(
                coupon.discountType(),
                coupon.discountValue(),
                coupon.maxDiscount() == null ? null : coupon.maxDiscount().amount(),
                base);
        long payable = PosCalculator.payable(
                sale.subtotal().amount(),
                sale.promotionDiscount().amount(),
                discount,
                sale.memberDiscount().amount(),
                0);
        repository.updateSaleBenefits(saleId, coupon.id(), discount, 0, 0, payable);
        return saleView(requireSale(saleId));
    }

    @Transactional
    public PosSaleView redeemPoints(UUID saleId, RedeemPointsRequest request) {
        PosSale sale = lockSale(saleId);
        if (sale.status() != PosSaleStatus.DRAFT) {
            throw ApiException.conflict("POS_SALE_NOT_DRAFT", "只有草稿销售单可以使用积分");
        }
        if (repository.successfulTenderTotal(saleId) > 0) {
            throw ApiException.conflict("POS_SALE_ALREADY_PAYING", "已有收款记录，不能再修改积分抵扣");
        }
        if (sale.pointsRedeemed() > 0) {
            throw ApiException.conflict("POS_POINTS_ALREADY_REDEEMED", "本单已经使用过积分抵扣");
        }
        if (sale.memberId() == null) {
            throw ApiException.badRequest("POS_MEMBER_REQUIRED", "使用积分前请先识别会员");
        }
        long base = sale.subtotal().amount()
                - sale.promotionDiscount().amount()
                - sale.couponDiscount().amount()
                - sale.memberDiscount().amount();
        long points = request.points();
        if (points > base) {
            throw ApiException.badRequest("POS_POINTS_EXCEED_PAYABLE",
                    "积分抵扣不能超过应收金额，最多可抵扣 " + base + " 积分");
        }
        if (!repository.decrementPoints(sale.memberId(), points)) {
            throw ApiException.conflict("POS_INSUFFICIENT_POINTS", "会员积分余额不足");
        }
        long balanceAfter = repository.pointsBalance(sale.memberId());
        repository.insertPointsLedger(sale.memberId(), saleId, PointsEntryType.REDEEM, -points,
                balanceAfter, "收银积分抵扣", "POS_SALE:" + saleId + ":REDEEM");
        long payable = base - points;
        repository.updateSaleBenefits(saleId, sale.couponId(), sale.couponDiscount().amount(),
                points, points, payable);
        return saleView(requireSale(saleId));
    }

    @Transactional
    public PosSaleView addTender(UUID saleId, AddTenderRequest request) {
        PosSale sale = lockSale(saleId);
        if (sale.status() != PosSaleStatus.DRAFT) {
            throw ApiException.conflict("POS_SALE_NOT_DRAFT", "只有草稿销售单可以收款");
        }
        String key = normalizeKey(request.idempotencyKey());
        if (key != null) {
            PosTender existing = repository.findTenderByKey(saleId, key);
            if (existing != null) {
                return saleView(sale);
            }
        }
        TenderType type;
        try {
            type = TenderType.valueOf(request.type().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("POS_TENDER_TYPE_INVALID", "不支持的收款方式：" + request.type());
        }
        long paid = repository.successfulTenderTotal(saleId);
        long remaining = sale.payable().amount() - paid;
        if (remaining <= 0) {
            throw ApiException.conflict("POS_SALE_ALREADY_PAID", "销售单已收满，无需继续收款");
        }
        if (request.amount() > remaining) {
            throw ApiException.badRequest("POS_TENDER_EXCEEDS_REMAINING",
                    "本次收款超过待收金额：待收 " + remaining + " 分，本次 " + request.amount() + " 分");
        }
        String operator = request.operatorName() == null || request.operatorName().isBlank()
                ? sale.operatorName()
                : request.operatorName().trim();
        String currency = sale.currency();
        long amount = request.amount();
        long cashTendered = 0;
        long change = 0;
        String providerName = INTERNAL_PROVIDER;
        String providerTradeNo = null;
        TenderStatus status = TenderStatus.SUCCESS;

        switch (type) {
            case CASH -> {
                cashTendered = request.cashTendered() == null ? amount : request.cashTendered();
                if (cashTendered < amount) {
                    throw ApiException.badRequest("POS_CASH_INSUFFICIENT",
                            "实收现金 " + cashTendered + " 分少于本次收款 " + amount + " 分");
                }
                change = cashTendered - amount;
                if (change > 0 && amount != remaining) {
                    throw ApiException.badRequest("POS_CASH_CHANGE_MUST_BE_LAST",
                            "现金找零必须在最后一笔收款完成，当前待收 " + remaining + " 分");
                }
                repository.insertCashMovement(sale.shiftId(), saleId, CashMovementType.SALE.name(),
                        amount, currency, "现金销售", operator);
            }
            case STORED_VALUE -> {
                if (sale.memberId() == null) {
                    throw ApiException.badRequest("POS_MEMBER_REQUIRED", "储值支付前请先识别会员");
                }
                if (!repository.decrementStoredValue(sale.memberId(), amount)) {
                    throw ApiException.conflict("POS_STORED_VALUE_INSUFFICIENT", "会员储值余额不足");
                }
                long balanceAfter = repository.storedValueBalance(sale.memberId());
                repository.insertCardLedger(sale.memberId(), saleId, MemberCardEntryType.PURCHASE,
                        -amount, balanceAfter, currency, "收银储值消费", "POS_SALE:" + saleId + ":STORED:" + amount);
                providerName = INTERNAL_PROVIDER;
            }
            default -> {
                String requestedProvider = request.provider() == null || request.provider().isBlank()
                        ? DEFAULT_PROVIDER
                        : request.provider().trim();
                PosPaymentProvider provider = providers.get(requestedProvider);
                if (provider == null) {
                    throw ApiException.badRequest("POS_PROVIDER_NOT_FOUND", "支付适配器未安装：" + requestedProvider);
                }
                PosPaymentProvider.PosPaymentIntent intent = provider.create(
                        new PosPaymentProvider.PosPaymentRequest(saleId, sale.saleNo(), type, amount, currency));
                if (!intent.result().success()) {
                    throw ApiException.conflict("POS_TENDER_FAILED",
                            "收款失败：" + intent.result().message() + "（流水 " + intent.providerTradeNo() + "）");
                }
                providerName = provider.provider();
                providerTradeNo = intent.providerTradeNo();
            }
        }

        PosTender tender = new PosTender(
                Ids.newId(),
                type,
                new Money(amount, currency),
                providerName,
                providerTradeNo,
                status,
                type == TenderType.CASH ? new Money(cashTendered, currency) : null,
                type == TenderType.CASH ? new Money(change, currency) : null,
                Money.zero(currency),
                null,
                Instant.now());
        repository.insertTender(saleId, tender, key);
        if (type == TenderType.CASH) {
            repository.updateSaleCash(saleId,
                    sale.cashTendered().amount() + cashTendered,
                    sale.change().amount() + change);
        }
        return saleView(requireSale(saleId));
    }

    @Transactional
    public PosSaleView completeSale(UUID saleId) {
        PosSale sale = lockSale(saleId);
        if (sale.status() != PosSaleStatus.DRAFT) {
            throw ApiException.conflict("POS_SALE_NOT_DRAFT", "销售单当前状态为 " + sale.status() + "，不能完成");
        }
        long paid = repository.successfulTenderTotal(saleId);
        if (paid != sale.payable().amount()) {
            throw ApiException.conflict("POS_SALE_BALANCE_REMAINING",
                    "收款金额与应收不一致：应收 " + sale.payable().amount() + " 分，已收 " + paid + " 分");
        }
        Instant now = Instant.now();
        inventoryService.confirmPosSaleReservations(saleId);
        inventoryService.shipPosSale(saleId);

        if (sale.couponId() != null) {
            if (!repository.markCouponUsed(sale.couponId(), saleId, now)) {
                throw ApiException.conflict("POS_COUPON_STATE_CHANGED", "优惠券状态已变化，无法核销");
            }
            repository.insertCouponRedemption(sale.couponId(), saleId,
                    sale.couponDiscount().amount(), sale.operatorName());
        }
        long earned = 0;
        if (sale.memberId() != null) {
            PosMember member = repository.findMember(sale.memberId());
            MemberLevel level = member == null ? null : repository.findMemberLevel(member.tier());
            int multiplier = level == null ? 1 : level.pointsMultiplier();
            earned = PosCalculator.pointsEarned(sale.payable().amount()) * multiplier;
            if (earned > 0) {
                repository.incrementPoints(sale.memberId(), earned);
                long balanceAfter = repository.pointsBalance(sale.memberId());
                repository.insertPointsLedger(sale.memberId(), saleId, PointsEntryType.EARN, earned,
                        balanceAfter, "收银消费积分", "POS_SALE:" + saleId + ":EARN");
            }
            long growth = PosCalculator.pointsEarned(sale.payable().amount());
            if (growth > 0) {
                long growthBalance = repository.addGrowth(sale.memberId(), growth);
                repository.insertGrowthLedger(sale.memberId(), saleId, growth, growthBalance,
                        "收银消费成长值", "POS_SALE:" + saleId + ":GROWTH");
                List<MemberLevel> levels = repository.listMemberLevels();
                MemberLevel best = levels.stream()
                        .filter(candidate -> candidate.minGrowth() <= growthBalance)
                        .max(java.util.Comparator.comparingLong(MemberLevel::minGrowth))
                        .orElse(null);
                if (best != null && !best.code().equals(member.tier())) {
                    repository.updateMemberTier(sale.memberId(), best.code());
                }
            }
            grantBirthdayGift(member, level);
        }
        repository.markSaleCompleted(saleId, earned, now);
        analyticsService.record("pos_sale_completed", saleId, null, Map.of(
                "storeId", sale.storeId().toString(),
                "payable", sale.payable().amount(),
                "currency", sale.currency(),
                "pointsEarned", earned));
        return saleView(requireSale(saleId));
    }

    @Transactional
    public PosSaleView voidSale(UUID saleId) {
        PosSale sale = lockSale(saleId);
        if (sale.status() != PosSaleStatus.DRAFT) {
            throw ApiException.conflict("POS_SALE_NOT_DRAFT", "只有草稿销售单可以作废");
        }
        inventoryService.releasePosSale(saleId);
        for (PosTender tender : repository.findTenders(saleId)) {
            if (tender.status() != TenderStatus.SUCCESS) {
                continue;
            }
            reverseTender(sale, tender, "作废销售单");
            repository.markTenderReversed(tender.id());
        }
        if (sale.couponId() != null) {
            repository.releaseCoupon(sale.couponId());
            repository.rollbackCouponRedemption(saleId, Instant.now());
        }
        if (sale.memberId() != null && sale.pointsRedeemed() > 0) {
            repository.incrementPoints(sale.memberId(), sale.pointsRedeemed());
            long balanceAfter = repository.pointsBalance(sale.memberId());
            repository.insertPointsLedger(sale.memberId(), saleId, PointsEntryType.REFUND,
                    sale.pointsRedeemed(), balanceAfter, "作废销售单退回积分",
                    "POS_SALE:" + saleId + ":VOID_POINTS");
        }
        repository.markSaleVoided(saleId, Instant.now());
        analyticsService.record("pos_sale_voided", saleId, null, Map.of("storeId", sale.storeId().toString()));
        return saleView(requireSale(saleId));
    }

    @Transactional
    public PosSaleView refundSale(UUID saleId, RefundPosSaleRequest request) {
        requireManagerApproval(request.managerUsername(), request.managerPin(), "退款");
        PosSale sale = lockSale(saleId);
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (PosSaleLine line : sale.lines()) {
            int remaining = line.quantity() - line.refundedQuantity();
            if (remaining > 0) {
                quantities.put(line.id(), remaining);
            }
        }
        if (quantities.isEmpty()) {
            throw ApiException.conflict("POS_SALE_ALREADY_REFUNDED", "销售单已全部退款");
        }
        String operator = request.operatorName() == null || request.operatorName().isBlank()
                ? sale.operatorName()
                : request.operatorName().trim();
        return applyRefund(sale, quantities, request.reason().trim(), operator, RefundType.FULL);
    }

    @Transactional
    public PosSaleView refundLines(UUID saleId, RefundPosLinesRequest request) {
        PosSale sale = lockSale(saleId);
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (RefundPosLinesRequest.Item item : request.items()) {
            if (item.quantity() <= 0) {
                throw ApiException.badRequest("POS_REFUND_QUANTITY_INVALID", "退款数量必须大于 0");
            }
            try {
                quantities.merge(item.saleLineId(), item.quantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw ApiException.badRequest("POS_REFUND_QUANTITY_OVERFLOW", "退款数量超出可处理范围");
            }
        }
        if (quantities.isEmpty()) {
            throw ApiException.badRequest("POS_REFUND_EMPTY", "请选择至少一个商品行");
        }
        String operator = request.operatorName() == null || request.operatorName().isBlank()
                ? sale.operatorName()
                : request.operatorName().trim();
        RefundType type = isFullRefund(sale, quantities) ? RefundType.FULL : RefundType.PARTIAL;
        return applyRefund(sale, quantities, request.reason().trim(), operator, type);
    }

    private PosSaleView applyRefund(PosSale sale, Map<UUID, Integer> quantities, String reason,
                                    String operator, RefundType refundType) {
        if (sale.status() != PosSaleStatus.COMPLETED && sale.status() != PosSaleStatus.PARTIALLY_REFUNDED) {
            throw ApiException.conflict("POS_SALE_NOT_REFUNDABLE",
                    "只有已完成或部分退款的销售单可以退款，当前状态为 " + sale.status());
        }
        PosShift saleShift = requireShift(sale.shiftId());
        if (saleShift.status() != PosShiftStatus.OPEN) {
            throw ApiException.conflict("POS_REFUND_SHIFT_CLOSED",
                    "原班次已关闭，请在当前班次发起退款并走门店财务流程");
        }

        long subtotal = sale.subtotal().amount();
        long payable = sale.payable().amount();
        Map<UUID, PosSaleLine> linesById = new LinkedHashMap<>();
        long[] lineTotals = new long[sale.lines().size()];
        for (int index = 0; index < sale.lines().size(); index++) {
            PosSaleLine line = sale.lines().get(index);
            linesById.put(line.id(), line);
            lineTotals[index] = line.lineTotal().amount();
        }
        long[] lineNets = PosCalculator.allocateNet(lineTotals, payable);
        Map<UUID, Long> netByLine = new LinkedHashMap<>();
        for (int index = 0; index < sale.lines().size(); index++) {
            netByLine.put(sale.lines().get(index).id(), lineNets[index]);
        }
        Map<UUID, List<PosSaleAllocation>> allocationsByLine = new LinkedHashMap<>();
        for (PosSaleAllocation allocation : repository.findAllocations(sale.id())) {
            allocationsByLine.computeIfAbsent(allocation.saleLineId(), key -> new ArrayList<>()).add(allocation);
        }

        long additionalRefund = 0;
        boolean fullyRefunded = true;
        List<RefundLineAmount> refundLines = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            PosSaleLine line = linesById.get(entry.getKey());
            if (line == null) {
                throw ApiException.badRequest("POS_REFUND_LINE_NOT_FOUND",
                        "销售单不包含商品行：" + entry.getKey());
            }
            int quantity = entry.getValue();
            int remaining = line.quantity() - line.refundedQuantity();
            if (quantity > remaining) {
                throw ApiException.conflict("POS_REFUND_QUANTITY_EXCEEDED",
                        "商品 " + line.title() + " 最多可退 " + remaining + " 件，本次请求 " + quantity + " 件");
            }
            long lineNet = netByLine.get(line.id());
            int newRefundedQuantity = line.refundedQuantity() + quantity;
            long targetAmount = PosCalculator.prorated(lineNet, newRefundedQuantity, line.quantity());
            long lineRefund = targetAmount - line.refundedAmount().amount();
            if (lineRefund < 0) {
                throw new IllegalStateException("Line " + line.id() + " target refund "
                        + targetAmount + " is below already refunded " + line.refundedAmount().amount());
            }
            additionalRefund = Math.addExact(additionalRefund, lineRefund);
            refundLines.add(new RefundLineAmount(line, quantity, lineRefund));
            if (newRefundedQuantity < line.quantity()) {
                fullyRefunded = false;
            }
        }
        for (PosSaleLine line : sale.lines()) {
            Integer requested = quantities.get(line.id());
            int remaining = line.quantity() - line.refundedQuantity();
            if (remaining > 0 && (requested == null || requested < remaining)) {
                fullyRefunded = false;
            }
        }
        long refundedBefore = sale.refunded().amount();
        if (refundedBefore + additionalRefund > payable) {
            throw new IllegalStateException("Refund " + additionalRefund + " exceeds remaining payable "
                    + (payable - refundedBefore) + " for sale " + sale.id());
        }

        UUID refundId = Ids.newId();
        String refundNo = "RF-" + Ids.orderNo();
        repository.insertRefund(refundId, refundNo, sale.id(), additionalRefund, sale.currency(),
                reason, operator, refundType);
        for (RefundLineAmount refundLine : refundLines) {
            PosSaleLine line = refundLine.line();
            repository.insertRefundLine(refundId, line.id(), refundLine.quantity(),
                    refundLine.amount(), sale.currency());
            UUID refundLineId = repository.findRefundLineId(refundId, line.id());
            if (refundLineId == null) {
                throw new IllegalStateException("Refund line was not persisted for sale line " + line.id());
            }
            int restoreRemaining = refundLine.quantity();
            for (PosSaleAllocation allocation : allocationsByLine.getOrDefault(line.id(), List.of())) {
                int available = allocation.quantity() - allocation.refundedQuantity();
                int restore = Math.min(restoreRemaining, available);
                if (restore <= 0) {
                    continue;
                }
                inventoryService.restorePosStock(allocation.batchId(), line.skuId(), sale.storeId(),
                        restore, sale.id());
                if (!repository.addAllocationRefund(allocation.id(), restore)) {
                    throw ApiException.conflict("POS_REFUND_ALLOCATION_CONFLICT",
                            "退款批次分配已变化，请重试：" + allocation.batchId());
                }
                repository.insertRefundAllocation(refundLineId, allocation.batchId(), restore);
                restoreRemaining -= restore;
            }
            if (restoreRemaining > 0) {
                throw new IllegalStateException("Not enough allocation quantity to restore "
                        + restoreRemaining + " units for sale line " + line.id());
            }
            if (!repository.addLineRefund(line.id(), refundLine.quantity(), refundLine.amount())) {
                throw ApiException.conflict("POS_REFUND_LINE_CONFLICT",
                        "商品行退款进度已变化，请重试：" + line.title());
            }
        }

        long tenderRefunded = refundTenders(sale, refundId, additionalRefund, payable, refundedBefore, reason);
        if (tenderRefunded != additionalRefund) {
            throw new IllegalStateException("Tender refund allocation mismatch: expected " + additionalRefund
                    + ", allocated " + tenderRefunded + " for sale " + sale.id());
        }

        long pointsReversed = reverseEarnedPoints(sale, refundId, additionalRefund, fullyRefunded);
        if (fullyRefunded && sale.memberId() != null && sale.pointsRedeemed() > 0) {
            repository.incrementPoints(sale.memberId(), sale.pointsRedeemed());
            long balanceAfter = repository.pointsBalance(sale.memberId());
            repository.insertPointsLedger(sale.memberId(), sale.id(), PointsEntryType.REFUND,
                    sale.pointsRedeemed(), balanceAfter, "销售退款退回抵扣积分",
                    "POS_REFUND:" + refundId + ":REDEEM");
        }
        if (fullyRefunded && sale.couponId() != null) {
            repository.rollbackCouponRedemption(sale.id(), Instant.now());
            repository.restoreUsedCoupon(sale.couponId());
        }

        if (fullyRefunded) {
            repository.markSaleFullyRefunded(sale.id(), additionalRefund, pointsReversed);
        } else {
            repository.markSalePartiallyRefunded(sale.id(), additionalRefund, pointsReversed);
        }
        analyticsService.record(fullyRefunded ? "pos_sale_refunded" : "pos_sale_partially_refunded",
                sale.id(), null, Map.of(
                        "storeId", sale.storeId().toString(),
                        "amount", additionalRefund,
                        "refundType", refundType.name(),
                        "reason", reason));
        return saleView(requireSale(sale.id()));
    }

    private long refundTenders(PosSale sale, UUID refundId, long refundAmount, long payable,
                               long refundedBefore, String reason) {
        List<PosTender> tenders = repository.findTenders(sale.id()).stream()
                .filter(tender -> tender.status() == TenderStatus.SUCCESS)
                .toList();
        if (refundAmount == 0) {
            return 0;
        }
        long cumulativeTarget = refundedBefore + refundAmount;
        Map<UUID, Long> allocatedByTender = new LinkedHashMap<>();
        long allocated = 0;
        for (int index = 0; index < tenders.size(); index++) {
            PosTender tender = tenders.get(index);
            long already = allocatedByTender.getOrDefault(tender.id(), 0L);
            long max = tender.amount().amount() - tender.refundedAmount().amount() - already;
            long amount;
            if (index == tenders.size() - 1) {
                amount = refundAmount - allocated;
            } else {
                long target = (tender.amount().amount() * cumulativeTarget + payable / 2) / payable;
                amount = target - tender.refundedAmount().amount() - already;
            }
            amount = Math.max(0, Math.min(amount, max));
            if (amount == 0) {
                continue;
            }
            refundTenderAmount(sale, tender, amount, refundId, reason);
            repository.insertRefundTender(refundId, tender.id(), tender.type(), amount,
                    tender.provider(), tender.providerTradeNo());
            repository.addTenderRefund(tender.id(), amount);
            allocatedByTender.put(tender.id(), already + amount);
            allocated += amount;
        }
        if (allocated < refundAmount) {
            for (PosTender tender : tenders) {
                long already = allocatedByTender.getOrDefault(tender.id(), 0L);
                long max = tender.amount().amount() - tender.refundedAmount().amount() - already;
                long extra = Math.min(refundAmount - allocated, Math.max(max, 0));
                if (extra <= 0) {
                    continue;
                }
                refundTenderAmount(sale, tender, extra, refundId, reason);
                repository.insertRefundTender(refundId, tender.id(), tender.type(), extra,
                        tender.provider(), tender.providerTradeNo());
                repository.addTenderRefund(tender.id(), extra);
                allocatedByTender.put(tender.id(), already + extra);
                allocated += extra;
            }
        }
        return allocated;
    }

    private long reverseEarnedPoints(PosSale sale, UUID refundId, long refundAmount, boolean fullyRefunded) {
        if (sale.memberId() == null || sale.pointsEarned() == 0 || sale.payable().amount() == 0) {
            return 0;
        }
        long cumulative = sale.refunded().amount() + refundAmount;
        long target = fullyRefunded
                ? sale.pointsEarned()
                : (sale.pointsEarned() * cumulative + sale.payable().amount() / 2) / sale.payable().amount();
        long reverse = Math.max(target - sale.pointsReversed(), 0);
        if (reverse == 0) {
            return 0;
        }
        long balance = repository.pointsBalance(sale.memberId());
        long deduct = Math.min(balance, reverse);
        if (deduct == 0) {
            return 0;
        }
        repository.decrementPoints(sale.memberId(), deduct);
        long balanceAfter = repository.pointsBalance(sale.memberId());
        repository.insertPointsLedger(sale.memberId(), sale.id(), PointsEntryType.REFUND,
                -deduct, balanceAfter, "销售退款扣回积分",
                "POS_REFUND:" + refundId + ":EARN");
        return deduct;
    }

    private boolean isFullRefund(PosSale sale, Map<UUID, Integer> quantities) {
        boolean hasLine = false;
        for (PosSaleLine line : sale.lines()) {
            int remaining = line.quantity() - line.refundedQuantity();
            if (remaining <= 0) {
                continue;
            }
            hasLine = true;
            Integer requested = quantities.get(line.id());
            if (requested == null || requested != remaining) {
                return false;
            }
        }
        return hasLine;
    }

    private void refundTenderAmount(PosSale sale, PosTender tender, long amount, UUID refundId, String reason) {
        switch (tender.type()) {
            case CASH -> repository.insertCashMovement(sale.shiftId(), sale.id(), CashMovementType.REFUND.name(),
                    -amount, sale.currency(), reason, sale.operatorName());
            case STORED_VALUE -> {
                if (sale.memberId() == null) {
                    throw new IllegalStateException("Stored-value tender without member on sale " + sale.id());
                }
                repository.incrementStoredValue(sale.memberId(), amount);
                long balanceAfter = repository.storedValueBalance(sale.memberId());
                repository.insertCardLedger(sale.memberId(), sale.id(), MemberCardEntryType.REFUND,
                        amount, balanceAfter, sale.currency(), reason,
                        "POS_REFUND:" + refundId + ":STORED:" + tender.id());
            }
            default -> {
                PosPaymentProvider provider = providers.get(tender.provider());
                if (provider == null) {
                    throw ApiException.conflict("POS_PROVIDER_NOT_FOUND",
                            "原收款适配器未安装，无法原路退款：" + tender.provider());
                }
                PosPaymentProvider.PosRefundResult result = provider.refund(
                        new PosPaymentProvider.PosRefundRequest(tender.providerTradeNo(),
                                amount, sale.currency(), reason));
                if (!result.success()) {
                    throw ApiException.conflict("POS_REFUND_FAILED",
                            "支付渠道退款失败：" + result.message() + "（退款单 " + result.refundNo() + "）");
                }
            }
        }
    }

    public PosSaleView getSale(UUID saleId) {
        return saleView(requireSale(saleId));
    }

    public List<PosSaleView> findStoreSales(UUID storeId, LocalDate date) {
        storeService.getStore(storeId);
        return repository.findStoreSales(storeId, date == null ? LocalDate.now() : date, 50)
                .stream().map(this::saleView).toList();
    }

    // ------------------------------------------------------------------ reports

    public PosDailyReport dailyReport(UUID storeId, LocalDate date) {
        Store store = storeService.getStore(storeId);
        LocalDate businessDate = date == null ? LocalDate.now() : date;
        PosRepository.DailyTotals totals = repository.dailyTotals(storeId, businessDate);
        long refundTotal = repository.refundTotal(storeId, businessDate);
        int refundCount = repository.refundCount(storeId, businessDate);
        List<PosTenderTotal> tenderMix = repository.tenderTotals(storeId, businessDate);
        List<PosShiftView> shifts = repository.findShifts(storeId, businessDate).stream()
                .map(this::shiftView)
                .toList();
        String currency = tenderMix.isEmpty() ? DEFAULT_CURRENCY : tenderMix.get(0).amount().currency();
        long cashGross = tenderMix.stream()
                .filter(total -> "CASH".equals(total.tenderType()))
                .mapToLong(total -> total.amount().amount())
                .sum();
        long cashTotal = cashGross - repository.refundTotalByTender(storeId, businessDate, "CASH");
        long variance = repository.cashVariance(storeId, businessDate);
        return new PosDailyReport(
                businessDate,
                store.id(),
                store.name(),
                totals.salesCount(),
                refundCount,
                new Money(totals.gross(), currency),
                new Money(totals.discounts(), currency),
                new Money(totals.net(), currency),
                new Money(refundTotal, currency),
                new Money(cashTotal, currency),
                new Money(totals.pointsDiscount(), currency),
                new Money(totals.couponDiscount(), currency),
                totals.pointsRedeemed(),
                totals.pointsEarned(),
                totals.couponsUsed(),
                new Money(variance, currency),
                shifts.size(),
                tenderMix,
                shifts);
    }

    public PosReconciliationReport reconciliation(UUID storeId, LocalDate date) {
        Store store = storeService.getStore(storeId);
        LocalDate businessDate = date == null ? LocalDate.now() : date;
        List<PosTenderTotal> tenderMix = repository.tenderTotals(storeId, businessDate);
        List<PosReconciliationReport.Row> rows = new ArrayList<>();
        for (PosTenderTotal total : tenderMix) {
            long refunded = repository.refundTotalByTender(storeId, businessDate, total.tenderType());
            long expected = total.amount().amount() - refunded;
            rows.add(new PosReconciliationReport.Row(
                    total.tenderType(),
                    new Money(expected, total.amount().currency()),
                    new Money(expected, total.amount().currency()),
                    new Money(0, total.amount().currency()),
                    "MATCHED"));
        }
        return new PosReconciliationReport(businessDate, store.id(), store.name(), rows);
    }

    // ------------------------------------------------------------------ internals

    private void grantBirthdayGift(PosMember member, MemberLevel level) {
        if (member.birthday() == null || level == null || level.birthdayPoints() <= 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (member.birthday().getMonthValue() != today.getMonthValue()
                || member.birthday().getDayOfMonth() != today.getDayOfMonth()) {
            return;
        }
        if (member.birthdayGiftYear() != null && member.birthdayGiftYear() == today.getYear()) {
            return;
        }
        repository.incrementPoints(member.id(), level.birthdayPoints());
        long balanceAfter = repository.pointsBalance(member.id());
        repository.insertPointsLedger(member.id(), null, PointsEntryType.EARN, level.birthdayPoints(),
                balanceAfter, "生日礼积分", "BIRTHDAY:" + member.id() + ":" + today.getYear());
        repository.updateBirthdayGiftYear(member.id(), today.getYear());
    }

    private void requireManagerApproval(String username, String pin, String action) {
        if (!requireManagerApproval) {
            return;
        }
        StaffUser manager = staffAuthService.verifyPin(username, pin);
        if (manager == null || (!"ADMIN".equals(manager.role())
                && !"OPERATOR".equals(manager.role()) && !"STORE".equals(manager.role()))) {
            throw ApiException.conflict("POS_MANAGER_APPROVAL_REQUIRED",
                    action + "需要店长/运营账号 PIN 授权");
        }
    }

    private void reverseTender(PosSale sale, PosTender tender, String reason) {
        switch (tender.type()) {
            case CASH -> repository.insertCashMovement(sale.shiftId(), sale.id(), CashMovementType.REFUND.name(),
                    -tender.amount().amount(), sale.currency(), reason, sale.operatorName());
            case STORED_VALUE -> {
                if (sale.memberId() == null) {
                    throw new IllegalStateException("Stored-value tender without member on sale " + sale.id());
                }
                repository.incrementStoredValue(sale.memberId(), tender.amount().amount());
                long balanceAfter = repository.storedValueBalance(sale.memberId());
                repository.insertCardLedger(sale.memberId(), sale.id(), MemberCardEntryType.REFUND,
                        tender.amount().amount(), balanceAfter, sale.currency(), reason,
                        "POS_SALE:" + sale.id() + ":VOID_STORED");
            }
            default -> cancelProviderTender(tender, reason);
        }
    }

    private void refundTender(PosSale sale, PosTender tender, String reason) {
        switch (tender.type()) {
            case CASH -> repository.insertCashMovement(sale.shiftId(), sale.id(), CashMovementType.REFUND.name(),
                    -tender.amount().amount(), sale.currency(), reason, sale.operatorName());
            case STORED_VALUE -> {
                if (sale.memberId() == null) {
                    throw new IllegalStateException("Stored-value tender without member on sale " + sale.id());
                }
                repository.incrementStoredValue(sale.memberId(), tender.amount().amount());
                long balanceAfter = repository.storedValueBalance(sale.memberId());
                repository.insertCardLedger(sale.memberId(), sale.id(), MemberCardEntryType.REFUND,
                        tender.amount().amount(), balanceAfter, sale.currency(), reason,
                        "POS_SALE:" + sale.id() + ":REFUND_STORED");
            }
            default -> {
                PosPaymentProvider provider = providers.get(tender.provider());
                if (provider == null) {
                    throw ApiException.conflict("POS_PROVIDER_NOT_FOUND",
                            "原收款适配器未安装，无法原路退款：" + tender.provider());
                }
                PosPaymentProvider.PosRefundResult result = provider.refund(
                        new PosPaymentProvider.PosRefundRequest(tender.providerTradeNo(),
                                tender.amount().amount(), sale.currency(), reason));
                if (!result.success()) {
                    throw ApiException.conflict("POS_REFUND_FAILED",
                            "支付渠道退款失败：" + result.message() + "（退款单 " + result.refundNo() + "）");
                }
            }
        }
    }

    private void cancelProviderTender(PosTender tender, String reason) {
        if (tender.providerTradeNo() == null) {
            throw ApiException.conflict("POS_TENDER_CANCEL_FAILED",
                    "收款流水缺少渠道交易号，无法撤销：" + tender.id());
        }
        PosPaymentProvider provider = providers.get(tender.provider());
        if (provider == null) {
            throw ApiException.conflict("POS_PROVIDER_NOT_FOUND",
                    "原收款适配器未安装，无法撤销：" + tender.provider());
        }
        PosPaymentProvider.PosPaymentResult result = provider.cancel(tender.providerTradeNo(), reason);
        if (!result.success()) {
            throw ApiException.conflict("POS_TENDER_CANCEL_FAILED", "支付渠道撤销失败：" + result.message());
        }
    }

    private PosSaleView saleView(PosSale sale) {
        long paid = repository.successfulTenderTotal(sale.id());
        long remaining = sale.payable().amount() - paid;
        if (remaining < 0) {
            throw new IllegalStateException("Sale " + sale.id() + " has tenders " + paid
                    + " above payable " + sale.payable().amount());
        }
        PosMember member = sale.memberId() == null ? null : repository.findMember(sale.memberId());
        PosCoupon coupon = sale.couponId() == null ? null : repository.findCoupon(sale.couponId());
        Store store = storeService.getStore(sale.storeId());
        return new PosSaleView(
                sale,
                new Money(paid, sale.currency()),
                new Money(remaining, sale.currency()),
                member,
                coupon,
                store.name());
    }

    private PosShiftView shiftView(PosShift shift) {
        String currency = shift.openingFloat().currency();
        long cashSales = repository.cashTotalByType(shift.id(), CashMovementType.SALE.name());
        long cashRefunds = repository.cashTotalByType(shift.id(), CashMovementType.REFUND.name());
        long cashIn = repository.cashTotalByType(shift.id(), CashMovementType.CASH_IN.name());
        long cashOut = repository.cashTotalByType(shift.id(), CashMovementType.CASH_OUT.name());
        Money expected = shift.expectedCash() != null
                ? shift.expectedCash()
                : new Money(repository.cashTotal(shift.id()), currency);
        return new PosShiftView(
                shift,
                new Money(cashSales, currency),
                new Money(cashRefunds, currency),
                new Money(cashIn, currency),
                new Money(cashOut, currency),
                expected,
                repository.countShiftSales(shift.id()));
    }

    private PosSale requireSale(UUID saleId) {
        PosSale sale = repository.findSale(saleId);
        if (sale == null) {
            throw ApiException.notFound("POS_SALE_NOT_FOUND", "销售单不存在：" + saleId);
        }
        return sale;
    }

    private PosSale lockSale(UUID saleId) {
        repository.lockSale(saleId);
        return requireSale(saleId);
    }

    private PosShift requireShift(UUID shiftId) {
        PosShift shift = repository.findShift(shiftId);
        if (shift == null) {
            throw ApiException.notFound("POS_SHIFT_NOT_FOUND", "班次不存在：" + shiftId);
        }
        return shift;
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.length() > 128) {
            throw ApiException.badRequest("INVALID_IDEMPOTENCY_KEY", "Idempotency-Key 不能超过 128 个字符");
        }
        return trimmed;
    }

    private String hash(CreatePosSaleRequest request) {
        List<CreatePosSaleRequest.Item> items = new ArrayList<>(request.items());
        items.sort(Comparator.comparing(item -> item.skuId().toString()));
        StringBuilder canonical = new StringBuilder()
                .append(request.storeId()).append('|')
                .append(request.shiftId()).append('|')
                .append(request.memberId() == null ? "" : request.memberId()).append('|');
        for (CreatePosSaleRequest.Item item : items) {
            canonical.append(item.skuId()).append(':').append(item.quantity()).append(',');
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required but unavailable", exception);
        }
    }

    private record ResolvedItem(Sku sku, String title, int quantity) {
    }

    private record RefundLineAmount(PosSaleLine line, int quantity, long amount) {
    }
}