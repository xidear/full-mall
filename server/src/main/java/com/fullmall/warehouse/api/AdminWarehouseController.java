package com.fullmall.warehouse.api;

import com.fullmall.warehouse.api.WarehouseDtos.ConfirmStocktakeRequest;
import com.fullmall.warehouse.api.WarehouseDtos.CreateReceiptRequest;
import com.fullmall.warehouse.api.WarehouseDtos.CreateStocktakeRequest;
import com.fullmall.warehouse.api.WarehouseDtos.CreateTransferRequest;
import com.fullmall.warehouse.api.WarehouseDtos.LocationView;
import com.fullmall.warehouse.api.WarehouseDtos.ReceiptView;
import com.fullmall.warehouse.api.WarehouseDtos.ReplenishmentSuggestion;
import com.fullmall.warehouse.api.WarehouseDtos.StocktakeView;
import com.fullmall.warehouse.api.WarehouseDtos.TransferView;
import com.fullmall.warehouse.app.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/warehouse")
public class AdminWarehouseController {

    private final WarehouseService warehouseService;

    public AdminWarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/locations")
    public List<LocationView> locations() {
        return warehouseService.locations();
    }

    @GetMapping("/receipts")
    public List<ReceiptView> receipts() {
        return warehouseService.listReceipts();
    }

    @PostMapping("/receipts")
    public ReceiptView createReceipt(@Valid @RequestBody CreateReceiptRequest request) {
        return warehouseService.createReceipt(request);
    }

    @PostMapping("/receipts/{receiptId}/confirm")
    public ReceiptView confirmReceipt(@PathVariable UUID receiptId) {
        return warehouseService.confirmReceipt(receiptId);
    }

    @GetMapping("/stocktakes")
    public List<StocktakeView> stocktakes() {
        return warehouseService.listStocktakes();
    }

    @PostMapping("/stocktakes")
    public StocktakeView createStocktake(@Valid @RequestBody CreateStocktakeRequest request) {
        return warehouseService.createStocktake(request);
    }

    @PostMapping("/stocktakes/{stocktakeId}/confirm")
    public StocktakeView confirmStocktake(@PathVariable UUID stocktakeId,
                                          @Valid @RequestBody ConfirmStocktakeRequest request) {
        return warehouseService.confirmStocktake(stocktakeId, request);
    }

    @GetMapping("/transfers")
    public List<TransferView> transfers() {
        return warehouseService.listTransfers();
    }

    @PostMapping("/transfers")
    public TransferView createTransfer(@Valid @RequestBody CreateTransferRequest request) {
        return warehouseService.createTransfer(request);
    }

    @PostMapping("/transfers/{transferId}/ship")
    public TransferView shipTransfer(@PathVariable UUID transferId) {
        return warehouseService.shipTransfer(transferId);
    }

    @PostMapping("/transfers/{transferId}/receive")
    public TransferView receiveTransfer(@PathVariable UUID transferId) {
        return warehouseService.receiveTransfer(transferId);
    }

    @GetMapping("/replenishment")
    public List<ReplenishmentSuggestion> replenishment(@RequestParam String locationType,
                                                       @RequestParam UUID locationId) {
        return warehouseService.replenishment(locationType, locationId);
    }
}
