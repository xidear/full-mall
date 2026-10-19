package com.fullmall.payment.app;

import com.fullmall.order.app.OrderService;
import com.fullmall.order.domain.Order;
import com.fullmall.order.domain.OrderStatus;
import com.fullmall.promotion.domain.GroupBuyFailedEvent;
import com.fullmall.promotion.app.PromotionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GroupBuyRefundListener {

    private static final Logger log = LoggerFactory.getLogger(GroupBuyRefundListener.class);

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PromotionService promotionService;

    public GroupBuyRefundListener(OrderService orderService, PaymentService paymentService,
                                  PromotionService promotionService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.promotionService = promotionService;
    }

    @EventListener
    public void onGroupBuyFailed(GroupBuyFailedEvent event) {
        for (UUID orderId : event.paidOrderIds()) {
            try {
                refund(orderId);
            } catch (RuntimeException exception) {
                log.error("拼团 {} 订单 {} 退款失败，将在下一轮重试", event.groupBuyId(), orderId, exception);
            }
        }
    }

    private void refund(UUID orderId) {
        Order order = orderService.getOrder(orderId);
        if (order.status() == OrderStatus.PAID) {
            paymentService.refund(orderId, order.total().amount());
            orderService.markAfterSaleRefunded(orderId, true);
            promotionService.markMemberRefunded(orderId);
            return;
        }
        if (order.status() == OrderStatus.REFUNDED || order.status() == OrderStatus.PARTIALLY_REFUNDED) {
            promotionService.markMemberRefunded(orderId);
            return;
        }
        throw new IllegalStateException("拼团失败订单状态为 " + order.status() + "，无法退款：" + orderId);
    }
}
