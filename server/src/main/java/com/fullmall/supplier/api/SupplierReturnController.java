package com.fullmall.supplier.api;

import com.fullmall.supplier.app.SupplierReturnService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * 供应商售后（商城 → 供应商）：仅运营后台使用。
 *
 * <p>供应商侧查看/申诉另行接入（供应商门户），与商城顾客售后完全独立。
 */
@RestController
@RequestMapping("/admin/v1/supplier-returns")
public class SupplierReturnController {

    private final SupplierReturnService returns;

    public SupplierReturnController(SupplierReturnService returns) {
        this.returns = returns;
    }

    public record CreateReturn(@NotNull UUID supplierId, @NotNull UUID orderId,
                               String reason, long amount, String currency) {
    }

    @PostMapping
    public Map<String, UUID> create(@RequestBody CreateReturn request, Authentication authentication) {
        String operator = authentication == null || authentication.getName() == null
                ? "staff:unknown" : "staff:" + authentication.getName();
        UUID id = returns.create(request.supplierId(), request.orderId(), request.reason(),
                request.amount(), request.currency(), operator);
        return Map.of("id", id);
    }

    public record Transition(String status, String note) {
    }

    @PostMapping("/{id}/status")
    public Map<String, Boolean> transition(@PathVariable UUID id, @RequestBody Transition request) {
        returns.transition(id, request.status(), request.note());
        return Map.of("updated", true);
    }

    /** Keyset 分页（深页推荐）。 */
    @GetMapping("/keyset")
    public Map<String, Object> keyset(@RequestParam(required = false) UUID supplierId,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String cursorCreatedAt,
                                      @RequestParam(required = false) UUID cursorId,
                                      @RequestParam(defaultValue = "20") int size) {
        return returns.listKeyset(supplierId, status, cursorCreatedAt, cursorId, size);
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) UUID supplierId,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return returns.list(supplierId, status, page, size);
    }
}
