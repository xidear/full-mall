package com.fullmall.payment.api;

import com.fullmall.payment.app.PaymentService;
import com.fullmall.payment.domain.Payment;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 后台退款入口（部分/全额）。
 *
 * <p>为什么放在 payment 模块而不是 order：order 已被 payment 依赖，反向依赖会构成
 * 模块循环（Modulith 结构测试会直接失败）。
 *
 * <p>按行分摊由 `PaymentService.refund` 内部完成，这里不重复记录。
 */
@RestController
public class AdminRefundController {

    private final PaymentService payments;
    private final AuditEventService auditEvents;

    public AdminRefundController(PaymentService payments, AuditEventService auditEvents) {
        this.payments = payments;
        this.auditEvents = auditEvents;
    }

    public record RefundRequest(long amount, String reason) {
    }

    @PostMapping("/admin/v1/orders/{orderId}/refunds")
    public Payment refund(@PathVariable UUID orderId, @RequestBody RefundRequest request,
                          Authentication authentication) {
        if (request.amount() <= 0) {
            throw ApiException.badRequest("INVALID_REFUND_AMOUNT", "退款金额必须大于 0");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw ApiException.badRequest("REFUND_REASON_REQUIRED", "退款必须填写原因");
        }
        String actor = authentication == null || authentication.getName() == null
                ? "staff:unknown" : "staff:" + authentication.getName();
        auditEvents.record(new AuditEventService.AuditEvent(
                "ORDER_REFUND_REQUESTED", actor, "order", orderId.toString(),
                "金额 " + request.amount() + "，原因 " + request.reason().trim(), null));
        return payments.refund(orderId, request.amount());
    }
}
