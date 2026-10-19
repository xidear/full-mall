package com.fullmall.order.jobs;

import com.fullmall.order.app.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final OrderService orderService;
    private final long expiryMinutes;

    public OrderExpiryJob(OrderService orderService,
                          @Value("${app.reservation.expiry-minutes:30}") long expiryMinutes) {
        this.orderService = orderService;
        this.expiryMinutes = expiryMinutes;
    }

    @Scheduled(initialDelayString = "${app.order.expiry-initial-delay-ms:60000}",
            fixedDelayString = "${app.order.expiry-interval-ms:60000}")
    public void cancelExpiredOrders() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(expiryMinutes));
        for (UUID orderId : orderService.findStalePendingOrderIds(cutoff, 200)) {
            boolean cancelled = orderService.cancelUnpaidOrder(orderId);
            if (cancelled) {
                log.info("超时未支付订单已取消并释放库存/促销名额 orderId={}", orderId);
            }
        }
    }
}
