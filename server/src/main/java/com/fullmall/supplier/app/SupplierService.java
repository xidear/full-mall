package com.fullmall.supplier.app;

import com.fullmall.fulfillment.app.FulfillmentService;
import com.fullmall.fulfillment.domain.FulfillmentOrder;
import com.fullmall.inventory.app.InventoryService;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.Money;
import com.fullmall.supplier.api.SupplierAcceptRequest;
import com.fullmall.supplier.api.SupplierOrderLineView;
import com.fullmall.supplier.api.SupplierOrderView;
import com.fullmall.supplier.api.SupplierPingView;
import com.fullmall.supplier.api.SupplierRejectRequest;
import com.fullmall.supplier.api.SupplierShipRequest;
import com.fullmall.supplier.api.SupplierStockFeedRequest;
import com.fullmall.supplier.api.SupplierStockFeedResult;
import com.fullmall.supplier.infra.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SupplierService {

    private static final String DEFAULT_CURRENCY = "CNY";

    private final SupplierRepository repository;
    private final FulfillmentService fulfillmentService;
    private final InventoryService inventoryService;

    public SupplierService(SupplierRepository repository,
                           FulfillmentService fulfillmentService,
                           InventoryService inventoryService) {
        this.repository = repository;
        this.fulfillmentService = fulfillmentService;
        this.inventoryService = inventoryService;
    }

    public SupplierPingView ping(UUID supplierId) {
        String name = repository.supplierName(supplierId);
        if (name == null) {
            throw ApiException.notFound("SUPPLIER_NOT_FOUND", "供应商不存在：" + supplierId);
        }
        return new SupplierPingView(supplierId, name, Instant.now());
    }

    public List<SupplierOrderView> listOrders(UUID supplierId, String status) {
        return repository.findOrders(supplierId, status).stream()
                .map(header -> new SupplierOrderView(
                        header.fulfillmentId(),
                        header.orderId(),
                        header.orderNo(),
                        header.status(),
                        header.createdAt(),
                        header.acceptedAt(),
                        header.carrierCode(),
                        header.trackingNo(),
                        header.contactName(),
                        header.contactPhone(),
                        header.addressJson(),
                        new Money(header.totalAmount(), header.currency()),
                        header.note(),
                        repository.findOrderLines(header.fulfillmentId(), supplierId).stream()
                                .map(line -> new SupplierOrderLineView(
                                        line.skuId(),
                                        line.title(),
                                        line.skuCode(),
                                        line.supplierSkuCode(),
                                        line.quantity(),
                                        new Money(line.unitPrice(), line.currency())))
                                .toList()))
                .toList();
    }

    @Transactional
    public SupplierOrderView accept(UUID supplierId, UUID fulfillmentId, SupplierAcceptRequest request) {
        FulfillmentOrder fulfillment = fulfillmentService.acceptSupplierOrder(
                fulfillmentId, supplierId, request.note());
        return findOrder(supplierId, fulfillment.id());
    }

    @Transactional
    public SupplierOrderView ship(UUID supplierId, UUID fulfillmentId, SupplierShipRequest request) {
        FulfillmentOrder fulfillment = fulfillmentService.shipSupplierOrder(
                fulfillmentId, supplierId, request.carrierCode().trim(), request.trackingNo().trim());
        return findOrder(supplierId, fulfillment.id());
    }

    @Transactional
    public SupplierOrderView reject(UUID supplierId, UUID fulfillmentId, SupplierRejectRequest request) {
        FulfillmentOrder fulfillment = fulfillmentService.rejectSupplierOrder(
                fulfillmentId, supplierId, request.reason().trim());
        return findOrder(supplierId, fulfillment.id());
    }

    @Transactional
    public SupplierStockFeedResult updateStock(UUID supplierId, SupplierStockFeedRequest request) {
        int updated = 0;
        for (SupplierStockFeedRequest.Item item : request.items()) {
            SupplierRepository.SupplierSkuRow sku = repository.findSupplierSku(supplierId, item.supplierSkuCode());
            if (sku == null || !sku.active()) {
                throw ApiException.conflict("SUPPLIER_SKU_NOT_FOUND",
                        "供应商 SKU 映射不存在或已停用：" + item.supplierSkuCode());
            }
            if (item.priceAmount() != null) {
                if (item.priceAmount() < 0) {
                    throw ApiException.badRequest("SUPPLIER_PRICE_INVALID", "供应商价格不能为负数");
                }
                String currency = item.priceCurrency() == null || item.priceCurrency().isBlank()
                        ? DEFAULT_CURRENCY
                        : item.priceCurrency().trim().toUpperCase();
                repository.updateSupplierSkuPrice(supplierId, sku.skuId(), item.priceAmount(), currency);
            }
            inventoryService.upsertSupplierStock(supplierId, sku.skuId(), item.availableQuantity());
            repository.upsertStockFeed(supplierId, sku.skuId(), sku.supplierSkuCode(),
                    item.availableQuantity(), item.priceAmount(), item.priceCurrency());
            updated++;
        }
        return new SupplierStockFeedResult(updated, 0);
    }

    private SupplierOrderView findOrder(UUID supplierId, UUID fulfillmentId) {
        return listOrders(supplierId, null).stream()
                .filter(order -> order.fulfillmentId().equals(fulfillmentId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("SUPPLIER_ORDER_NOT_FOUND",
                        "供应商订单不存在：" + fulfillmentId));
    }
}