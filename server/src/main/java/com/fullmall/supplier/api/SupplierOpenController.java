package com.fullmall.supplier.api;

import com.fullmall.supplier.app.SupplierService;
import com.fullmall.supplier.web.SupplierHmacFilter;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/open/v1")
public class SupplierOpenController {

    private final SupplierService supplierService;
    private final com.fullmall.supplier.app.SupplierSettlementService settlementService;
    private final com.fullmall.supplier.app.SupplierSettlementCsvService settlementCsvService;

    public SupplierOpenController(SupplierService supplierService,
                                  com.fullmall.supplier.app.SupplierSettlementService settlementService,
                                  com.fullmall.supplier.app.SupplierSettlementCsvService settlementCsvService) {
        this.supplierService = supplierService;
        this.settlementService = settlementService;
        this.settlementCsvService = settlementCsvService;
    }

    @GetMapping(value = "/settlements.csv", produces = "text/csv;charset=UTF-8")
    public org.springframework.http.ResponseEntity<String> settlementsCsv(
            @RequestAttribute(SupplierHmacFilter.SUPPLIER_ID_ATTRIBUTE) UUID supplierId,
            @RequestParam(defaultValue = "200") int limit) {
        var csv = settlementCsvService.export(supplierId, limit, "supplier:" + supplierId, null);
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"supplier-settlements.csv\"")
                .body(csv.content());
    }

    @GetMapping("/settlements")
    public List<com.fullmall.supplier.infra.SupplierSettlementRepository.SettlementRow> settlements(
            @RequestAttribute(SupplierHmacFilter.SUPPLIER_ID_ATTRIBUTE) UUID supplierId,
            @RequestParam(defaultValue = "20") int limit) {
        return settlementService.list(supplierId, limit);
    }

    @GetMapping("/ping")
    public SupplierPingView ping(
            @RequestAttribute(SupplierHmacFilter.SUPPLIER_ID_ATTRIBUTE) UUID supplierId) {
        return supplierService.ping(supplierId);
    }

    @GetMapping("/orders")
    public List<SupplierOrderView> orders(
            @RequestAttribute(SupplierHmacFilter.SUPPLIER_ID_ATTRIBUTE) UUID supplierId,
            @RequestParam(required = false) String status) {
        return supplierService.listOrders(supplierId, status);
    }

    @GetMapping("/orders/{fulfillmentId}")
    public SupplierOrderView order(
            @RequestAttribute(SupplierHmacFilter.SUPPLIER_ID_ATTRIBUTE) UUID supplierId,
            @PathVariable UUID fulfillmentId) {
        return supplierService.getOrder(supplierId, fulfillmentId);
    }

    @PostMapping("/orders/{fulfillmentId}/accept")
    public SupplierOrderView accept(
            @RequestAttribute(SupplierHmacFilter.SUPPLIER_ID_ATTRIBUTE) UUID supplierId,
            @PathVariable UUID fulfillmentId,
            @Valid @RequestBody SupplierAcceptRequest request) {
        return supplierService.accept(supplierId, fulfillmentId, request);
    }

    @PostMapping("/orders/{fulfillmentId}/ship")
    public SupplierOrderView ship(
            @RequestAttribute(SupplierHmacFilter.SUPPLIER_ID_ATTRIBUTE) UUID supplierId,
            @PathVariable UUID fulfillmentId,
            @Valid @RequestBody SupplierShipRequest request) {
        return supplierService.ship(supplierId, fulfillmentId, request);
    }

    @PostMapping("/orders/{fulfillmentId}/reject")
    public SupplierOrderView reject(
            @RequestAttribute(SupplierHmacFilter.SUPPLIER_ID_ATTRIBUTE) UUID supplierId,
            @PathVariable UUID fulfillmentId,
            @Valid @RequestBody SupplierRejectRequest request) {
        return supplierService.reject(supplierId, fulfillmentId, request);
    }

    @PostMapping("/inventory/feed")
    public SupplierStockFeedResult inventoryFeed(
            @RequestAttribute(SupplierHmacFilter.SUPPLIER_ID_ATTRIBUTE) UUID supplierId,
            @Valid @RequestBody SupplierStockFeedRequest request) {
        return supplierService.updateStock(supplierId, request);
    }
}
