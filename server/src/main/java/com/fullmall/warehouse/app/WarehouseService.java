package com.fullmall.warehouse.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.util.Ids;
import com.fullmall.warehouse.api.WarehouseDtos.ConfirmStocktakeRequest;
import com.fullmall.warehouse.api.WarehouseDtos.CreateReceiptRequest;
import com.fullmall.warehouse.api.WarehouseDtos.CreateStocktakeRequest;
import com.fullmall.warehouse.api.WarehouseDtos.CreateTransferRequest;
import com.fullmall.warehouse.api.WarehouseDtos.LocationView;
import com.fullmall.warehouse.api.WarehouseDtos.ReceiptView;
import com.fullmall.warehouse.api.WarehouseDtos.ReplenishmentSuggestion;
import com.fullmall.warehouse.api.WarehouseDtos.StocktakeView;
import com.fullmall.warehouse.api.WarehouseDtos.TransferView;
import com.fullmall.warehouse.infra.WarehouseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository repository;
    private final int replenishmentTarget;

    public WarehouseService(WarehouseRepository repository,
                            @Value("${app.warehouse.replenishment-target:30}") int replenishmentTarget) {
        this.repository = repository;
        this.replenishmentTarget = replenishmentTarget;
    }

    public List<LocationView> locations() {
        return repository.locations();
    }

    @Transactional
    public ReceiptView createReceipt(CreateReceiptRequest request) {
        String locationType = locationType(request.locationType());
        UUID receiptId = repository.insertReceipt(
                "GR-" + Ids.orderNo(), locationType, request.locationId(), request.supplierId(),
                request.note());
        Set<String> batchKeys = new HashSet<>();
        for (CreateReceiptRequest.ReceiptLine line : request.lines()) {
            String batchNo = line.batchNo().trim();
            String key = line.skuId() + ":" + batchNo;
            if (!batchKeys.add(key)) {
                throw ApiException.badRequest("RECEIPT_DUPLICATE_BATCH",
                        "同一收货单中批次重复：" + line.batchNo());
            }
            if (repository.batchExists(line.skuId(), locationType, request.locationId(), batchNo)) {
                throw ApiException.conflict("RECEIPT_BATCH_EXISTS",
                        "批次已存在：" + batchNo);
            }
            String currency = line.currency() == null || line.currency().isBlank()
                    ? "CNY" : line.currency().trim().toUpperCase();
            repository.insertReceiptLine(receiptId, line.skuId(), batchNo, line.productionDate(),
                    line.expiryDate(), line.quantity(), line.unitCostAmount(), currency);
        }
        return repository.findReceipt(receiptId);
    }

    @Transactional
    public ReceiptView confirmReceipt(UUID receiptId) {
        ReceiptView receipt = requireReceipt(receiptId);
        if (!"DRAFT".equals(receipt.status())) {
            throw ApiException.conflict("RECEIPT_NOT_DRAFT", "收货单当前状态为 " + receipt.status());
        }
        for (var line : receipt.lines()) {
            if (repository.batchExists(line.skuId(), receipt.locationType(), receipt.locationId(), line.batchNo())) {
                throw ApiException.conflict("RECEIPT_BATCH_EXISTS", "批次已存在：" + line.batchNo());
            }
            UUID batchId = repository.insertBatch(line.skuId(), receipt.locationType(), receipt.locationId(),
                    line.batchNo(), line.productionDate(), line.expiryDate(), line.quantity(),
                    line.unitCost() == null ? null : line.unitCost().amount(),
                    line.unitCost() == null ? "CNY" : line.unitCost().currency());
            repository.insertStockMove(batchId, line.skuId(), receipt.locationType(), receipt.locationId(),
                    "RECEIPT", line.quantity(), "GOODS_RECEIPT", receiptId);
        }
        repository.markReceiptConfirmed(receiptId, Instant.now());
        return repository.findReceipt(receiptId);
    }

    public List<ReceiptView> listReceipts() {
        return repository.listReceipts(100);
    }

    @Transactional
    public StocktakeView createStocktake(CreateStocktakeRequest request) {
        String locationType = locationType(request.locationType());
        List<WarehouseRepository.BatchRow> batches = repository.batchesAt(locationType, request.locationId());
        if (batches.isEmpty()) {
            throw ApiException.conflict("STOCKTAKE_NO_BATCHES",
                    "该地点没有可盘点批次");
        }
        UUID stocktakeId = repository.insertStocktake(
                "ST-" + Ids.orderNo(), locationType, request.locationId(), request.note());
        for (WarehouseRepository.BatchRow batch : batches) {
            repository.insertStocktakeLine(stocktakeId, batch.id(), batch.skuId(), batch.quantity());
        }
        return repository.findStocktake(stocktakeId);
    }

    @Transactional
    public StocktakeView confirmStocktake(UUID stocktakeId, ConfirmStocktakeRequest request) {
        StocktakeView stocktake = requireStocktake(stocktakeId);
        if (!"DRAFT".equals(stocktake.status())) {
            throw ApiException.conflict("STOCKTAKE_NOT_DRAFT", "盘点单当前状态为 " + stocktake.status());
        }
        Set<UUID> seen = new HashSet<>();
        for (ConfirmStocktakeRequest.CountedLine counted : request.lines()) {
            if (!seen.add(counted.lineId())) {
                throw ApiException.badRequest("STOCKTAKE_DUPLICATE_LINE", "盘点行重复：" + counted.lineId());
            }
            var line = stocktake.lines().stream()
                    .filter(candidate -> candidate.id().equals(counted.lineId()))
                    .findFirst()
                    .orElseThrow(() -> ApiException.badRequest("STOCKTAKE_LINE_NOT_FOUND",
                            "盘点行不存在：" + counted.lineId()));
            int variance = counted.countedQuantity() - line.systemQuantity();
            repository.updateStocktakeLine(counted.lineId(), counted.countedQuantity(), variance, counted.reason());
            repository.setBatchQuantity(line.batchId(), counted.countedQuantity());
            if (variance != 0) {
                repository.insertStockMove(line.batchId(), line.skuId(), stocktake.locationType(),
                        stocktake.locationId(), "STOCKTAKE_ADJUST", variance, "STOCKTAKE", stocktakeId);
            }
        }
        repository.markStocktakeConfirmed(stocktakeId, Instant.now());
        return repository.findStocktake(stocktakeId);
    }

    public List<StocktakeView> listStocktakes() {
        return repository.listStocktakes(100);
    }

    @Transactional
    public TransferView createTransfer(CreateTransferRequest request) {
        String fromType = locationType(request.fromType());
        String toType = locationType(request.toType());
        if (fromType.equals(toType) && request.fromId().equals(request.toId())) {
            throw ApiException.badRequest("TRANSFER_SAME_LOCATION", "调出与调入门店/仓库不能相同");
        }
        UUID transferId = repository.insertTransfer("TR-" + Ids.orderNo(), fromType, request.fromId(),
                toType, request.toId(), request.note());
        for (CreateTransferRequest.TransferLine line : request.lines()) {
            repository.insertTransferLine(transferId, line.skuId(), line.quantity());
        }
        return repository.findTransfer(transferId);
    }

    @Transactional
    public TransferView shipTransfer(UUID transferId) {
        TransferView transfer = requireTransfer(transferId);
        if (!"DRAFT".equals(transfer.status())) {
            throw ApiException.conflict("TRANSFER_NOT_DRAFT", "调拨单当前状态为 " + transfer.status());
        }
        for (var line : transfer.lines()) {
            List<WarehouseRepository.BatchRow> batches = repository.availableBatchesAt(
                    line.skuId(), transfer.fromType(), transfer.fromId());
            int remaining = line.quantity();
            for (WarehouseRepository.BatchRow batch : batches) {
                int available = batch.quantity() - batch.reservedQuantity();
                int take = Math.min(remaining, available);
                if (take <= 0) {
                    continue;
                }
                if (!repository.decrementForTransfer(batch.id(), take)) {
                    throw ApiException.conflict("TRANSFER_STOCK_RACE", "调拨库存已变化，请重试");
                }
                repository.insertTransferAllocation(line.id(), batch, take);
                repository.insertStockMove(batch.id(), line.skuId(), transfer.fromType(), transfer.fromId(),
                        "TRANSFER_OUT", -take, "STOCK_TRANSFER", transferId);
                remaining -= take;
            }
            if (remaining > 0) {
                throw ApiException.conflict("TRANSFER_INSUFFICIENT_STOCK",
                        "商品 " + line.skuCode() + " 可用库存不足，缺少 " + remaining + " 件");
            }
        }
        repository.markTransferShipped(transferId, Instant.now());
        return repository.findTransfer(transferId);
    }

    @Transactional
    public TransferView receiveTransfer(UUID transferId) {
        TransferView transfer = requireTransfer(transferId);
        if (!"IN_TRANSIT".equals(transfer.status())) {
            throw ApiException.conflict("TRANSFER_NOT_IN_TRANSIT", "调拨单当前状态为 " + transfer.status());
        }
        for (WarehouseRepository.AllocationRow allocation : repository.allocations(transferId)) {
            UUID destinationBatchId = repository.upsertDestinationBatch(
                    transfer.toType(), transfer.toId(), allocation);
            repository.insertStockMove(destinationBatchId, allocation.skuId(), transfer.toType(),
                    transfer.toId(), "TRANSFER_IN", allocation.quantity(), "STOCK_TRANSFER", transferId);
        }
        repository.markTransferReceived(transferId, Instant.now());
        return repository.findTransfer(transferId);
    }

    public List<TransferView> listTransfers() {
        return repository.listTransfers(100);
    }

    public List<ReplenishmentSuggestion> replenishment(String locationType, UUID locationId) {
        return repository.replenishment(locationType(locationType), locationId, replenishmentTarget);
    }

    private ReceiptView requireReceipt(UUID receiptId) {
        ReceiptView receipt = repository.findReceipt(receiptId);
        if (receipt == null) {
            throw ApiException.notFound("RECEIPT_NOT_FOUND", "收货单不存在：" + receiptId);
        }
        return receipt;
    }

    private StocktakeView requireStocktake(UUID stocktakeId) {
        StocktakeView stocktake = repository.findStocktake(stocktakeId);
        if (stocktake == null) {
            throw ApiException.notFound("STOCKTAKE_NOT_FOUND", "盘点单不存在：" + stocktakeId);
        }
        return stocktake;
    }

    private TransferView requireTransfer(UUID transferId) {
        TransferView transfer = repository.findTransfer(transferId);
        if (transfer == null) {
            throw ApiException.notFound("TRANSFER_NOT_FOUND", "调拨单不存在：" + transferId);
        }
        return transfer;
    }

    private String locationType(String value) {
        try {
            return com.fullmall.inventory.domain.LocationType
                    .valueOf(value.trim().toUpperCase()).name();
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("WAREHOUSE_LOCATION_TYPE_INVALID",
                    "地点类型必须是 WAREHOUSE/STORE/SUPPLIER，实际为 " + value);
        }
    }
}