package com.fullmall.payment.app;

import com.fullmall.order.domain.Order;
import com.fullmall.payment.domain.Payment;

public interface PaymentConnector {

    String provider();

    Payment createPayment(Order order);

    Payment confirm(Payment payment);

    Payment refund(Payment payment, long amount);
}