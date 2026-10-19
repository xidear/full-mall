package com.fullmall.promotion;

import com.fullmall.catalog.app.CatalogService;
import com.fullmall.catalog.domain.Product;
import com.fullmall.catalog.domain.Sku;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.Money;
import com.fullmall.promotion.app.PromotionPricing;
import com.fullmall.promotion.app.PromotionService;
import com.fullmall.promotion.domain.Promotion;
import com.fullmall.promotion.domain.PromotionStatus;
import com.fullmall.promotion.domain.PromotionType;
import com.fullmall.promotion.infra.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromotionServiceTest {

    private static final UUID MUG_SKU = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID MILK_SKU = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final PromotionRepository repository = mock(PromotionRepository.class);
    private final CatalogService catalogService = mock(CatalogService.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final PromotionService service = new PromotionService(repository, catalogService, publisher);

    @Test
    void flashSaleOverridesPriceAndEnforcesStock() {
        Promotion flash = promotion(PromotionType.FLASH_SALE, 2990L, null, null, null, 30, 0);
        when(repository.findActive(any())).thenReturn(List.of(flash));
        when(catalogService.getSku(MUG_SKU)).thenReturn(sku(MUG_SKU, 3990));

        PromotionPricing pricing = service.prepare(Map.of(MUG_SKU, 2), "13800000000", null);

        assertThat(pricing.priceOverrides().get(MUG_SKU).amount()).isEqualTo(2990);
        assertThat(pricing.priceOverrides().get(MUG_SKU).currency()).isEqualTo("CNY");
        assertThat(pricing.pricePromotionForSku(MUG_SKU)).isPresent();
        assertThat(pricing.gifts()).isEmpty();
    }

    @Test
    void soldOutFlashSaleFailsExplicitly() {
        Promotion flash = promotion(PromotionType.FLASH_SALE, 2990L, null, null, null, 30, 30);
        when(repository.findActive(any())).thenReturn(List.of(flash));

        assertThatThrownBy(() -> service.prepare(Map.of(MUG_SKU, 1), "13800000000", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("已售罄");
    }

    @Test
    void buyGiftComputesGiftQuantityAndCapsPerOrder() {
        Promotion gift = promotion(PromotionType.BUY_GIFT, null, MILK_SKU, 2, 1, 20, 0);
        gift = new Promotion(gift.id(), gift.code(), gift.type(), gift.title(), gift.description(), gift.status(),
                gift.startsAt(), gift.endsAt(), MILK_SKU, null, gift.giftSkuId(), 2, 1, 3, 20, 0,
                null, null, null, 20, gift.createdAt());
        when(repository.findActive(any())).thenReturn(List.of(gift));
        when(catalogService.getSku(MILK_SKU)).thenReturn(sku(MILK_SKU, 1990));

        PromotionPricing pricing = service.prepare(Map.of(MILK_SKU, 8), "13800000000", null);

        assertThat(pricing.gifts()).hasSize(1);
        assertThat(pricing.gifts().get(0).quantity()).isEqualTo(3);
        assertThat(pricing.priceOverrides()).isEmpty();
        assertThat(pricing.applied().get(0).giftQuantity()).isEqualTo(3);
    }

    @Test
    void giftBelowThresholdDoesNotApply() {
        Promotion gift = promotion(PromotionType.BUY_GIFT, null, MILK_SKU, 2, 1, 20, 0);
        when(repository.findActive(any())).thenReturn(List.of(gift));

        PromotionPricing pricing = service.prepare(Map.of(MILK_SKU, 1), "13800000000", null);

        assertThat(pricing.gifts()).isEmpty();
    }

    @Test
    void groupBuyUsesCapturedGroupPriceAndRejectsExtraItems() {
        UUID groupId = UUID.randomUUID();
        Promotion group = promotion(PromotionType.GROUP_BUY, 2490L, null, null, null, null, 0);
        when(repository.findGroupBuy(groupId)).thenReturn(java.util.Optional.of(new com.fullmall.promotion.domain.GroupBuy(
                groupId, "GBTEST1234", group.id(), MUG_SKU, new Money(2490, "CNY"), 2,
                "13800000000", "FORMING", Instant.now().plusSeconds(3600), Instant.now(), null, 0, 0)));

        PromotionPricing pricing = service.prepare(Map.of(MUG_SKU, 1), "13800000000", groupId);

        assertThat(pricing.priceOverrides().get(MUG_SKU).amount()).isEqualTo(2490);
        assertThat(pricing.groupBuyId()).isEqualTo(groupId);
        assertThatThrownBy(() -> service.prepare(Map.of(MUG_SKU, 1, MILK_SKU, 1), "13800000000", groupId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("只能包含");
    }

    private Promotion promotion(PromotionType type, Long promoPrice, UUID giftSku, Integer buyQuantity,
                                Integer giftQuantity, Integer stockLimit, int soldQuantity) {
        return new Promotion(UUID.randomUUID(), "TEST-" + type, type, "测试活动", null, PromotionStatus.ACTIVE,
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), MUG_SKU,
                promoPrice, giftSku, buyQuantity, giftQuantity, 2, stockLimit, soldQuantity,
                5, type == PromotionType.GROUP_BUY ? 2 : null,
                type == PromotionType.GROUP_BUY ? 24 : null, 0, Instant.now());
    }

    private Sku sku(UUID id, long price) {
        return new Sku(id, PRODUCT_ID, "SKU-" + id, null, new Money(price, "CNY"), null, Map.of(), null, null);
    }
}
