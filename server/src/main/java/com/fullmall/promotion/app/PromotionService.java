package com.fullmall.promotion.app;

import com.fullmall.catalog.app.CatalogService;
import com.fullmall.catalog.domain.Product;
import com.fullmall.catalog.domain.Sku;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.Money;
import com.fullmall.platform.util.Ids;
import com.fullmall.promotion.api.PromotionDtos.CreateGroupBuyRequest;
import com.fullmall.promotion.api.PromotionDtos.CreatePromotionRequest;
import com.fullmall.promotion.api.PromotionDtos.GroupBuyMemberView;
import com.fullmall.promotion.api.PromotionDtos.GroupBuyView;
import com.fullmall.promotion.api.PromotionDtos.PromotionView;
import com.fullmall.promotion.domain.GroupBuy;
import com.fullmall.promotion.domain.GroupBuyFailedEvent;
import com.fullmall.promotion.domain.GroupBuyMember;
import com.fullmall.promotion.domain.PromotionClaim;
import com.fullmall.promotion.domain.Promotion;
import com.fullmall.promotion.domain.PromotionStatus;
import com.fullmall.promotion.domain.PromotionType;
import com.fullmall.promotion.infra.PromotionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PromotionService {

    private static final Logger log = LoggerFactory.getLogger(PromotionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final PromotionRepository repository;
    private final CatalogService catalogService;
    private final ApplicationEventPublisher eventPublisher;

    public PromotionService(PromotionRepository repository, CatalogService catalogService,
                            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.catalogService = catalogService;
        this.eventPublisher = eventPublisher;
    }

    public List<PromotionView> listActive() {
        Instant now = Instant.now();
        return repository.findActive(now).stream().map(promotion -> view(promotion, now)).toList();
    }

    public List<PromotionView> listAll() {
        Instant now = Instant.now();
        return repository.findAll().stream().map(promotion -> view(promotion, now)).toList();
    }

    @Transactional
    public PromotionView create(CreatePromotionRequest request) {
        Instant startsAt = request.startsAt();
        Instant endsAt = request.endsAt();
        if (!endsAt.isAfter(startsAt)) {
            throw ApiException.badRequest("PROMOTION_WINDOW_INVALID", "促销结束时间必须晚于开始时间");
        }
        Sku sku = catalogService.getSku(request.skuId());
        validateRequest(request, sku);
        if (request.giftSkuId() != null) {
            catalogService.getSku(request.giftSkuId());
        }
        String code = request.code().trim().toUpperCase();
        if (repository.findActive(Instant.now()).stream().anyMatch(promotion -> promotion.code().equals(code))) {
            throw ApiException.conflict("PROMOTION_CODE_EXISTS", "促销编码已存在：" + code);
        }
        Promotion promotion = new Promotion(
                Ids.newId(),
                code,
                request.type(),
                request.title().trim(),
                request.description(),
                PromotionStatus.ACTIVE,
                startsAt,
                endsAt,
                request.skuId(),
                request.promoPriceAmount(),
                request.giftSkuId(),
                request.buyQuantity(),
                request.giftQuantity(),
                request.maxGiftPerOrder(),
                request.stockLimit(),
                0,
                request.perUserLimit(),
                request.groupSize(),
                request.groupDurationHours(),
                request.priority() == null ? 0 : request.priority(),
                Instant.now());
        try {
            repository.insert(promotion);
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            throw ApiException.conflict("PROMOTION_CODE_EXISTS", "促销编码已存在：" + code);
        }
        return view(promotion, Instant.now());
    }

    @Transactional
    public PromotionView updateStatus(UUID promotionId, PromotionStatus status) {
        Promotion promotion = repository.find(promotionId)
                .orElseThrow(() -> ApiException.notFound("PROMOTION_NOT_FOUND", "促销不存在：" + promotionId));
        repository.updateStatus(promotionId, status);
        return view(new Promotion(promotion.id(), promotion.code(), promotion.type(), promotion.title(),
                promotion.description(), status, promotion.startsAt(), promotion.endsAt(), promotion.skuId(),
                promotion.promoPriceAmount(), promotion.giftSkuId(), promotion.buyQuantity(),
                promotion.giftQuantity(), promotion.maxGiftPerOrder(), promotion.stockLimit(),
                promotion.soldQuantity(), promotion.perUserLimit(), promotion.groupSize(),
                promotion.groupDurationHours(), promotion.priority(), promotion.createdAt()), Instant.now());
    }

    public PromotionPricing prepare(Map<UUID, Integer> items, String phone, UUID groupBuyId) {
        if (phone == null || phone.isBlank()) {
            throw ApiException.badRequest("CONTACT_REQUIRED", "下单必须提供联系人手机号才能校验促销限购");
        }
        String normalizedPhone = phone.trim();
        Instant now = Instant.now();
        if (groupBuyId != null) {
            return prepareGroupBuy(items, normalizedPhone, groupBuyId, now);
        }

        Map<UUID, Money> priceOverrides = new LinkedHashMap<>();
        List<PromotionPricing.GiftLine> gifts = new ArrayList<>();
        List<PromotionPricing.AppliedPromotion> applied = new ArrayList<>();
        List<Promotion> active = repository.findActive(now);

        for (Promotion promotion : active) {
            if (promotion.type() != PromotionType.FLASH_SALE) {
                continue;
            }
            Integer quantity = items.get(promotion.skuId());
            if (quantity == null || priceOverrides.containsKey(promotion.skuId())) {
                continue;
            }
            if (promotion.isSoldOut()) {
                throw ApiException.conflict("PROMOTION_SOLD_OUT",
                        "秒杀活动「" + promotion.title() + "」已售罄，请刷新后重试");
            }
            if (promotion.perUserLimit() != null
                    && repository.countUserRedemptions(promotion.id(), normalizedPhone) + quantity
                    > promotion.perUserLimit()) {
                throw ApiException.conflict("PROMOTION_USER_LIMIT",
                        "秒杀活动「" + promotion.title() + "」每单/每人限购 " + promotion.perUserLimit() + " 件");
            }
            Sku sku = catalogService.getSku(promotion.skuId());
            priceOverrides.put(promotion.skuId(), new Money(promotion.promoPriceAmount(), sku.price().currency()));
            applied.add(new PromotionPricing.AppliedPromotion(
                    promotion.id(), promotion.code(), promotion.title(), promotion.type(),
                    promotion.skuId(), quantity, 0));
        }

        for (Promotion promotion : active) {
            if (promotion.type() != PromotionType.BUY_GIFT) {
                continue;
            }
            Integer quantity = items.get(promotion.skuId());
            if (quantity == null || quantity < promotion.buyQuantity()) {
                continue;
            }
            int giftCount = (quantity / promotion.buyQuantity()) * promotion.giftQuantity();
            if (promotion.maxGiftPerOrder() != null) {
                giftCount = Math.min(giftCount, promotion.maxGiftPerOrder());
            }
            if (promotion.stockLimit() != null && promotion.soldQuantity() + giftCount > promotion.stockLimit()) {
                throw ApiException.conflict("PROMOTION_GIFT_SOLD_OUT",
                        "买赠活动「" + promotion.title() + "」赠品名额不足，请减少购买数量");
            }
            gifts.add(new PromotionPricing.GiftLine(promotion.id(), promotion.giftSkuId(), giftCount));
            applied.add(new PromotionPricing.AppliedPromotion(
                    promotion.id(), promotion.code(), promotion.title(), promotion.type(),
                    promotion.skuId(), quantity, giftCount));
        }

        return new PromotionPricing(priceOverrides, gifts, applied, null);
    }

    @Transactional
    public void recordCheckout(PromotionPricing pricing, UUID orderId, String phone) {
        String normalizedPhone = phone == null ? null : phone.trim();
        if (pricing.groupBuyId() != null) {
            recordGroupMember(pricing.groupBuyId(), orderId, normalizedPhone);
            return;
        }
        for (PromotionPricing.AppliedPromotion promotion : pricing.applied()) {
            repository.lockUser(promotion.promotionId(), normalizedPhone);
            if (promotion.type() == PromotionType.FLASH_SALE) {
                Promotion entity = repository.find(promotion.promotionId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Promotion missing during checkout: " + promotion.promotionId()));
                if (entity.perUserLimit() != null
                        && repository.countUserRedemptions(promotion.promotionId(), normalizedPhone)
                        + promotion.quantity() > entity.perUserLimit()) {
                    throw ApiException.conflict("PROMOTION_USER_LIMIT",
                            "秒杀活动「" + promotion.title() + "」限购数量已用完");
                }
                Integer limit = stockLimit(promotion.promotionId());
                if (limit == null) {
                    repository.claimUnlimited(promotion.promotionId(), promotion.quantity());
                } else {
                    repository.claimStock(promotion.promotionId(), promotion.quantity(), limit);
                }
                repository.insertRedemption(promotion.promotionId(), orderId, normalizedPhone,
                        promotion.quantity(), 0);
            } else if (promotion.type() == PromotionType.BUY_GIFT) {
                Integer limit = stockLimit(promotion.promotionId());
                if (limit == null) {
                    repository.claimUnlimited(promotion.promotionId(), promotion.giftQuantity());
                } else {
                    repository.claimStock(promotion.promotionId(), promotion.giftQuantity(), limit);
                }
                repository.insertRedemption(promotion.promotionId(), orderId, normalizedPhone,
                        promotion.quantity(), promotion.giftQuantity());
            }
        }
    }

    @Transactional
    public void orderPaid(UUID orderId) {
        repository.findMemberByOrder(orderId).ifPresent(member -> {
            GroupBuy groupBuy = repository.findGroupBuy(member.groupBuyId())
                    .orElseThrow(() -> new IllegalStateException("Group buy missing for member: " + member.id()));
            if ("FAILED".equals(groupBuy.status())) {
                throw ApiException.conflict("GROUP_BUY_FAILED", "拼团已失败，订单不能继续支付");
            }
            if (groupBuy.isForming() && groupBuy.expiresAt().isBefore(Instant.now())) {
                throw ApiException.conflict("GROUP_BUY_EXPIRED", "拼团已过期，请重新发起");
            }
            repository.markMemberPaid(orderId);
            repository.completeGroupIfFull(groupBuy.id());
        });
    }

    @Transactional
    public GroupBuyView createGroupBuy(CreateGroupBuyRequest request) {
        Promotion promotion = repository.find(request.promotionId())
                .orElseThrow(() -> ApiException.notFound("PROMOTION_NOT_FOUND", "拼团活动不存在"));
        if (promotion.type() != PromotionType.GROUP_BUY) {
            throw ApiException.badRequest("PROMOTION_NOT_GROUP_BUY", "该活动不是拼团活动");
        }
        Instant now = Instant.now();
        if (!promotion.isActiveAt(now)) {
            throw ApiException.conflict("PROMOTION_INACTIVE", "拼团活动未开始或已结束");
        }
        Sku sku = catalogService.getSku(promotion.skuId());
        GroupBuy groupBuy = new GroupBuy(
                Ids.newId(),
                nextGroupCode(),
                promotion.id(),
                promotion.skuId(),
                new Money(promotion.promoPriceAmount(), sku.price().currency()),
                promotion.groupSize(),
                request.phone().trim(),
                "FORMING",
                now.plus(Duration.ofHours(promotion.groupDurationHours())),
                now,
                null,
                0,
                0);
        repository.lockGroupCode(groupBuy.code());
        if (repository.findGroupBuyByCode(groupBuy.code()).isPresent()) {
            throw new IllegalStateException("Generated duplicate group buy code: " + groupBuy.code());
        }
        repository.insertGroupBuy(groupBuy);
        return groupBuyView(repository.findGroupBuy(groupBuy.id())
                .orElseThrow(() -> new IllegalStateException("Group buy missing after insert: " + groupBuy.id())));
    }

    @Transactional
    public void releaseOrder(UUID orderId) {
        for (PromotionClaim claim : repository.findUnreleasedClaims(orderId)) {
            if (claim.type() == PromotionType.FLASH_SALE) {
                repository.decreaseSold(claim.promotionId(), claim.saleQuantity());
            } else if (claim.type() == PromotionType.BUY_GIFT) {
                repository.decreaseSold(claim.promotionId(), claim.giftQuantity());
            }
        }
        repository.markClaimsReleased(orderId);
        repository.deleteUnpaidGroupMember(orderId);
    }

    public void markMemberRefunded(UUID orderId) {
        repository.markMemberRefunded(orderId);
    }

    public GroupBuyView findGroupBuy(String code) {
        GroupBuy groupBuy = repository.findGroupBuyByCode(code.trim().toUpperCase())
                .orElseThrow(() -> ApiException.notFound("GROUP_BUY_NOT_FOUND", "拼团不存在：" + code));
        return groupBuyView(groupBuy);
    }

    public List<GroupBuyView> listGroupBuys(UUID promotionId) {
        List<UUID> ids = repository.findGroupBuyIds(promotionId);
        return ids.stream()
                .map(id -> repository.findGroupBuy(id)
                        .orElseThrow(() -> new IllegalStateException("Group buy missing: " + id)))
                .map(this::groupBuyView)
                .toList();
    }

    @Scheduled(initialDelayString = "${app.promotion.group-expiry-initial-delay-ms:60000}",
            fixedDelayString = "${app.promotion.group-expiry-interval-ms:60000}")
    public void expireGroupBuys() {
        for (UUID groupBuyId : repository.findExpiredFormingGroups(Instant.now())) {
            List<UUID> refundOrderIds = repository.failGroup(groupBuyId);
            log.info("拼团 {} 已过期，待退款订单 {} 个", groupBuyId, refundOrderIds.size());
            if (!refundOrderIds.isEmpty()) {
                eventPublisher.publishEvent(new GroupBuyFailedEvent(groupBuyId, refundOrderIds));
            }
        }
        List<UUID> unpaidRefunds = repository.findAllUnrefundedOrderIds();
        if (!unpaidRefunds.isEmpty()) {
            log.warn("发现 {} 个拼团失败订单尚未完成退款，将重试", unpaidRefunds.size());
            eventPublisher.publishEvent(new GroupBuyFailedEvent(null, unpaidRefunds));
        }
    }

    private PromotionPricing prepareGroupBuy(Map<UUID, Integer> items, String phone, UUID groupBuyId, Instant now) {
        GroupBuy groupBuy = repository.findGroupBuy(groupBuyId)
                .orElseThrow(() -> ApiException.notFound("GROUP_BUY_NOT_FOUND", "拼团不存在：" + groupBuyId));
        if (!groupBuy.isForming()) {
            throw ApiException.conflict("GROUP_BUY_NOT_FORMING", "拼团已结束，不能继续下单");
        }
        if (!groupBuy.expiresAt().isAfter(now)) {
            throw ApiException.conflict("GROUP_BUY_EXPIRED", "拼团已过期，请重新发起");
        }
        if (groupBuy.joinedCount() >= groupBuy.groupSize()) {
            throw ApiException.conflict("GROUP_BUY_FULL", "该拼团人数已满");
        }
        if (items.size() != 1 || items.get(groupBuy.skuId()) == null) {
            throw ApiException.badRequest("GROUP_BUY_ITEMS_INVALID", "拼团订单只能包含该拼团商品");
        }
        if (items.get(groupBuy.skuId()) != 1) {
            throw ApiException.badRequest("GROUP_BUY_QUANTITY_INVALID", "每单拼团商品数量必须为 1 件");
        }
        Map<UUID, Money> overrides = new LinkedHashMap<>();
        overrides.put(groupBuy.skuId(), groupBuy.price());
        List<PromotionPricing.AppliedPromotion> applied = new ArrayList<>();
        applied.add(new PromotionPricing.AppliedPromotion(
                groupBuy.promotionId(), "GROUP-" + groupBuy.code(), "拼团 " + groupBuy.code(),
                PromotionType.GROUP_BUY, groupBuy.skuId(), 1, 0));
        return new PromotionPricing(overrides, List.of(), applied, groupBuy.id());
    }

    private void recordGroupMember(UUID groupBuyId, UUID orderId, String phone) {
        repository.lockGroup(groupBuyId);
        GroupBuy groupBuy = repository.findGroupBuy(groupBuyId)
                .orElseThrow(() -> new IllegalStateException("Group buy vanished during checkout: " + groupBuyId));
        if (!groupBuy.isForming()) {
            throw ApiException.conflict("GROUP_BUY_NOT_FORMING", "拼团已结束，不能继续下单");
        }
        if (!groupBuy.expiresAt().isAfter(Instant.now())) {
            throw ApiException.conflict("GROUP_BUY_EXPIRED", "拼团已过期，请重新发起");
        }
        if (groupBuy.joinedCount() >= groupBuy.groupSize()) {
            throw ApiException.conflict("GROUP_BUY_FULL", "该拼团人数已满");
        }
        String role = phone.equals(groupBuy.creatorPhone()) ? "CREATOR" : "MEMBER";
        repository.insertGroupMember(groupBuyId, orderId, phone, role);
    }

    private Integer stockLimit(UUID promotionId) {
        return repository.find(promotionId)
                .orElseThrow(() -> new IllegalStateException("Promotion missing: " + promotionId))
                .stockLimit();
    }

    private void validateRequest(CreatePromotionRequest request, Sku sku) {
        if (request.type() == PromotionType.FLASH_SALE) {
            if (request.promoPriceAmount() == null || request.promoPriceAmount() <= 0) {
                throw ApiException.badRequest("PROMOTION_PRICE_INVALID", "秒杀价必须大于 0");
            }
            if (request.promoPriceAmount() >= sku.price().amount()) {
                throw ApiException.badRequest("PROMOTION_PRICE_INVALID", "秒杀价必须低于商品原价");
            }
            if (request.stockLimit() == null || request.stockLimit() <= 0) {
                throw ApiException.badRequest("PROMOTION_STOCK_INVALID", "秒杀活动必须设置活动库存");
            }
        } else if (request.type() == PromotionType.BUY_GIFT) {
            if (request.giftSkuId() == null || request.buyQuantity() == null || request.giftQuantity() == null) {
                throw ApiException.badRequest("PROMOTION_GIFT_INVALID", "买赠活动必须选择赠品并设置购买/赠送数量");
            }
            if (request.maxGiftPerOrder() != null && request.maxGiftPerOrder() < request.giftQuantity()) {
                throw ApiException.badRequest("PROMOTION_GIFT_INVALID", "每单最多赠品数不能小于单次赠送数量");
            }
        } else if (request.type() == PromotionType.GROUP_BUY) {
            if (request.promoPriceAmount() == null || request.promoPriceAmount() <= 0) {
                throw ApiException.badRequest("PROMOTION_PRICE_INVALID", "拼团价必须大于 0");
            }
            if (request.promoPriceAmount() >= sku.price().amount()) {
                throw ApiException.badRequest("PROMOTION_PRICE_INVALID", "拼团价必须低于商品原价");
            }
            if (request.groupSize() == null || request.groupSize() < 2) {
                throw ApiException.badRequest("PROMOTION_GROUP_INVALID", "成团人数至少为 2 人");
            }
            if (request.groupDurationHours() == null || request.groupDurationHours() <= 0) {
                throw ApiException.badRequest("PROMOTION_GROUP_INVALID", "拼团有效时长必须大于 0 小时");
            }
        }
    }

    private PromotionView view(Promotion promotion, Instant now) {
        Sku sku = catalogService.getSku(promotion.skuId());
        Product product = catalogService.getProduct(sku.productId(), "zh-CN");
        Sku giftSku = promotion.giftSkuId() == null ? null : catalogService.getSku(promotion.giftSkuId());
        Product giftProduct = giftSku == null ? null : catalogService.getProduct(giftSku.productId(), "zh-CN");
        Integer remaining = promotion.stockLimit() == null
                ? null
                : Math.max(0, promotion.stockLimit() - promotion.soldQuantity());
        return new PromotionView(
                promotion.id(),
                promotion.code(),
                promotion.type(),
                promotion.title(),
                promotion.description(),
                promotion.status().name(),
                promotion.startsAt(),
                promotion.endsAt(),
                promotion.isActiveAt(now),
                promotion.isSoldOut(),
                promotion.skuId(),
                sku.skuCode(),
                product.id(),
                product.title(),
                sku.price(),
                promotion.promoPriceAmount() == null
                        ? null
                        : new Money(promotion.promoPriceAmount(), sku.price().currency()),
                promotion.giftSkuId(),
                giftSku == null ? null : giftSku.skuCode(),
                giftProduct == null ? null : giftProduct.title(),
                promotion.buyQuantity(),
                promotion.giftQuantity(),
                promotion.maxGiftPerOrder(),
                promotion.stockLimit(),
                promotion.soldQuantity(),
                remaining,
                promotion.perUserLimit(),
                promotion.groupSize(),
                promotion.groupDurationHours(),
                promotion.priority());
    }

    private GroupBuyView groupBuyView(GroupBuy groupBuy) {
        Sku sku = catalogService.getSku(groupBuy.skuId());
        Product product = catalogService.getProduct(sku.productId(), "zh-CN");
        List<GroupBuyMemberView> members = repository.findGroupMembers(groupBuy.id()).stream()
                .map(member -> new GroupBuyMemberView(
                        maskPhone(member.phone()), member.role(), member.paidAt() != null, member.joinedAt()))
                .toList();
        return new GroupBuyView(
                groupBuy.id(),
                groupBuy.code(),
                groupBuy.promotionId(),
                groupBuy.skuId(),
                sku.skuCode(),
                product.title(),
                groupBuy.price(),
                groupBuy.groupSize(),
                groupBuy.joinedCount(),
                groupBuy.paidCount(),
                Math.max(0, groupBuy.groupSize() - groupBuy.joinedCount()),
                groupBuy.status(),
                groupBuy.expiresAt(),
                groupBuy.createdAt(),
                groupBuy.completedAt(),
                members);
    }

    private String nextGroupCode() {
        StringBuilder builder = new StringBuilder("GB");
        for (int i = 0; i < 8; i++) {
            builder.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
        }
        return builder.toString();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}