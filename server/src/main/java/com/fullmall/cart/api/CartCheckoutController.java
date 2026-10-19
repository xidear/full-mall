package com.fullmall.cart.api;

import com.fullmall.address.app.AddressService;
import com.fullmall.platform.identity.OwnerKeyResolver;
import com.fullmall.cart.app.CartService;
import com.fullmall.order.api.CheckoutRequest;
import com.fullmall.order.app.OrderService;
import com.fullmall.platform.error.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 购物车结算：把购物车内容转成下单请求。
 *
 * <p>规则：
 * <ul>
 *   <li>购物车为空直接拒绝（不产生 0 行订单）；</li>
 *   <li>收货人/电话/地址**取自地址簿并按归属人校验**，下单后冻结进订单快照；</li>
 *   <li>下单成功后清空购物车；下单失败不动购物车（避免用户丢商品）。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartCheckoutController {

    private final CartService carts;
    private final OwnerKeyResolver owners;
    private final AddressService addresses;
    private final OrderService orders;

    public CartCheckoutController(CartService carts, OwnerKeyResolver owners,
                                  AddressService addresses, OrderService orders) {
        this.carts = carts;
        this.owners = owners;
        this.addresses = addresses;
        this.orders = orders;
    }

    public record Body(UUID addressId, String fulfillmentMode, UUID storeId) {
    }

    @PostMapping("/checkout")
    public com.fullmall.order.domain.Order checkout(
            @RequestBody Body body, Authentication authentication,
            @RequestHeader(value = OwnerKeyResolver.GUEST_TOKEN_HEADER, required = false) String guestToken,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String owner = owners.ownerKey(authentication, guestToken);

        List<CartService.CartItem> items = carts.items(owner);
        if (items.isEmpty()) {
            throw ApiException.badRequest("CART_EMPTY", "购物车为空，无法结算");
        }
        if (body.addressId() == null) {
            throw ApiException.badRequest("ADDRESS_REQUIRED", "必须选择收货地址");
        }

        AddressService.Address address = addresses.get(owner, body.addressId());

        CheckoutRequest request = new CheckoutRequest(
                items.stream().map(item -> new CheckoutRequest.Item(item.skuId(), item.quantity())).toList(),
                com.fullmall.order.domain.FulfillmentMode.valueOf(
                        body.fulfillmentMode() == null ? "SHIP" : body.fulfillmentMode()),
                body.storeId(),
                new CheckoutRequest.Contact(address.recipient(), address.phone(), null),
                new CheckoutRequest.Address(address.province(), address.city(), address.district(),
                        address.detail(), address.postalCode()),
                List.of(),
                null, null, null);

        var order = orders.checkout(request, idempotencyKey);
        carts.clear(owner);
        return order;
    }
}
