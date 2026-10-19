package com.fullmall.aftersale.api;

import com.fullmall.aftersale.api.AfterSaleDtos.AfterSaleView;
import com.fullmall.aftersale.api.AfterSaleDtos.CreateAfterSaleRequest;
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
@RequestMapping("/api/v1/orders/{orderId}/after-sales")
public class CustomerAfterSaleController {

    private final AfterSaleService afterSaleService;

    public CustomerAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @PostMapping
    public AfterSaleView create(@PathVariable UUID orderId,
                                @Valid @RequestBody CreateAfterSaleRequest request) {
        return afterSaleService.create(orderId, request);
    }

    @GetMapping
    public List<AfterSaleView> list(@PathVariable UUID orderId, @RequestParam String phone) {
        return afterSaleService.listByOrder(orderId, phone);
    }
}
