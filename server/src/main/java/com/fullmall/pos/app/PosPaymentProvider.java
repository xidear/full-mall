package com.fullmall.pos.app;

import com.fullmall.pos.domain.TenderType;

import java.time.Instant;
import java.util.UUID;

/**
 * Third-party POS acquirer boundary. Business code only depends on this interface; vendor SDK DTOs
 * must never leak into the domain or API layer.
 */
public interface PosPaymentProvider {

    String provider();

    PosPaymentIntent create(PosPaymentRequest request);

    PosPaymentResult query(String providerTradeNo);

    PosPaymentResult cancel(String providerTradeNo, String reason);

    PosRefundResult refund(PosRefundRequest request);

    record PosPaymentRequest(
            UUID saleId,
            String saleNo,
            TenderType tenderType,
            long amount,
            String currency
    ) {
    }

    record PosPaymentIntent(
            String providerTradeNo,
            String qrContent,
            PosPaymentResult result,
            Instant expiresAt
    ) {
    }

    record PosPaymentResult(
            String providerTradeNo,
            boolean success,
            String status,
            String message
    ) {
    }

    record PosRefundRequest(
            String providerTradeNo,
            long amount,
            String currency,
            String reason
    ) {
    }

    record PosRefundResult(
            String refundNo,
            boolean success,
            String status,
            String message
    ) {
    }
}
