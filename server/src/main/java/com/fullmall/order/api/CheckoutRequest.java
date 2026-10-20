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
        UUID groupBuyId,
        @Valid CheckoutContext context
) {

    /**
     * 结算的可选上下文（积分、券、验证码、下单来源）。
     *
     * <p>收成一个对象而不是 5 个散字段：`CheckoutRequest` 的参数曾达 14 个，
     * 位置传参已导致过"券没提交上却以为提交了"的实际故障。
     */
    public record CheckoutContext(
            @jakarta.validation.constraints.Min(0) Long pointsToUse,
            UUID couponId,
            /** 访客下单必须提供手机验证码（已登录会员可省略）。 */
            String otpCode,
            /** 下单来源：CUSTOMER（默认）/ CASHIER / ADMIN。 */
            String placedBy,
            /** 员工代下单时的操作员（必填）。 */
            String placedByOperator
    ) {
        public static CheckoutContext none() {
            return new CheckoutContext(null, null, null, null, null);
        }
    }

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