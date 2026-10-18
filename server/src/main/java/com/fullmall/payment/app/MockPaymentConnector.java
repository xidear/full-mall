package com.fullmall.payment.app;

import com.fullmall.order.domain.Order;
import com.fullmall.payment.domain.Payment;
import com.fullmall.payment.domain.PaymentStatus;
import com.fullmall.platform.util.Ids;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MockPaymentConnector implements PaymentConnector {

    @Override
    public String provider() {
        return "MOCK";
    }

    @Override
    public Payment createPayment(Order order) {
        return new Payment(
                Ids.newId(),
                order.id(),
                provider(),
                PaymentStatus.PENDING,
                order.total(),
                null,
                null,
                Instant.now());
    }

    @Override
    public Payment confirm(Payment payment) {
        if (payment.status() == PaymentStatus.PAID) {
            return payment;
        }
        return new Payment(
                payment.id(),
                payment.orderId(),
                payment.provider(),
                PaymentStatus.PAID,
                payment.amount(),
                "MOCK-" + payment.id(),
                Instant.now(),
                payment.createdAt());
    }

    @Override
    public Payment refund(Payment payment, long amount) {
        if (amount <= 0 || amount > payment.amount().amount()) {
            throw new IllegalArgumentException("Refund amount must be between 1 and " + payment.amount().amount());
        }
        PaymentStatus status = amount == payment.amount().amount()
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
        return new Payment(
                payment.id(),
                payment.orderId(),
                payment.provider(),
                status,
                payment.amount(),
                payment.providerTransactionId(),
                payment.paidAt(),
                payment.createdAt());
    }
}