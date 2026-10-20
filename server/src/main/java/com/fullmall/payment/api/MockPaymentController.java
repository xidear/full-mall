package com.fullmall.payment.api;

import com.fullmall.payment.app.PaymentService;
import com.fullmall.payment.domain.Payment;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/mock")
// 生产环境不注册该接口：mock 支付会写"已支付"状态，绝不能出现在真实环境
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
        "!'prod'.equalsIgnoreCase('${app.mode:dev}')")
public class MockPaymentController {

    private final PaymentService paymentService;

    public MockPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{orderId}/pay")
    public Payment pay(@PathVariable UUID orderId) {
        return paymentService.payMock(orderId);
    }
}