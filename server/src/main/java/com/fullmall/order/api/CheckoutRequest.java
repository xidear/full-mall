package com.fullmall.order.api;

import com.fullmall.order.domain.FulfillmentMode;
import com.fullmall.platform.validation.PhoneNumber;
import com.fullmall.platform.validation.ValidationRules;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * 刻意不含金额字段：单价与总额由 OrderService 按 SKU 现价（分）重算，客户端传入的只是展示值。
 * 幂等键走 `Idempotency-Key` 请求头；校验数字与正则见 docs/validation.schema.json，经 ValidationRules 引用。
 */
public record CheckoutRequest(
        // 单次结算上限 50 行：限制请求体体积与事务内的 SKU 查询次数；同 skuId 重复出现由 OrderService 合并累加
        @Valid @NotEmpty @Size(max = 50) List<Item> items,
        @NotNull FulfillmentMode fulfillmentMode,
        UUID storeId,
        @Valid @NotNull Contact contact,
        @Valid Address address,
        List<UUID> legalAcceptanceIds,
        UUID referrerId,
        String referralMode,
        UUID groupBuyId,
        @Valid CheckoutContext context,
        /** 可选：砍价场次 id（READY 时按锁定价结算） */
        UUID bargainSessionId
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
            @Min(ValidationRules.QUANTITY_MIN) @Max(ValidationRules.QUANTITY_MAX) int quantity
    ) {
    }

    /** 只校验格式、不代表号码可达（可达性由 otpCode 证明）；号码本身是订单归属会员的匹配键。 */
    public record Contact(
            @NotBlank @Size(max = ValidationRules.CONTACT_NAME_MAX) String name,
            @NotBlank @Pattern(regexp = PhoneNumber.PATTERN, message = "must be a valid phone number") String phone,
            @Email String email
    ) {
    }

    /** 仅配送（SHIPPING）需要；自提时允许为 null，由 OrderService 报 ADDRESS_REQUIRED，不在此按 mode 做条件校验。 */
    public record Address(
            String province,
            String city,
            String district,
            @NotBlank String detail,
            String postalCode
    ) {
    }
}