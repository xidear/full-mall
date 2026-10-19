package com.fullmall.pos.app;

import com.fullmall.pos.domain.TenderType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

@Component
public class MockPosPaymentProvider implements PosPaymentProvider {

    public static final String PROVIDER = "mock";

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public PosPaymentIntent create(PosPaymentRequest request) {
        String tradeNo = "MOCK" + request.tenderType().name().charAt(0)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        PosPaymentResult result = new PosPaymentResult(tradeNo, true, "SUCCESS", "Mock 支付成功");
        return new PosPaymentIntent(tradeNo, "mock://pay/" + tradeNo, result, Instant.now().plus(Duration.ofMinutes(5)));
    }

    @Override
    public PosPaymentResult query(String providerTradeNo) {
        return new PosPaymentResult(providerTradeNo, true, "SUCCESS", "Mock 支付成功");
    }

    @Override
    public PosPaymentResult cancel(String providerTradeNo, String reason) {
        return new PosPaymentResult(providerTradeNo, true, "CANCELLED", reason);
    }

    @Override
    public PosRefundResult refund(PosRefundRequest request) {
        String refundNo = "MOCKREFUND" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return new PosRefundResult(refundNo, true, "SUCCESS", "Mock 退款成功");
    }
}
