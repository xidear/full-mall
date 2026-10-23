package com.fullmall.payment.wechat;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信支付 v3 退款：`POST /v3/refund/domestic/refunds`。
 *
 * <p>与下单同一套签名（SHA256withRSA）。退款必须带原 `out_trade_no`、退款单号、
 * 退款金额与**原订单总额**（微信要求 amount.total 与支付一致）。
 *
 * <p>失败语义：非 2xx 抛错并带微信错误体；成功但无 `refund_id` 也视为失败
 * （避免"以为退了"）。
 */
public class WechatRefundClient {

    private static final ObjectMapper MAPPER = com.fullmall.platform.json.Json.mapper();

    private final String merchantId;
    private final String serialNumber;
    private final PrivateKey privateKey;
    private final String baseUrl;
    private final String notifyUrl;
    private final HttpClient http = com.fullmall.platform.http.HttpClients.shared();

    public WechatRefundClient(String merchantId, String serialNumber, String privateKeyPem,
                              String apiV3Key, String baseUrl, String notifyUrl) {
        if (isBlank(merchantId) || isBlank(serialNumber) || isBlank(privateKeyPem)
                || isBlank(apiV3Key) || isBlank(baseUrl) || isBlank(notifyUrl)) {
            throw new IllegalStateException(
                    "微信退款需配置 merchant-id/serial-number/private-key/api-v3-key/base-url/notify-url");
        }
        if (apiV3Key.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("APIv3 密钥必须是 32 字节");
        }
        this.merchantId = merchantId;
        this.serialNumber = serialNumber;
        this.privateKey = WechatV3Signer.privateKeyFromPem(privateKeyPem);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.notifyUrl = notifyUrl;
    }

    public record Refund(String refundId, String status, String rawResponse) {
    }

    /**
     * @param outTradeNo  原商户订单号
     * @param outRefundNo 退款单号（同一单多次部分退款需唯一）
     * @param refundFen   本次退款金额（分）
     * @param totalFen    原订单总额（分）
     */
    public Refund refund(String outTradeNo, String outRefundNo, long refundFen, long totalFen,
                         String reason) {
        if (isBlank(outTradeNo) || isBlank(outRefundNo)) {
            throw new IllegalArgumentException("订单号与退款单号不能为空");
        }
        if (refundFen <= 0 || totalFen <= 0 || refundFen > totalFen) {
            throw new IllegalArgumentException("退款金额必须大于 0 且不超过原订单金额");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("out_trade_no", outTradeNo);
        payload.put("out_refund_no", outRefundNo);
        payload.put("reason", reason == null || reason.isBlank() ? "售后退款" : reason);
        payload.put("notify_url", notifyUrl);
        payload.put("amount", Map.of("refund", refundFen, "total", totalFen, "currency", "CNY"));
        String body;
        try {
            body = MAPPER.writeValueAsString(payload);
        } catch (Exception error) {
            throw new IllegalStateException("退款报文序列化失败", error);
        }
        String path = "/v3/refund/domestic/refunds";
        long ts = Instant.now().getEpochSecond();
        var signed = WechatV3Signer.sign("POST", path, body, merchantId, serialNumber,
                privateKey, ts, WechatV3Signer.randomNonce());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", signed.authorization())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("微信退款失败 HTTP " + response.statusCode()
                        + "：" + response.body());
            }
            var node = MAPPER.readTree(response.body());
            String refundId = node.path("refund_id").asText("");
            if (refundId.isBlank()) {
                throw new IllegalStateException("微信退款返回缺少 refund_id：" + response.body());
            }
            return new Refund(refundId, node.path("status").asText(""), response.body());
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("微信退款请求失败：" + error.getMessage(), error);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
