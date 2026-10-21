package com.fullmall.distribution.api;

import com.fullmall.platform.money.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class DistributionDtos {

    private DistributionDtos() {
    }

    public record BindReferralRequest(
            @NotBlank String phone,
            @NotNull UUID promoterId,
            @NotBlank String mode,
            String source
    ) {
    }

    public record ReferralBindingView(
            UUID id, String customerPhone, UUID promoterId, String mode, String source,
            boolean active, Instant createdAt, Instant expiresAt
    ) {
    }

    public record CommissionView(
            UUID id, UUID orderId, String orderNo, UUID promoterId, String promoterName,
            Money orderAmount, Money commission, String status, Instant createdAt, Instant settledAt,
            /** 已回冲金额（售后退货完成后累加）。 */
            Money reversedAmount,
            /** 售后冻结中金额（>0 表示售后未关闭，不可提现）。 */
            Money holdAmount
    ) {
    }
}
