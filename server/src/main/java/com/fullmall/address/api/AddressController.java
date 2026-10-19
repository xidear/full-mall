package com.fullmall.address.api;

import com.fullmall.address.app.AddressService;
import com.fullmall.platform.identity.OwnerKeyResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 地址簿接口。
 *
 * <p>归属解析复用 {@link CartOwnerResolver}：已登录 → `member:<用户名>`；
 * 未登录 → `X-Cart-Token` → `guest:<token>`。会员登录（MEM-001）落地后，
 * 地址将只允许登录用户访问，这里的游客分支会收窄。
 */
@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService addresses;
    private final OwnerKeyResolver owners;

    public AddressController(AddressService addresses, OwnerKeyResolver owners) {
        this.addresses = addresses;
        this.owners = owners;
    }

    public record AddressBody(@NotBlank String recipient, @NotBlank String phone,
                              String regionCode, String province, String city, String district,
                              @NotBlank String detail, String postalCode) {
    }

    private AddressService.AddressInput toInput(AddressBody body) {
        return new AddressService.AddressInput(body.recipient(), body.phone(), body.regionCode(),
                body.province(), body.city(), body.district(), body.detail(), body.postalCode());
    }

    private String owner(Authentication authentication, String guestToken) {
        return owners.ownerKey(authentication, guestToken);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<AddressService.AddressView> create(
            @Valid @RequestBody AddressBody body, Authentication authentication,
            @RequestHeader(value = OwnerKeyResolver.GUEST_TOKEN_HEADER, required = false) String guestToken) {
        String key = owner(authentication, guestToken);
        addresses.create(key, toInput(body));
        return addresses.list(key);
    }

    @PutMapping("/{id}")
    public List<AddressService.AddressView> update(
            @PathVariable UUID id, @Valid @RequestBody AddressBody body, Authentication authentication,
            @RequestHeader(value = OwnerKeyResolver.GUEST_TOKEN_HEADER, required = false) String guestToken) {
        String key = owner(authentication, guestToken);
        addresses.update(key, id, toInput(body));
        return addresses.list(key);
    }

    @PostMapping("/{id}/default")
    public List<AddressService.AddressView> setDefault(
            @PathVariable UUID id, Authentication authentication,
            @RequestHeader(value = OwnerKeyResolver.GUEST_TOKEN_HEADER, required = false) String guestToken) {
        String key = owner(authentication, guestToken);
        addresses.setDefault(key, id);
        return addresses.list(key);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication,
                       @RequestHeader(value = OwnerKeyResolver.GUEST_TOKEN_HEADER, required = false) String guestToken) {
        addresses.delete(owner(authentication, guestToken), id);
    }

    @GetMapping
    public List<AddressService.AddressView> list(Authentication authentication,
                                                 @RequestHeader(value = OwnerKeyResolver.GUEST_TOKEN_HEADER, required = false) String guestToken) {
        return addresses.list(owner(authentication, guestToken));
    }

    @GetMapping("/{id}")
    public AddressService.Address detail(@PathVariable UUID id, Authentication authentication,
                                         @RequestHeader(value = OwnerKeyResolver.GUEST_TOKEN_HEADER, required = false) String guestToken) {
        return addresses.get(owner(authentication, guestToken), id);
    }
}
