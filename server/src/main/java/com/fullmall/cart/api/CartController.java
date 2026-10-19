package com.fullmall.cart.api;

import com.fullmall.cart.app.CartOwnerResolver;
import com.fullmall.cart.app.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 购物车接口：登录用户或游客 token 均可用；登录后可合并游客车。 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService carts;
    private final CartOwnerResolver owners;

    public CartController(CartService carts, CartOwnerResolver owners) {
        this.carts = carts;
        this.owners = owners;
    }

    public record AddItem(@NotNull UUID skuId, @Min(1) int quantity) {
    }

    public record UpdateItem(@Min(1) int quantity) {
    }

    @PostMapping("/items")
    public List<CartService.CartItem> add(@Valid @RequestBody AddItem body,
                                          Authentication authentication,
                                          @RequestHeader(value = CartOwnerResolver.GUEST_TOKEN_HEADER, required = false)
                                          String guestToken) {
        carts.addItem(owners.ownerKey(authentication, guestToken), body.skuId(), body.quantity());
        return carts.items(owners.ownerKey(authentication, guestToken));
    }

    @PutMapping("/items/{skuId}")
    public List<CartService.CartItem> update(@PathVariable UUID skuId, @Valid @RequestBody UpdateItem body,
                                             Authentication authentication,
                                             @RequestHeader(value = CartOwnerResolver.GUEST_TOKEN_HEADER, required = false)
                                             String guestToken) {
        String owner = owners.ownerKey(authentication, guestToken);
        carts.updateQuantity(owner, skuId, body.quantity());
        return carts.items(owner);
    }

    @DeleteMapping("/items/{skuId}")
    public List<CartService.CartItem> remove(@PathVariable UUID skuId, Authentication authentication,
                                             @RequestHeader(value = CartOwnerResolver.GUEST_TOKEN_HEADER, required = false)
                                             String guestToken) {
        String owner = owners.ownerKey(authentication, guestToken);
        carts.remove(owner, skuId);
        return carts.items(owner);
    }

    @GetMapping
    public List<CartService.CartItem> items(Authentication authentication,
                                            @RequestHeader(value = CartOwnerResolver.GUEST_TOKEN_HEADER, required = false)
                                            String guestToken) {
        return carts.items(owners.ownerKey(authentication, guestToken));
    }

    /** 登录后合并游客车：需要登录身份 + 游客 token。 */
    @PostMapping("/merge")
    public Map<String, Integer> merge(Authentication authentication,
                                      @RequestHeader(value = CartOwnerResolver.GUEST_TOKEN_HEADER, required = false)
                                      String guestToken) {
        String member = owners.memberKey(authentication);
        String guest = owners.guestKey(guestToken);
        return Map.of("merged", carts.merge(guest, member));
    }
}
