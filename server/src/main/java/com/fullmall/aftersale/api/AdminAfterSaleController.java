package com.fullmall.aftersale.api;

import com.fullmall.aftersale.api.AfterSaleDtos.ApproveAfterSaleRequest;
import com.fullmall.aftersale.api.AfterSaleDtos.AfterSaleView;
import com.fullmall.aftersale.api.AfterSaleDtos.ReceiveAfterSaleRequest;
import com.fullmall.aftersale.api.AfterSaleDtos.RejectAfterSaleRequest;
import com.fullmall.aftersale.app.AfterSaleService;
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
@RequestMapping("/admin/v1/after-sales")
public class AdminAfterSaleController {

    private final AfterSaleService afterSaleService;

    public AdminAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @GetMapping
    public List<AfterSaleView> list(@RequestParam(required = false) String status) {
        return afterSaleService.list(status);
    }

    @PostMapping("/{afterSaleId}/approve")
    public AfterSaleView approve(@PathVariable UUID afterSaleId,
                                 @Valid @RequestBody ApproveAfterSaleRequest request) {
        return afterSaleService.approve(afterSaleId, request, "运营后台");
    }

    @PostMapping("/{afterSaleId}/reject")
    public AfterSaleView reject(@PathVariable UUID afterSaleId,
                                @Valid @RequestBody RejectAfterSaleRequest request) {
        return afterSaleService.reject(afterSaleId, request, "运营后台");
    }

    @PostMapping("/{afterSaleId}/receive")
    public AfterSaleView receive(@PathVariable UUID afterSaleId,
                                 @Valid @RequestBody ReceiveAfterSaleRequest request) {
        return afterSaleService.receive(afterSaleId, request, "运营后台");
    }

    @PostMapping("/{afterSaleId}/refund")
    public AfterSaleView refund(@PathVariable UUID afterSaleId) {
        return afterSaleService.refund(afterSaleId, "运营后台");
    }
}
