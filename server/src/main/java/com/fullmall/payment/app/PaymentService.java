package com.fullmall.payment.app;

import com.fullmall.analytics.app.AnalyticsService;
import com.fullmall.order.app.OrderService;
import com.fullmall.order.domain.Order;
import com.fullmall.order.domain.OrderStatus;
import com.fullmall.payment.domain.Payment;
import com.fullmall.payment.domain.PaymentStatus;
import com.fullmall.payment.infra.PaymentRepository;
import com.fullmall.platform.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final PaymentConnector mockPaymentConnector;
    private final AnalyticsService analyticsService;
    private final String configuredProvider;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderService orderService,
                          PaymentConnector mockPaymentConnector,
                          AnalyticsService analyticsService,
                          @Value("${app.payment.provider:mock}") String configuredProvider) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.mockPaymentConnector = mockPaymentConnector;
        this.analyticsService = analyticsService;
        this.configuredProvider = configuredProvider;
    }

    @Transactional
    public Payment payMock(UUID orderId) {
        if (!"mock".equalsIgnoreCase(configuredProvider)) {
            throw ApiException.badRequest("PAYMENT_PROVIDER_NOT_MOCK",
                    "Mock payment endpoint is disabled because payment.provider=" + configuredProvider);
        }
        Order order = orderService.getOrder(orderId);

        Payment existing = paymentRepository.findByOrderIdAndProvider(orderId, "MOCK");
        if (existing != null && existing.status() == PaymentStatus.PAID) {
            return existing;
        }
        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "Order is not payable in status " + order.status());
        }

        Payment payment = existing == null ? mockPaymentConnector.createPayment(order) : existing;
        if (existing == null) {
            paymentRepository.insert(payment);
        }
        Payment paid = mockPaymentConnector.confirm(payment);
        paymentRepository.update(paid);
        paymentRepository.insertEvent(paid.id(), "PAYMENT_PAID", "{\"provider\":\"MOCK\"}");
        orderService.markPaid(orderId);
        analyticsService.record("payment_succeeded", orderId, null, Map.of(
                "provider", paid.provider(),
                "amount", paid.amount().amount(),
                "currency", paid.amount().currency()));
        return paid;
    }
}