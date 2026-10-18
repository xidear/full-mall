package com.fullmall.inventory.api;

import com.fullmall.inventory.app.InventoryService;
import com.fullmall.inventory.domain.InventoryBatch;
import com.fullmall.inventory.domain.LocationType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/inventory/batches")
public class InventoryAdminController {

    private final InventoryService inventoryService;

    public InventoryAdminController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryBatch> list(
            @RequestParam(required = false) LocationType locationType,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID skuId,
            @RequestParam(required = false) Integer expiringInDays) {
        return inventoryService.findBatches(locationType, locationId, skuId, expiringInDays);
    }
}