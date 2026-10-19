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
    private final com.fullmall.distribution.app.CommissionLedgerService commissionLedger;
    private final com.fullmall.distribution.app.ReferralService referralService;
    private final com.fullmall.member.app.MemberRewardService memberRewards;
    private final com.fullmall.member.app.MemberPointsService memberPoints;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderService orderService,
                          PaymentConnector mockPaymentConnector,
                          AnalyticsService analyticsService,
                          @Value("${app.payment.provider:mock}") String configuredProvider,
                          com.fullmall.distribution.app.CommissionLedgerService commissionLedger,
                          com.fullmall.distribution.app.ReferralService referralService,
                          com.fullmall.member.app.MemberRewardService memberRewards,
                          com.fullmall.member.app.MemberPointsService memberPoints) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.mockPaymentConnector = mockPaymentConnector;
        this.analyticsService = analyticsService;
        this.configuredProvider = configuredProvider;
        this.commissionLedger = commissionLedger;
        this.referralService = referralService;
        this.memberRewards = memberRewards;
        this.memberPoints = memberPoints;
    }

    /**
     * 退款对佣金的处理（分销金融正确性）：
     * 整单退款 → 佣金回冲；部分退款 → 佣金延长冻结（比例回冲尚未实现，绝不提前解冻）。
     */
    private void applyCommissionPolicy(UUID orderId, long refundedAmount) {
        var order = orderService.getOrder(orderId);
        long orderTotal = order.total().amount();
        if (refundedAmount >= orderTotal) {
            commissionLedger.reverseByOrder(orderId, "订单整单退款");
            // 会员侧：退回抵扣的积分 + 回退本单赚取的积分/成长值（均幂等）
            var orderForMember = orderService.getOrder(orderId);
            memberPoints.rollbackForOrder(orderId, orderForMember.contactPhone());
            memberRewards.rollback(orderId, orderForMember.contactPhone());
        } else {
            commissionLedger.extendFreezeByOrder(orderId, java.time.Duration.ofDays(7), "部分退款，延长冻结");
        }
    }

    @Transactional
    public Payment refund(UUID orderId, long amount) {
        if (!"mock".equalsIgnoreCase(configuredProvider)) {
            throw ApiException.badRequest("PAYMENT_PROVIDER_NOT_MOCK",
                    "当前开源构建只支持 Mock 退款；真实适配器需在正式构建中配置");
        }
        Payment payment = paymentRepository.findByOrderIdAndProvider(orderId, "MOCK");
        if (payment == null || (payment.status() != PaymentStatus.PAID
                && payment.status() != PaymentStatus.PARTIALLY_REFUNDED)) {
            throw ApiException.conflict("PAYMENT_NOT_REFUNDABLE", "订单没有可退款的支付记录");
        }
        Payment refunded = mockPaymentConnector.refund(payment, amount);
        paymentRepository.update(refunded);
        paymentRepository.insertEvent(payment.id(), "PAYMENT_REFUNDED",
                "{\"amount\":" + amount + "}");
        applyCommissionPolicy(orderId, amount);
        analyticsService.record("payment_refunded", orderId, null, Map.of(
                "provider", refunded.provider(),
                "amount", amount,
                "currency", refunded.amount().currency()));
        return refunded;
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
        // 佣金：支付成功后按实付生成（写入即为 FROZEN，冻结期满才可提现）
        referralService.settleOrder(orderId, paid.amount().amount(), paid.amount().currency());
        // 会员积分与成长值（MEM-002）：匿名订单不产生
        memberRewards.accrue(orderId, order.contactPhone(), paid.amount().amount());
        analyticsService.record("payment_succeeded", orderId, null, Map.of(
                "provider", paid.provider(),
                "amount", paid.amount().amount(),
                "currency", paid.amount().currency()));
        return paid;
    }
}