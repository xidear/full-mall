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
 * 微信支付 v3 Native 下单（扫码支付）。
 *
 * <p>与回调验签配套：下单拿到 `code_url` 生成二维码，用户支付后回调入账。
 * 缺配置构造即失败；非 2xx 抛错并带微信返回的错误体，绝不返回空字符串当成功。
 */
public class WechatNativeOrderClient {

    private static final ObjectMapper MAPPER = com.fullmall.platform.json.Json.mapper();

    private final String appId;
    private final String merchantId;
    private final String serialNumber;
    private final String apiV3Key;
    private final PrivateKey privateKey;
    private final String baseUrl;
    private final String notifyUrl;
    private final HttpClient http = com.fullmall.platform.http.HttpClients.shared();

    public WechatNativeOrderClient(String appId, String merchantId, String serialNumber,
                                   String privateKeyPem, String apiV3Key,
                                   String baseUrl, String notifyUrl) {
        if (isBlank(appId) || isBlank(merchantId) || isBlank(serialNumber) || isBlank(privateKeyPem)
                || isBlank(apiV3Key) || isBlank(baseUrl) || isBlank(notifyUrl)) {
            throw new IllegalStateException(
                    "微信支付 Native 下单需配置 app-id/merchant-id/serial-number/private-key/api-v3-key/base-url/notify-url");
        }
        if (apiV3Key.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("APIv3 密钥必须是 32 字节");
        }
        this.appId = appId;
        this.merchantId = merchantId;
        this.serialNumber = serialNumber;
        this.privateKey = WechatV3Signer.privateKeyFromPem(privateKeyPem);
        this.apiV3Key = apiV3Key;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.notifyUrl = notifyUrl;
    }

    public record NativeOrder(String codeUrl, String rawResponse) {
    }

    /** 下单：金额单位分；description 会展示在用户支付页。 */
    public NativeOrder create(String outTradeNo, long amountFen, String description) {
        if (outTradeNo == null || outTradeNo.isBlank()) {
            throw new IllegalArgumentException("商户订单号不能为空");
        }
        if (amountFen <= 0) {
            throw new IllegalArgumentException("金额必须大于 0（分）");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appid", appId);
        payload.put("mchid", merchantId);
        payload.put("description", description == null || description.isBlank() ? "商品订单" : description);
        payload.put("out_trade_no", outTradeNo);
        payload.put("notify_url", notifyUrl);
        payload.put("amount", Map.of("total", amountFen, "currency", "CNY"));
        String body;
        try {
            body = MAPPER.writeValueAsString(payload);
        } catch (Exception error) {
            throw new IllegalStateException("下单报文序列化失败", error);
        }
        String path = "/v3/pay/transactions/native";
        long ts = Instant.now().getEpochSecond();
        String nonce = WechatV3Signer.randomNonce();
        var signed = WechatV3Signer.sign("POST", path, body, merchantId, serialNumber,
                privateKey, ts, nonce);
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
                throw new IllegalStateException("微信下单失败 HTTP " + response.statusCode()
                        + "：" + response.body());
            }
            String codeUrl = MAPPER.readTree(response.body()).path("code_url").asText("");
            if (codeUrl.isBlank()) {
                throw new IllegalStateException("微信下单返回缺少 code_url：" + response.body());
            }
            return new NativeOrder(codeUrl, response.body());
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("微信下单请求失败：" + error.getMessage(), error);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
