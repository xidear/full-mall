package com.fullmall.purchase.api;

import org.springframework.web.bind.annotation.RequestParam;
import com.fullmall.purchase.app.PurchaseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 采购与收货（后台）。收货是成本产生的唯一入口。 */
@RestController
@RequestMapping("/admin/v1/purchases")
public class AdminPurchaseController {

    private final PurchaseService purchases;

    public AdminPurchaseController(PurchaseService purchases) {
        this.purchases = purchases;
    }

    public record CreateRequest(UUID supplierId, String currency, List<PurchaseService.Line> lines) {
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, UUID> create(@RequestBody CreateRequest body) {
        UUID id = purchases.create(body.supplierId(),
                body.currency() == null || body.currency().isBlank() ? "CNY" : body.currency(),
                body.lines());
        return Map.of("purchaseId", id);
    }

    @PostMapping("/{purchaseId}/receive")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public PurchaseService.ReceiptResult receive(
            @PathVariable UUID purchaseId,
            @RequestParam String locationType,
            @RequestParam UUID locationId) {
        return purchases.receive(purchaseId, locationType, locationId);
    }
}
