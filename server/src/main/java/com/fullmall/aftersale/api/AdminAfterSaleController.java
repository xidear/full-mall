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

    /** 换货发货：记录承运商与运单号并通知顾客。 */
    @PostMapping("/{afterSaleId}/exchange-ship")
    public AfterSaleDtos.AfterSaleView shipExchange(
            @PathVariable java.util.UUID afterSaleId,
            @org.springframework.web.bind.annotation.RequestBody AfterSaleDtos.ExchangeShipRequest request,
            org.springframework.security.core.Authentication authentication) {
        var view = afterSaleService.shipExchange(afterSaleId, request.carrierCode(), request.trackingNo());
        return view;
    }

    /** 换货完成（顾客确认收到后由门店/客服标记）。 */
    @PostMapping("/{afterSaleId}/complete-exchange")
    public AfterSaleDtos.AfterSaleView completeExchange(
            @PathVariable java.util.UUID afterSaleId,
            org.springframework.security.core.Authentication authentication) {
        return afterSaleService.completeExchange(afterSaleId, authentication.getName());
    }

    @PostMapping("/{afterSaleId}/refund")
    public AfterSaleView refund(@PathVariable UUID afterSaleId) {
        return afterSaleService.refund(afterSaleId, "运营后台");
    }
}
