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
 * 支付宝当面付预下单（`alipay.trade.precreate`）。
 *
 * <p>流程：组装公共参数 + biz_content → RSA2 签名 → 表单 POST 网关 → **验证响应签名** → 取 `qr_code`。
 *
 * <p>安全：响应签名不通过**一律拒绝**（否则无法确认响应的确来自支付宝）。
 * 业务码非 `10000` 也当失败，并带 sub_code/sub_msg。
 */
public class AlipayPrecreateClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String appId;
    private final PrivateKey appPrivateKey;
    private final PublicKey alipayPublicKey;
    private final String gateway;
    private final String notifyUrl;
    private final HttpClient http = com.fullmall.platform.http.HttpClients.shared();

    public AlipayPrecreateClient(String appId, String appPrivateKeyPem, String alipayPublicKeyPem,
                                 String gateway, String notifyUrl) {
        if (isBlank(appId) || isBlank(appPrivateKeyPem) || isBlank(alipayPublicKeyPem)
                || isBlank(gateway) || isBlank(notifyUrl)) {
            throw new IllegalStateException(
                    "支付宝预下单需配置 app-id/app-private-key/alipay-public-key/gateway/notify-url");
        }
        this.appId = appId;
        this.appPrivateKey = AlipaySigner.privateKeyFromPem(appPrivateKeyPem);
        this.alipayPublicKey = AlipaySigner.publicKeyFromPem(alipayPublicKeyPem);
        this.gateway = gateway;
        this.notifyUrl = notifyUrl;
    }

    public record Precreate(String qrCode, String rawResponse) {
    }

    public Precreate create(String outTradeNo, long amountFen, String subject) {
        if (isBlank(outTradeNo)) {
            throw new IllegalArgumentException("商户订单号不能为空");
        }
        if (amountFen <= 0) {
            throw new IllegalArgumentException("金额必须大于 0（分）");
        }
        String bizContent;
        try {
            bizContent = MAPPER.writeValueAsString(Map.of(
                    "out_trade_no", outTradeNo,
                    "total_amount", com.fullmall.platform.money.Money.toYuan(amountFen),
                    "subject", subject == null || subject.isBlank() ? "商品订单" : subject,
                    "timeout_express", "30m"));
        } catch (Exception error) {
            throw new IllegalStateException("biz_content 序列化失败", error);
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", appId);
        params.put("method", "alipay.trade.precreate");
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", ZonedDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("notify_url", notifyUrl);
        params.put("biz_content", bizContent);
        String sign = AlipaySigner.sign(params, appPrivateKey);
        params.put("sign", sign);

        String body = params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
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
                throw new IllegalStateException("支付宝预下单 HTTP " + response.statusCode());
            }
            var root = MAPPER.readTree(response.body());
            String nodeName = "alipay_trade_precreate_response";
            var node = root.path(nodeName);
            String responseSign = root.path("sign").asText("");
            if (responseSign.isBlank()) {
                throw new IllegalStateException("支付宝响应缺少签名，拒绝采信：" + response.body());
            }
            if (!AlipaySigner.verify(node.toString(), responseSign, alipayPublicKey)) {
                throw new IllegalStateException("支付宝响应验签失败，拒绝采信");
            }
            String code = node.path("code").asText("");
            if (!"10000".equals(code)) {
                throw new IllegalStateException("支付宝预下单失败 " + code + "："
                        + node.path("sub_code").asText("") + " " + node.path("sub_msg").asText(""));
            }
            String qrCode = node.path("qr_code").asText("");
            if (qrCode.isBlank()) {
                throw new IllegalStateException("支付宝预下单未返回 qr_code");
            }
            return new Precreate(qrCode, response.body());
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("支付宝预下单请求失败：" + error.getMessage(), error);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
