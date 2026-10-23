package com.fullmall.payment.callback;

import com.fullmall.order.app.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 微信支付 v3 回调入口。
 *
 * <p>顺序严格：**先验签 → 再解密 → 校验状态 → 幂等入账**。
 * 任一步失败就拒绝，绝不"先记成功再补验签"。
 *
 * <p>注意：金额以回调里的 `amount.total` 为准，并与本地订单金额比对；
 * 不一致时不入账，返回失败让微信重试（人工介入核查）。
 */
@RestController
@RequestMapping("/api/v1/payments/wechat")
public class PaymentCallbackController {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentCallbackController.class);

    private final WechatV3CallbackVerifier verifier;
    private final OrderService orders;

    public PaymentCallbackController(WechatV3CallbackVerifier verifier, OrderService orders) {
        this.verifier = verifier;
        this.orders = orders;
    }

    @PostMapping("/notify")
    public ResponseEntity<Map<String, String>> notify(
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestBody(required = false) String body) {
        var headers = new WechatV3CallbackVerifier.Headers(timestamp, nonce, signature);
        if (!verifier.verify(headers, body, Instant.now().getEpochSecond())) {
            LOG.warn("微信回调验签失败 timestamp={} nonce={}", timestamp, nonce);
            return ResponseEntity.status(401).body(Map.of("code", "SIGNATURE_INVALID"));
        }
        Map<String, Object> transaction;
        try {
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).path("resource");
            var decrypted = verifier.decrypt(node.path("ciphertext").asText(),
                    node.path("nonce").asText(), node.path("associated_data").asText(""));
            transaction = verifier.parseTransaction(decrypted.plaintextJson());
        } catch (Exception error) {
            LOG.error("微信回调解密/解析失败：{}", error.getMessage());
            return ResponseEntity.status(400).body(Map.of("code", "DECRYPT_FAILED"));
        }
        String tradeState = String.valueOf(transaction.get("tradeState"));
        String orderNo = String.valueOf(transaction.get("outTradeNo"));
        if (!"SUCCESS".equals(tradeState)) {
            // 非成功状态：确认收到，不落账（后续状态由微信继续通知）
            LOG.info("微信回调非成功状态 order={} state={}", orderNo, tradeState);
            return ResponseEntity.ok(Map.of("code", "SUCCESS"));
        }
        try {
            boolean marked = orders.markPaidByOrderNo(orderNo, (Long) transaction.get("totalFen"));
            return ResponseEntity.ok(Map.of("code", marked ? "SUCCESS" : "ALREADY_PAID"));
        } catch (RuntimeException error) {
            LOG.error("微信回调入账失败 order={} 原因={}", orderNo, error.getMessage());
            return ResponseEntity.status(500).body(Map.of("code", "BOOKKEEPING_FAILED"));
        }
    }
}
