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

    private static final long FLAT_SHIPPING_FEE = 0L;

    private final OrderRepository orderRepository;
    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final FulfillmentService fulfillmentService;
    private final StoreService storeService;
    private final AnalyticsService analyticsService;
    private final IdempotencyRepository idempotencyRepository;
    private final NotificationService notificationService;
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
                        ReferralService referralService,
                        PromotionService promotionService,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.fulfillmentService = fulfillmentService;
        this.storeService = storeService;
        this.analyticsService = analyticsService;
        this.idempotencyRepository = idempotencyRepository;
        this.notificationService = notificationService;
        this.referralService = referralService;
        this.promotionService = promotionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order checkout(CheckoutRequest request, String idempotencyKey) {
        String idemKey = idempotencyKey == null ? null : idempotencyKey.trim();
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
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            Sku sku = catalogService.getSku(entry.getKey());
            Money unitPrice = promotionPricing.priceOverrides().getOrDefault(sku.id(), sku.price());
            if (currency == null) {
                currency = sku.price().currency();
                promoSubtotal = Money.zero(currency);
            } else if (!currency.equals(sku.price().currency())) {
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

        Money subtotal = Money.zero(currency);
        for (ResolvedItem item : resolved) {
            subtotal = subtotal.plus(item.sku().price().multiply(item.quantity()));
        }
        Money shippingFee = request.fulfillmentMode() == FulfillmentMode.PICKUP
                ? Money.zero(currency)
                : new Money(FLAT_SHIPPING_FEE, currency);
        Money discount = subtotal.minus(promoSubtotal);
        if (discount.amount() < 0) {
            throw new IllegalStateException("Promotion subtotal exceeds catalog subtotal for checkout");
        }
        Money total = subtotal.plus(shippingFee).minus(discount);

        UUID orderId = Ids.newId();
        Order order = new Order(
                orderId,
                Ids.orderNo(),
                OrderStatus.PENDING_PAYMENT,
                currency,
                subtotal,
                shippingFee,
                discount,
                total,
                request.contact().name(),
                request.contact().phone(),
                request.contact().email(),
                writeAddress(request.address()),
                request.fulfillmentMode(),
                request.storeId(),
                Instant.now(),
                List.of());
        orderRepository.insertOrder(order);
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
                    item.lineType());
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
        return true;
    }

    public java.util.Map<String, Object> listOrders(String status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = safePage * safeSize;
        List<OrderSummary> items = orderRepository.findSummaries(status, safeSize, offset);
        long total = orderRepository.countSummaries(status);
        return Map.of(
                "items", items,
                "page", safePage,
                "size", safeSize,
                "total", total);
    }

    @Transactional
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