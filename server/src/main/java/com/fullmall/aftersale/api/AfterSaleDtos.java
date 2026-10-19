package com.fullmall.aftersale.api;

import com.fullmall.platform.money.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AfterSaleDtos {

    private AfterSaleDtos() {
    }

    public record CreateAfterSaleRequest(
            @NotBlank String type,
            @NotBlank String reason,
            String description,
            @NotBlank String contactPhone,
            @NotEmpty List<@Valid Item> items
    ) {
        public record Item(@NotNull UUID orderLineId, @Positive int quantity) {
        }
    }

    public record AfterSaleLineView(
            UUID id, UUID orderLineId, UUID skuId, String title, String skuCode, int quantity
    ) {
    }

    public record AfterSaleEventView(
            UUID id, String eventType, String note, String operatorName, Instant createdAt
    ) {
    }

    public record AfterSaleView(
            UUID id, String afterSaleNo, UUID orderId, String orderNo, String type, String status,
            String reason, String description, String contactPhone, Money refundAmount,
            String adminNote, Instant createdAt, Instant approvedAt, Instant receivedAt,
            Instant refundedAt, Instant rejectedAt, List<AfterSaleLineView> lines,
            List<AfterSaleEventView> events
    ) {
    }

    public record ApproveAfterSaleRequest(String adminNote) {
    }

    public record RejectAfterSaleRequest(@NotBlank String reason) {
    }

    public record ReceiveAfterSaleRequest(@NotBlank String locationType, @NotNull UUID locationId) {
    }
}
