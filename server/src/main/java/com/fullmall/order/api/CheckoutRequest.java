package com.fullmall.order.api;

import com.fullmall.order.domain.FulfillmentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CheckoutRequest(
        @Valid @NotEmpty @Size(max = 50) List<Item> items,
        @NotNull FulfillmentMode fulfillmentMode,
        UUID storeId,
        @Valid @NotNull Contact contact,
        @Valid Address address,
        List<UUID> legalAcceptanceIds,
        UUID referrerId,
        String referralMode,
        UUID groupBuyId
) {

    public record Item(
            @NotNull UUID skuId,
            @Min(1) @Max(1000) int quantity
    ) {
    }

    public record Contact(
            @NotBlank @Size(max = 64) String name,
            @NotBlank @Pattern(regexp = "^[0-9+\\- ]{6,32}$", message = "must be a valid phone number") String phone,
            String email
    ) {
    }

    public record Address(
            String province,
            String city,
            String district,
            @NotBlank String detail,
            String postalCode
    ) {
    }
}