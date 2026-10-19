package com.fullmall.warehouse.api;

import com.fullmall.platform.money.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class WarehouseDtos {

    private WarehouseDtos() {
    }

    public record LocationView(UUID id, String name, String type) {
    }

    public record ReceiptLineView(
            UUID id, UUID skuId, String skuCode, String title, String batchNo,
            LocalDate productionDate, LocalDate expiryDate, int quantity, Money unitCost
    ) {
    }

    public record ReceiptView(
            UUID id, String receiptNo, String locationType, UUID locationId, String status,
            String note, Instant createdAt, Instant confirmedAt, List<ReceiptLineView> lines
    ) {
    }

    public record CreateReceiptRequest(
            @NotBlank String locationType,
            @NotNull UUID locationId,
            UUID supplierId,
            String note,
            @NotEmpty List<@Valid ReceiptLine> lines
    ) {
        public record ReceiptLine(
                @NotNull UUID skuId,
                @NotBlank String batchNo,
                LocalDate productionDate,
                LocalDate expiryDate,
                @Positive int quantity,
                Long unitCostAmount,
                String currency
        ) {
        }
    }

    public record CreateStocktakeRequest(
            @NotBlank String locationType,
            @NotNull UUID locationId,
            String note
    ) {
    }

    public record StocktakeLineView(
            UUID id, UUID batchId, UUID skuId, String skuCode, String title, String batchNo,
            LocalDate expiryDate, int systemQuantity, Integer countedQuantity, Integer variance, String reason
    ) {
    }

    public record StocktakeView(
            UUID id, String stocktakeNo, String locationType, UUID locationId, String status,
            String note, Instant createdAt, Instant confirmedAt, List<StocktakeLineView> lines
    ) {
    }

    public record ConfirmStocktakeRequest(
            @NotEmpty List<@Valid CountedLine> lines
    ) {
        public record CountedLine(
                @NotNull UUID lineId,
                @Min(0) int countedQuantity,
                String reason
        ) {
        }
    }

    public record CreateTransferRequest(
            @NotBlank String fromType,
            @NotNull UUID fromId,
            @NotBlank String toType,
            @NotNull UUID toId,
            String note,
            @NotEmpty List<@Valid TransferLine> lines
    ) {
        public record TransferLine(@NotNull UUID skuId, @Positive int quantity) {
        }
    }

    public record TransferLineView(UUID id, UUID skuId, String skuCode, String title, int quantity) {
    }

    public record TransferView(
            UUID id, String transferNo, String fromType, UUID fromId, String toType, UUID toId,
            String status, String note, Instant createdAt, Instant shippedAt, Instant receivedAt,
            List<TransferLineView> lines
    ) {
    }

    public record ReplenishmentSuggestion(
            UUID skuId, String skuCode, String title, String locationType, UUID locationId,
            int availableQuantity, int targetQuantity, int suggestedQuantity, LocalDate nearestExpiry
    ) {
    }
}
