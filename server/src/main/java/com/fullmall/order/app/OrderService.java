package com.fullmall.order.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.analytics.app.AnalyticsService;
import com.fullmall.catalog.app.CatalogService;
import com.fullmall.distribution.app.ReferralService;
import com.fullmall.catalog.domain.Product;
import com.fullmall.catalog.domain.ProductStatus;
import com.fullmall.catalog.domain.Sku;
import com.fullmall.fulfillment.app.FulfillmentService;
import com.fullmall.fulfillment.app.LineAllocation;
import com.fullmall.fulfillment.domain.FulfillmentCompletedEvent;
import com.fullmall.fulfillment.domain.FulfillmentOrder;
import com.fullmall.fulfillment.domain.FulfillmentStatus;
import com.fullmall.inventory.app.InventoryService;
import com.fullmall.inventory.domain.LocationAvailability;
import com.fullmall.inventory.domain.ReservationAllocation;
import com.fullmall.order.api.CheckoutRequest;
import com.fullmall.order.api.OrderSummary;
import com.fullmall.order.domain.FulfillmentMode;
import com.fullmall.order.domain.IdempotencyRecord;
import com.fullmall.order.domain.Order;
import com.fullmall.order.domain.OrderLine;
import com.fullmall.order.domain.OrderStatus;
import com.fullmall.order.infra.IdempotencyRepository;
import com.fullmall.order.infra.OrderRepository;
import com.fullmall.notification.app.NotificationService;
import com.fullmall.platform.domain.FulfillmentType;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.Money;
import com.fullmall.platform.util.Ids;
import com.fullmall.promotion.app.PromotionPricing;
import com.fullmall.promotion.app.PromotionService;
import com.fullmall.store.app.StoreService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(OrderService.class);

    private final com.fullmall.platform.media.MediaUrlResolver mediaUrls;
    private final MemberDiscountLookup memberDiscounts;
    private final com.fullmall.bargain.app.BargainService bargainService;
    private final com.fullmall.member.app.MemberPointsService memberPoints;
    private final com.fullmall.promotion.app.PromotionPreviewService promotionPreviews;
    private final com.fullmall.promotion.ladder.PromotionLadderService promotionLadders;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.fullmall.coupon.app.CouponService coupons;
    private final com.fullmall.coupon.app.CouponAllocationService couponAllocations;
    private final com.fullmall.promotion.app.CouponStackingPolicy.Mode globalCouponMode;
    private final com.fullmall.member.auth.MemberAuthService memberAuth;
    /** 下单时把市场快照进订单（ADR-0014）；解析不到会明确失败，不默认 CN。 */
    private final com.fullmall.setting.app.MarketContext marketContext;
    /** 按市场取价：覆盖价优先，币种不符直接拒绝（禁止把人民币价当港币卖）。 */
    private final com.fullmall.catalog.app.SkuPricingService skuPricing;
    /** 运费按 市场 × 目的地 × 重量档 计算，不再用写死的 FLAT_SHIPPING_FEE。 */
    private final com.fullmall.shipping.app.ShippingRateService shippingRates;
    /** 计税：目的地优先、市场税务辖区兜底；无规则时告警但不静默假装算过。 */
    private final com.fullmall.tax.app.TaxService taxService;
    /**
     * 下单门禁（市场 × 目的地）：市场是否开通、目的地是否可达、地址格式与运费规则是否齐备。
     * 缺任何一项都在定价/扣库存之前失败，避免"算完价才发现送不到"。
     */
    private final com.fullmall.capability.app.MarketCapabilityService capabilities;
    /** 下单汇率快照：只写“订单币种→基准币种”的事实，不用它改订单金额。 */
    private final com.fullmall.exchange.app.ExchangeRateService exchangeRates;
    private final String fxBaseCurrency;


    private final OrderRepository orderRepository;
    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final FulfillmentService fulfillmentService;
    private final StoreService storeService;
    private final AnalyticsService analyticsService;
    private final IdempotencyRepository idempotencyRepository;
    private final NotificationService notificationService;
    private final com.fullmall.platform.audit.AuditEventService auditEvents;
    private final ReferralService referralService;
    private final PromotionService promotionService;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        CatalogService catalogService,
                        InventoryService inventoryService,
                        FulfillmentService fulfillmentService,
                        StoreService storeService,
                        AnalyticsService analyticsService,
                        IdempotencyRepository idempotencyRepository,
                        NotificationService notificationService,
                        com.fullmall.platform.audit.AuditEventService auditEvents,
                        ReferralService referralService,
                        PromotionService promotionService,
                        ObjectMapper objectMapper,
                        com.fullmall.platform.media.MediaUrlResolver mediaUrls,
                        MemberDiscountLookup memberDiscounts,
                        com.fullmall.bargain.app.BargainService bargainService,
                        com.fullmall.member.app.MemberPointsService memberPoints,
                        com.fullmall.promotion.app.PromotionPreviewService promotionPreviews,
                        com.fullmall.promotion.ladder.PromotionLadderService promotionLadders,
                        org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
                        com.fullmall.coupon.app.CouponService coupons,
                        com.fullmall.coupon.app.CouponAllocationService couponAllocations,
                        @org.springframework.beans.factory.annotation.Value("${app.promotion.coupon.mode:EXCLUSIVE}") String couponMode,
                        com.fullmall.member.auth.MemberAuthService memberAuth,
                        com.fullmall.setting.app.MarketContext marketContext,
                        com.fullmall.catalog.app.SkuPricingService skuPricing,
                        com.fullmall.shipping.app.ShippingRateService shippingRates,
                        com.fullmall.tax.app.TaxService taxService,
                        com.fullmall.capability.app.MarketCapabilityService capabilities,
                        com.fullmall.exchange.app.ExchangeRateService exchangeRates,
                        @org.springframework.beans.factory.annotation.Value("${app.fx.base-currency}")
                        String fxBaseCurrency) {
        this.orderRepository = orderRepository;
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.fulfillmentService = fulfillmentService;
        this.storeService = storeService;
        this.analyticsService = analyticsService;
        this.idempotencyRepository = idempotencyRepository;
        this.notificationService = notificationService;
        this.auditEvents = auditEvents;
        this.referralService = referralService;
        this.promotionService = promotionService;
        this.objectMapper = objectMapper;
        this.mediaUrls = mediaUrls;
        this.memberDiscounts = memberDiscounts;
        this.bargainService = bargainService;
        this.memberPoints = memberPoints;
        this.promotionPreviews = promotionPreviews;
        this.promotionLadders = promotionLadders;
        this.jdbcTemplate = jdbcTemplate;
        this.coupons = coupons;
        this.couponAllocations = couponAllocations;
        this.memberAuth = memberAuth;
        this.marketContext = marketContext;
        this.skuPricing = skuPricing;
        this.shippingRates = shippingRates;
        this.taxService = taxService;
        this.capabilities = capabilities;
        this.exchangeRates = exchangeRates;
        this.fxBaseCurrency = fxBaseCurrency == null ? null
                : fxBaseCurrency.trim().toUpperCase(java.util.Locale.ROOT);
        if (this.fxBaseCurrency == null || this.fxBaseCurrency.isBlank()) {
            // 基准币种是报价/对账口径，缺失时不允许启动（不要默认 CNY）
            throw new IllegalStateException("app.fx.base-currency 未配置");
        }
        this.globalCouponMode = com.fullmall.promotion.app.CouponStackingPolicy.parse(
                couponMode, "app.promotion.coupon.mode");

    }

    @Transactional
    public Order checkout(CheckoutRequest request, String idempotencyKey) {
        String idemKey = idempotencyKey == null ? null : idempotencyKey.trim();
        // 市场在进入业务逻辑前解析：解析不到就失败，避免订单落到错误市场的币种/税/主体上
        String marketCode = marketContext.current();
        // 配送单先过能力门禁：市场停用、目的地未开通、地址格式或运费规则缺失都在这里拦下。
        // 自提不校验目的地——自提不寄，目的地国家与运费规则都无关。
        if (request.fulfillmentMode() == FulfillmentMode.SHIP) {
            capabilities.requireCheckoutCapable(marketCode,
                    request.address() == null ? null : request.address().countryCode());
        }
        if (idemKey != null && idemKey.length() > 128) {
            throw ApiException.badRequest("INVALID_IDEMPOTENCY_KEY", "Idempotency-Key must be at most 128 characters");
        }
        String requestHash = idemKey == null || idemKey.isEmpty() ? null : requestHash(request);
        if (idemKey != null && !idemKey.isEmpty()) {
            idempotencyRepository.lock("checkout", idemKey);
            Optional<IdempotencyRecord> existing = idempotencyRepository.find("checkout", idemKey);
            if (existing.isPresent()) {
                if (!existing.get().requestHash().equals(requestHash)) {
                    throw ApiException.conflict("IDEMPOTENCY_KEY_REUSED",
                            "Idempotency-Key was already used with a different request body");
                }
                return getOrder(existing.get().responseOrderId());
            }
        }

        if (request.fulfillmentMode() == FulfillmentMode.PICKUP) {
            if (request.storeId() == null) {
                throw ApiException.badRequest("STORE_REQUIRED", "storeId is required for pickup");
            }
            storeService.getStore(request.storeId());
        } else if (request.address() == null) {
            throw ApiException.badRequest("ADDRESS_REQUIRED", "Address is required for shipping");
        }

        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (CheckoutRequest.Item item : request.items()) {
            quantities.merge(item.skuId(), item.quantity(), Math::addExact);
        }

        PromotionPricing promotionPricing = promotionService.prepare(
                quantities, request.contact().phone(), request.groupBuyId());

        List<ResolvedItem> resolved = new ArrayList<>();
        String currency = null;
        Money promoSubtotal = null;
        // 优惠基数要用**市场价**（不是基础价）：配了市场覆盖价时两者不同，用基础价会让 discount 变负数
        Money marketSubtotal = null;
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            Sku sku = catalogService.getSku(entry.getKey());
            // 市场价（覆盖价优先，币种必须与市场一致）；促销价再覆盖它
            Money marketPrice = skuPricing.priceFor(sku, marketCode).price();
            Money unitPrice = promotionPricing.priceOverrides().getOrDefault(sku.id(), marketPrice);
            if (request.bargainSessionId() != null) {
                // 砍价价优先：本人 + READY 由 quote 校验，失败直接报错，不回落原价
                var quote = bargainService.quoteForPhone(request.bargainSessionId(),
                        request.contact().phone());
                if (!quote.skuId().equals(sku.id())) {
                    throw ApiException.conflict("BARGAIN_SKU_MISMATCH",
                            "砍价场次对应商品与下单商品不一致");
                }
                unitPrice = new Money(quote.unitPrice(), quote.currency());
            }
            if (currency == null) {
                currency = marketPrice.currency();
                promoSubtotal = Money.zero(currency);
                marketSubtotal = Money.zero(currency);
            } else if (!currency.equals(marketPrice.currency())) {
                throw ApiException.badRequest("CURRENCY_MISMATCH",
                        "All items in one order must use the same currency");
            }
            if (!currency.equals(unitPrice.currency())) {
                throw new IllegalStateException("Promotion price currency mismatch for sku " + sku.id());
            }
            Product product = catalogService.getProduct(sku.productId(), "zh-CN");
            if (product.status() != ProductStatus.ACTIVE) {
                throw ApiException.conflict("PRODUCT_NOT_ACTIVE", "Product is not active: " + product.id());
            }
            PromotionPricing.AppliedPromotion applied = promotionPricing.promotionForSku(sku.id()).orElse(null);
            resolved.add(new ResolvedItem(
                    sku,
                    entry.getValue(),
                    product.title(),
                    unitPrice,
                    applied == null ? null : applied.promotionId(),
                    promotionPricing.priceOverrides().containsKey(sku.id()) ? "PROMOTION" : "NORMAL"));
            promoSubtotal = promoSubtotal.plus(unitPrice.multiply(entry.getValue()));
            marketSubtotal = marketSubtotal.plus(marketPrice.multiply(entry.getValue()));
        }
        if (currency == null || promoSubtotal == null) {
            throw ApiException.badRequest("EMPTY_CART", "Cart must contain at least one item");
        }

        List<ResolvedItem> giftLines = new ArrayList<>();
        for (PromotionPricing.GiftLine gift : promotionPricing.gifts()) {
            Sku giftSku = catalogService.getSku(gift.skuId());
            Product giftProduct = catalogService.getProduct(giftSku.productId(), "zh-CN");
            if (!currency.equals(giftSku.price().currency())) {
                throw ApiException.badRequest("CURRENCY_MISMATCH", "Gift currency does not match order currency");
            }
            giftLines.add(new ResolvedItem(
                    giftSku,
                    gift.quantity(),
                    "赠品：" + giftProduct.title(),
                    Money.zero(currency),
                    gift.promotionId(),
                    "GIFT"));
        }
        List<ResolvedItem> orderItems = new ArrayList<>(resolved);
        orderItems.addAll(giftLines);

        // 商品小计 = 各 SKU 的市场价 × 数量（基础价只在没有覆盖价时才等于它，见 SkuPricingService）
        Money subtotal = marketSubtotal == null ? Money.zero(currency) : marketSubtotal;
        // 运费按 市场 × 收货国 × 重量档 计算；跨国无规则会明确报错，不默认免运费
        int totalWeightGrams = shippingRates.totalWeightGrams(quantities);
        Money shippingFee = request.fulfillmentMode() == FulfillmentMode.PICKUP
                ? Money.zero(currency)
                : shippingRates.fee(marketCode,
                        request.address() == null ? null : request.address().countryCode(),
                        totalWeightGrams, subtotal).amount();
        Money discount = subtotal.minus(promoSubtotal);
        if (discount.amount() < 0) {
            throw new IllegalStateException("Promotion subtotal exceeds catalog subtotal for checkout");
        }
        // 满减/满折（PRO-001）：与秒杀/买赠**互斥**——已有促销优惠时不再叠加，避免重复优惠
        if (discount.amount() == 0) {
            java.util.Map<UUID, Long> subtotalsBySku = new java.util.LinkedHashMap<>();
            for (ResolvedItem item : resolved) {
                subtotalsBySku.merge(item.sku().id(),
                        item.unitPrice().amount() * item.quantity(), Long::sum);
            }
            var engineResult = com.fullmall.promotion.app.PromotionEngine.chooseScoped(
                    promotionPreviews.scopedRules("ONLINE", subtotalsBySku));
            if (engineResult.applied()) {
                discount = new Money(engineResult.discountAmount(), currency);
            }
        }

        // 阶梯促销（C9）：默认关闭；开启时仅在没有活动型优惠时生效（口径见 PromotionLadderService）
        if (promotionLadders != null) {
            java.util.List<Long> unitPrices = new java.util.ArrayList<>();
            for (ResolvedItem item : resolved) {
                for (int i = 0; i < item.quantity(); i++) {
                    unitPrices.add(item.sku().price().amount());
                }
            }
            var ladder = promotionLadders.evaluate(subtotal.amount(), unitPrices, discount.amount() > 0);
            if (ladder.discountAmount() > 0) {
                discount = discount.plus(new Money(ladder.discountAmount(), currency));
            }
        }

        // 会员等级价（MEM-002）：促销后金额再打折，运费不参与；匿名不享受
        Integer tierRate = memberDiscounts.discountRate(request.contact().phone());
        MemberPricing.Discount memberPricing = MemberPricing.apply(
                subtotal.amount(), discount.amount(), shippingFee.amount(), tierRate);
        Money memberDiscount = new Money(memberPricing.memberDiscountAmount(), currency);
        Money total = new Money(memberPricing.payableAmount(), currency);
        discount = discount.plus(memberDiscount);

        // 税（ADR-0014）：目的地优先、市场辖区兜底；价外税把税额加到总额，价内税只做税基拆分
        Money netMerchandise = subtotal.minus(discount);
        var taxLine = taxService.calculate(marketCode,
                request.address() == null ? null : request.address().countryCode(), netMerchandise);
        if (taxLine.isPresent() && !taxLine.get().priceIncludesTax()) {
            total = total.plus(new Money(taxLine.get().taxAmount(), currency));
        }


        CheckoutRequest.CheckoutContext ctx = request.context() == null
                ? CheckoutRequest.CheckoutContext.none() : request.context();
        requireVerifiedCustomer(request, ctx);

        UUID orderId = Ids.newId();
        java.util.Map<UUID, Long> lineAmounts = new java.util.LinkedHashMap<>();
        UUID appliedCouponId = null;
        long appliedCouponDiscount = 0;

        // 优惠券（默认 EXCLUSIVE：与促销互斥，取优惠更大者；STACKABLE 则叠加）
        if (ctx.couponId() != null) {
            UUID couponMemberId = request.contact() == null ? null
                    : orderRepository.findMemberIdByPhone(request.contact().phone());
            if (couponMemberId == null) {
                throw ApiException.badRequest("COUPON_REQUIRES_MEMBER", "优惠券只对会员订单可用");
            }
            // 券的门槛按"命中范围的金额"判断，而不是整单金额
            java.util.Map<UUID, Long> couponSubtotals = new java.util.LinkedHashMap<>();
            for (ResolvedItem item : resolved) {
                couponSubtotals.merge(item.sku().id(),
                        item.sku().price().amount() * item.quantity(), Long::sum);
            }
            long eligibleAmount = coupons.eligibleAmount(ctx.couponId(), couponSubtotals);
            if (eligibleAmount <= 0) {
                throw ApiException.conflict("COUPON_NOT_APPLICABLE",
                        "购物车中没有该券可用的商品");
            }
            var eligibility = coupons.checkUsable(ctx.couponId(), couponMemberId, eligibleAmount);
            if (!eligibility.usable()) {
                throw ApiException.conflict("COUPON_NOT_USABLE", eligibility.reason());
            }
            // C2：单券可覆盖全局口径（NULL 跟随全局）
            var effectiveMode = com.fullmall.promotion.app.CouponStackingPolicy.resolve(
                    globalCouponMode, coupons.stackableOrNull(ctx.couponId()));
            if (effectiveMode == com.fullmall.promotion.app.CouponStackingPolicy.Mode.EXCLUSIVE) {
                if (eligibility.discountAmount() <= discount.amount()) {
                    throw ApiException.conflict("COUPON_NOT_BETTER_THAN_PROMOTION",
                            "该券优惠小于当前促销优惠，请移除券或改用它用");
                }
                total = new Money(total.amount() + discount.amount() - eligibility.discountAmount(), currency);
                discount = new Money(eligibility.discountAmount(), currency);
            } else {
                total = new Money(Math.max(total.amount() - eligibility.discountAmount(), 0), currency);
                discount = discount.plus(new Money(eligibility.discountAmount(), currency));
            }
            appliedCouponId = ctx.couponId();
            appliedCouponDiscount = eligibility.discountAmount();
            coupons.consume(ctx.couponId(), couponMemberId, orderId);
        }

        // 积分抵扣（MEM-003）：1 积分 = 1 分，最多抵到应付为 0
        if (ctx.pointsToUse() != null && ctx.pointsToUse() > 0) {
            var redemption = memberPoints.redeem(request.contact().phone(), orderId,
                    ctx.pointsToUse(), total.amount());
            if (redemption.pointsUsed() > 0) {
                total = new Money(total.amount() - redemption.amount(), currency);
                discount = discount.plus(new Money(redemption.amount(), currency));
            }
        }
        OrderFx fx = fxSnapshot(orderId, total, currency, Instant.now());
        Order order = new Order(
                orderId,
                Ids.orderNo(),
                OrderStatus.PENDING_PAYMENT,
                currency,
                marketCode,
                subtotal,
                shippingFee,
                totalWeightGrams,
                discount,
                total,
                request.contact().name(),
                request.contact().phone(),
                request.contact().email(),
                writeAddress(request.address()),
                request.fulfillmentMode(),
                request.storeId(),
                Instant.now(),
                List.of(),
                fx.rate(),
                fx.baseCurrency(),
                fx.baseTotalAmount(),
                fx.rateAt());
        orderRepository.insertOrder(order);
        // 税行与订单同事务：税额/税率一起快照，事后调整税率不会改写历史订单
        taxLine.ifPresent(line -> orderRepository.insertTaxLine(order.id(), line.jurisdiction(),
                line.name(), line.rateBps(), line.priceIncludesTax(),
                line.taxableAmount(), line.taxAmount()));
        // 记录下单来源与操作员（员工代下单必须可追溯）
        orderRepository.updatePlacement(orderId,
                ctx.placedBy() == null ? "CUSTOMER" : ctx.placedBy().trim().toUpperCase(),
                ctx.placedByOperator());
        // 绑定会员（手机号唯一匹配时），后续"我的订单"按 member_id 查
        orderRepository.attachMemberByPhone(orderId, request.contact().phone());
        // 未绑定会员的订单下发访问令牌：仅凭订单号不能再看到地址/电话/自提码
        if (orderRepository.memberIdOf(orderId) == null) {
            issueGuestToken(orderId);
        }
        // 通知走 notifyOrderEvent：会员订单落 MEMBER，未绑定会员（含门店代下单）落 CUSTOMER/手机号。
        // 此前只通知会员，导致"未绑定会员的订单下单后零通知"。
        notificationService.notifyOrderEvent(orderId, "ORDER_CREATED", "订单已创建",
                "订单 " + order.orderNo() + " 已创建，请尽快完成支付。", List.of("IN_APP", "SMS"));
        promotionService.recordCheckout(promotionPricing, orderId, request.contact().phone());
        referralService.attachCheckout(orderId, request.contact().phone(), request.referrerId(), request.referralMode());

        Map<FulfillmentKey, List<LineAllocation>> groups = new LinkedHashMap<>();
        for (ResolvedItem item : orderItems) {
            Sku sku = item.sku();
            List<LocationAvailability> locations = inventoryService.findAvailableLocations(sku.id(), sku.shelfLifePolicy());
            LocationAvailability location = FulfillmentPlanner.choose(
                    locations, item.quantity(), request.fulfillmentMode(), request.storeId());
            FulfillmentType fulfillmentType = toType(location.locationType(), request.fulfillmentMode());

            UUID orderLineId = Ids.newId();
            OrderLine line = new OrderLine(
                    orderLineId,
                    sku.id(),
                    item.productTitle(),
                    sku.skuCode(),
                    item.quantity(),
                    item.unitPrice(),
                    fulfillmentType,
                    item.promotionId(),
                    item.lineType(),
                    com.fullmall.order.domain.OrderLineSnapshot.toJson(
                            mediaUrls.absolute(sku.imageUrl()),
                            OrderLine.attributesText(sku.options()),
                            sku.compareAtPrice() == null ? null : sku.compareAtPrice().amount()));
            orderRepository.insertOrderLine(orderId, line);

            List<ReservationAllocation> allocations = inventoryService.reserveForOrderLine(
                    orderId,
                    orderLineId,
                    sku.id(),
                    item.quantity(),
                    location.locationType(),
                    location.locationId(),
                    sku.shelfLifePolicy());
            groups.computeIfAbsent(new FulfillmentKey(fulfillmentType, location.locationId()), key -> new ArrayList<>())
                    .add(new LineAllocation(orderLineId, sku.id(), item.quantity(), allocations));
            lineAmounts.put(orderLineId, item.unitPrice().amount() * item.quantity());
        }

        // 券额按行金额比例分摊（C4）：部分退货时才能算出"这一行用了多少券"
        if (appliedCouponId != null && appliedCouponDiscount > 0) {
            couponAllocations.apply(orderId, appliedCouponId, lineAmounts, appliedCouponDiscount);
        }

        for (Map.Entry<FulfillmentKey, List<LineAllocation>> entry : groups.entrySet()) {
            fulfillmentService.createFulfillment(
                    orderId,
                    entry.getKey().type(),
                    entry.getKey().sourceId(),
                    entry.getValue(),
                    request.contact().phone());
        }

        analyticsService.record("order_created", orderId, null, Map.of(
                "total", total.amount(),
                "currency", currency,
                "fulfillmentMode", request.fulfillmentMode().name(),
                "itemCount", orderItems.size(),
                "promotionDiscount", discount.amount(),
                "groupBuy", promotionPricing.groupBuyId() != null));

        if (idemKey != null && !idemKey.isEmpty()) {
            idempotencyRepository.insert("checkout", idemKey, requestHash, orderId);
        }

        return getOrder(orderId);
    }

    /**
     * 下单前校验"真人"：手机号要么已绑定会员，要么必须携带有效验证码（验证后自动建会员）。
     *
     * <p>背景：中国大陆线上交易必须可追溯到自然人；不允许"填个手机号就下单"的匿名订单。
     * 访客下单仍然允许——但必须先通过 OTP 校验，由 `verifyOtp` 自动创建会员。
     */
    private void requireVerifiedCustomer(CheckoutRequest request,
                                        CheckoutRequest.CheckoutContext ctx) {
        if (request.contact() == null || request.contact().phone() == null) {
            return; // 由既有参数校验负责报错
        }
        if (isStaffPlaced(ctx)) {
            // 员工代下单（门店面对面/预定）：无法给顾客发验证码，改为**记录操作员**保证可追溯
            if (ctx.placedByOperator() == null || ctx.placedByOperator().isBlank()) {
                throw ApiException.badRequest("OPERATOR_REQUIRED", "员工代下单必须记录操作员");
            }
            return;
        }
        String phone = request.contact().phone().trim();
        if (orderRepository.findMemberIdByPhone(phone) != null) {
            return; // 已是会员手机号，可直接下单（登录态另行校验）
        }
        if (ctx.otpCode() == null || ctx.otpCode().isBlank()) {
            throw ApiException.badRequest("CHECKOUT_OTP_REQUIRED",
                    "该手机号尚未注册，请先获取并填写短信验证码");
        }
        // 校验通过会自动创建会员，下单后由 attachMemberByPhone 绑定
        memberAuth.verifyOtp(phone, ctx.otpCode());
    }

    private boolean isStaffPlaced(CheckoutRequest.CheckoutContext ctx) {
        return ctx.placedBy() != null
                && ("CASHIER".equalsIgnoreCase(ctx.placedBy())
                    || "ADMIN".equalsIgnoreCase(ctx.placedBy()));
    }

    /** 订单归属会员（游客订单返回 null）。 */
    /** 已下发但尚未消费的游客令牌（请求线程内传递，响应头回传给客户端一次）。 */
    private final ThreadLocal<String> issuedGuestToken = new ThreadLocal<>();

    private void issueGuestToken(UUID orderId) {
        String token = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        orderRepository.storeAccessTokenHash(orderId,
                com.fullmall.platform.identity.OrderOwnershipGuard.hashToken(token));
        issuedGuestToken.set(token);
    }

    /** 取出并清除本次请求下发的游客令牌（控制器写入响应头）。 */
    public String takeIssuedGuestToken() {
        String token = issuedGuestToken.get();
        issuedGuestToken.remove();
        return token;
    }

    public UUID memberIdOf(UUID orderId) {
        return orderRepository.memberIdOf(orderId);
    }

    public Order getOrder(UUID orderId) {
        Order order = orderRepository.findOrder(orderId);
        if (order == null) {
            throw ApiException.notFound("ORDER_NOT_FOUND", "Order not found: " + orderId);
        }
        return order;
    }

    public void markAfterSaleRefunded(UUID orderId, boolean fullyRefunded) {
        orderRepository.updateStatus(orderId, fullyRefunded
                ? OrderStatus.REFUNDED
                : OrderStatus.PARTIALLY_REFUNDED);
    }

    public List<UUID> findStalePendingOrderIds(Instant cutoff, int limit) {
        return orderRepository.findStalePendingOrderIds(cutoff, limit);
    }

    @Transactional
    public boolean cancelUnpaidOrder(UUID orderId) {
        if (!orderRepository.cancelIfPending(orderId)) {
            return false;
        }
        inventoryService.releaseOrder(orderId);
        promotionService.releaseOrder(orderId);
        notificationService.notifyOrderEvent(orderId, "ORDER_CANCELLED", "订单已取消",
                "订单因超时未支付已取消，库存与优惠已释放。", List.of("IN_APP", "SMS"));
        auditEvents.record(new com.fullmall.platform.audit.AuditEventService.AuditEvent(
                "ORDER_CANCELLED_BY_SYSTEM", "system:expiry", "order", orderId.toString(),
                "超时未支付自动取消", null));
        return true;
    }

    public java.util.Map<String, Object> listOrders(String status, int page, int size) {
        return listOrders(status, page, size, "createdAt", "desc");
    }

    public java.util.Map<String, Object> listOrders(String status, int page, int size,
                                                    String sortKey, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = safePage * safeSize;
        List<OrderSummary> items = orderRepository.findSummaries(status, safeSize, offset,
                sortKey, direction);
        long total = orderRepository.countSummaries(status);
        return Map.of(
                "items", items,
                "page", safePage,
                "size", safeSize,
                "total", total);
    }

    /**
     * 订单 keyset 分页：返回本页与下一页游标（`nextCreatedAt`/`nextId`）。
     *
     * <p>与 offset 版本并存：前端未切换前不受影响；深页场景请用本方法。
     */
    public java.util.Map<String, Object> listOrdersKeyset(String status, String cursorCreatedAt,
                                                          UUID cursorId, int size) {
        var cursor = com.fullmall.platform.paging.KeysetPage.Cursor.parse(cursorCreatedAt, cursorId);
        int safeSize = com.fullmall.platform.paging.KeysetPage.normalizeSize(size);
        List<OrderSummary> items = orderRepository.findSummariesKeyset(
                status, cursor.createdAt(), cursor.id(), safeSize);
        return com.fullmall.platform.paging.KeysetPage.toResponse(
                items, safeSize, OrderSummary::createdAt, OrderSummary::id);
    }

    @Transactional
    public boolean markPaidByOrderNo(String orderNo, long paidAmountFen) {
        if (orderNo == null || orderNo.isBlank()) {
            throw ApiException.badRequest("ORDER_NO_REQUIRED", "订单号不能为空");
        }
        var rows = jdbcTemplate.query("SELECT id, status, total_amount FROM orders WHERE order_no = ?",
                rs -> rs.next()
                        ? java.util.Map.<String, Object>of("id", rs.getObject("id", UUID.class),
                        "status", rs.getString("status"),
                        "total", rs.getLong("total_amount"))
                        : null, orderNo);
        if (rows == null) {
            throw ApiException.notFound("ORDER_NOT_FOUND", "订单不存在：" + orderNo);
        }
        if (paidAmountFen != ((Number) rows.get("total")).longValue()) {
            throw ApiException.conflict("PAYMENT_AMOUNT_MISMATCH",
                    "回调金额 " + paidAmountFen + " 与订单应付 " + rows.get("total") + " 不一致");
        }
        if ("PAID".equals(rows.get("status"))) {
            return false;   // 幂等：重复通知不重复入账
        }
        markPaid((UUID) rows.get("id"));
        return true;
    }

    public void markPaid(UUID orderId) {
        Order order = getOrder(orderId);
        if (order.status() == OrderStatus.PAID) {
            return;
        }
        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "Order is not payable in status " + order.status());
        }
        if (!orderRepository.markPaidIfPending(orderId)) {
            Order current = getOrder(orderId);
            if (current.status() == OrderStatus.PAID) {
                return;
            }
            throw ApiException.conflict("ORDER_NOT_PAYABLE",
                    "订单已取消或状态已变化，不能继续支付：" + current.status());
        }
        promotionService.orderPaid(orderId);
        inventoryService.confirmOrderReservations(orderId);
        referralService.settleOrder(orderId, order.total().amount(), order.currency());
        notificationService.notifyOrderEvent(orderId, "ORDER_PAID", "支付成功",
                "您的订单已支付，我们会尽快为您备货。", List.of("IN_APP", "SMS"));
    }

    @EventListener
    @Transactional
    public void onFulfillmentCompleted(FulfillmentCompletedEvent event) {
        List<FulfillmentOrder> fulfillments = fulfillmentService.findFulfillments(event.orderId());
        if (fulfillments.isEmpty()) {
            throw new IllegalStateException("FulfillmentCompletedEvent without fulfillments: " + event.orderId());
        }
        boolean allComplete = fulfillments.stream()
                .allMatch(fulfillment -> fulfillment.status() == FulfillmentStatus.COMPLETED
                        || fulfillment.status() == FulfillmentStatus.SHIPPED);
        boolean anyComplete = fulfillments.stream()
                .anyMatch(fulfillment -> fulfillment.status() == FulfillmentStatus.COMPLETED
                        || fulfillment.status() == FulfillmentStatus.SHIPPED);
        if (!anyComplete) {
            throw new IllegalStateException("FulfillmentCompletedEvent without a completed fulfillment: " + event.orderId());
        }
        orderRepository.updateStatus(event.orderId(), allComplete ? OrderStatus.FULFILLED : OrderStatus.PARTIALLY_FULFILLED);
        if (allComplete) {
            notificationService.notifyOrderEvent(event.orderId(), "ORDER_FULFILLED", "订单已完成",
                    "订单已全部发货/履约完成，感谢您的购买。", List.of("IN_APP", "SMS"));
        } else {
            notificationService.notifyOrderEvent(event.orderId(), "ORDER_PARTIALLY_FULFILLED",
                    "订单部分发货", "订单已部分发货，剩余商品会尽快安排。", List.of("IN_APP"));
        }
    }

    private FulfillmentType toType(com.fullmall.inventory.domain.LocationType locationType, FulfillmentMode mode) {
        if (mode == FulfillmentMode.PICKUP) {
            return FulfillmentType.STORE_PICKUP;
        }
        return switch (locationType) {
            case WAREHOUSE -> FulfillmentType.WAREHOUSE_SHIP;
            case STORE -> FulfillmentType.STORE_SHIP;
            case SUPPLIER -> FulfillmentType.SUPPLIER_DROPSHIP;
        };
    }

    private String writeAddress(CheckoutRequest.Address address) {
        if (address == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(address);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize order address", exception);
        }
    }

    private String requestHash(CheckoutRequest request) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize checkout request for idempotency", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    /**
     * 下单汇率快照：只用于报表/对账/客服解释，订单金额与支付金额仍是订单币种。
     *
     * <p>没有汇率时**不阻塞下单**，但必须留下事实：写审计并在订单详情里显示“未快照”，
     * 避免以后把“没有汇率”误当成“汇率为 1”。
     */
    private OrderFx fxSnapshot(UUID orderId, Money total, String currency, Instant at) {
        if (currency.equals(fxBaseCurrency)) {
            return new OrderFx(java.math.BigDecimal.ONE, fxBaseCurrency, total.amount(), at);
        }
        try {
            var conversion = exchangeRates.convert(total.amount(), currency, fxBaseCurrency, at);
            return new OrderFx(conversion.rate(), fxBaseCurrency, conversion.convertedAmount(),
                    conversion.effectiveAt());
        } catch (com.fullmall.platform.error.ApiException failure) {
            if (!"EXCHANGE_RATE_NOT_FOUND".equals(failure.code())) {
                throw failure;
            }
            log.warn("订单 {} 缺少汇率快照：{} -> {}，金额 {}（不阻塞下单）",
                    orderId, currency, fxBaseCurrency, total.amount());
            auditEvents.record(new com.fullmall.platform.audit.AuditEventService.AuditEvent(
                    "ORDER_FX_RATE_MISSING", "system", "order", orderId.toString(),
                    "currency=" + currency + " base=" + fxBaseCurrency
                            + " amount=" + total.amount(), null));
            return new OrderFx(null, fxBaseCurrency, null, null);
        }
    }

    private record OrderFx(java.math.BigDecimal rate, String baseCurrency,
                           Long baseTotalAmount, Instant rateAt) {
    }

    private record ResolvedItem(
            Sku sku,
            int quantity,
            String productTitle,
            Money unitPrice,
            UUID promotionId,
            String lineType
    ) {
    }

    private record FulfillmentKey(FulfillmentType type, UUID sourceId) {
    }
}