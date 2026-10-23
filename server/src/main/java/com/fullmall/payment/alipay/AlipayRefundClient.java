package com.fullmall.payment.alipay;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付宝退款（`alipay.trade.refund`）。
 *
 * <p>与下单同套 RSA2：请求签名 + **响应验签**。
 * 业务码 `10000` 才算受理；`fund_change=Y` 表示资金已实际退回。
 */
public class AlipayRefundClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String appId;
    private final PrivateKey appPrivateKey;
    private final PublicKey alipayPublicKey;
    private final String gateway;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public AlipayRefundClient(String appId, String appPrivateKeyPem, String alipayPublicKeyPem,
                              String gateway) {
        if (isBlank(appId) || isBlank(appPrivateKeyPem) || isBlank(alipayPublicKeyPem)
                || isBlank(gateway)) {
            throw new IllegalStateException(
                    "支付宝退款需配置 app-id/app-private-key/alipay-public-key/gateway");
        }
        this.appId = appId;
        this.appPrivateKey = AlipaySigner.privateKeyFromPem(appPrivateKeyPem);
        this.alipayPublicKey = AlipaySigner.publicKeyFromPem(alipayPublicKeyPem);
        this.gateway = gateway;
    }

    public record Refund(String tradeNo, String fundChange, String rawResponse) {
    }

    public Refund refund(String outTradeNo, String outRequestNo, long refundFen, String reason) {
        if (isBlank(outTradeNo) || isBlank(outRequestNo)) {
            throw new IllegalArgumentException("订单号与退款请求号不能为空");
        }
        if (refundFen <= 0) {
            throw new IllegalArgumentException("退款金额必须大于 0（分）");
        }
        String bizContent;
        try {
            bizContent = MAPPER.writeValueAsString(Map.of(
                    "out_trade_no", outTradeNo,
                    "refund_amount", String.format("%.2f", refundFen / 100.0),
                    "out_request_no", outRequestNo,
                    "refund_reason", reason == null || reason.isBlank() ? "售后退款" : reason));
        } catch (Exception error) {
            throw new IllegalStateException("退款 biz_content 序列化失败", error);
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", appId);
        params.put("method", "alipay.trade.refund");
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", ZonedDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("biz_content", bizContent);
        params.put("sign", AlipaySigner.sign(params, appPrivateKey));

        String body = params.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(gateway))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("支付宝退款 HTTP " + response.statusCode());
            }
            var root = MAPPER.readTree(response.body());
            String nodeName = "alipay_trade_refund_response";
            var node = root.path(nodeName);
            String sign = root.path("sign").asText("");
            if (sign.isBlank() || !AlipaySigner.verify(node.toString(), sign, alipayPublicKey)) {
                throw new IllegalStateException("支付宝退款响应验签失败，拒绝采信");
            }
            if (!"10000".equals(node.path("code").asText(""))) {
                throw new IllegalStateException("支付宝退款失败 " + node.path("code").asText("")
                        + "：" + node.path("sub_code").asText("") + " "
                        + node.path("sub_msg").asText(""));
            }
            return new Refund(node.path("trade_no").asText(""),
                    node.path("fund_change").asText("N"), response.body());
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("支付宝退款请求失败：" + error.getMessage(), error);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
