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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信支付 v3 商家转账（批量转账到零钱，`/v3/transfer/batches`）。
 *
 * <p>用于佣金提现打款：一笔提现对应一个批次、一条明细。
 * 与支付同一套签名；响应必须带 `batch_id`，否则视为失败（不能"以为打了款"）。
 *
 * <p>注意：真实环境还需商户开通"商家转账到零钱"权限、用户 openid 与实名信息；
 * 本类只保证**报文与签名正确、失败可见**。
 */
public class WechatTransferClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String appId;
    private final String merchantId;
    private final String serialNumber;
    private final PrivateKey privateKey;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public WechatTransferClient(String appId, String merchantId, String serialNumber,
                                String privateKeyPem, String apiV3Key, String baseUrl) {
        if (isBlank(appId) || isBlank(merchantId) || isBlank(serialNumber)
                || isBlank(privateKeyPem) || isBlank(apiV3Key) || isBlank(baseUrl)) {
            throw new IllegalStateException(
                    "商家转账需配置 app-id/merchant-id/serial-number/private-key/api-v3-key/base-url");
        }
        if (apiV3Key.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("APIv3 密钥必须是 32 字节");
        }
        this.appId = appId;
        this.merchantId = merchantId;
        this.serialNumber = serialNumber;
        this.privateKey = WechatV3Signer.privateKeyFromPem(privateKeyPem);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public record Transfer(String batchId, String status, String rawResponse) {
    }

    /**
     * @param outBatchNo 商户批次号（提现单号）
     * @param openId     收款人 openid
     * @param amountFen  打款金额（分）
     * @param remark     转账备注（用户可见）
     */
    public Transfer transfer(String outBatchNo, String openId, long amountFen, String remark) {
        if (isBlank(outBatchNo) || isBlank(openId)) {
            throw new IllegalArgumentException("批次号与 openid 不能为空");
        }
        if (amountFen <= 0) {
            throw new IllegalArgumentException("转账金额必须大于 0（分）");
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("openid", openId);
        detail.put("transfer_amount", amountFen);
        detail.put("transfer_remark", remark == null || remark.isBlank() ? "佣金提现" : remark);
        detail.put("out_detail_no", outBatchNo + "-1");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appid", appId);
        payload.put("out_batch_no", outBatchNo);
        payload.put("batch_name", "佣金提现");
        payload.put("batch_remark", remark == null || remark.isBlank() ? "佣金提现" : remark);
        payload.put("total_amount", amountFen);
        payload.put("total_num", 1);
        payload.put("transfer_detail_list", new ArrayList<>(List.of(detail)));
        String body;
        try {
            body = MAPPER.writeValueAsString(payload);
        } catch (Exception error) {
            throw new IllegalStateException("转账报文序列化失败", error);
        }
        String path = "/v3/transfer/batches";
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
                throw new IllegalStateException("商家转账失败 HTTP " + response.statusCode()
                        + "：" + response.body());
            }
            var node = MAPPER.readTree(response.body());
            String batchId = node.path("batch_id").asText("");
            if (batchId.isBlank()) {
                throw new IllegalStateException("商家转账返回缺少 batch_id：" + response.body());
            }
            return new Transfer(batchId, node.path("batch_status").asText(""), response.body());
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("商家转账请求失败：" + error.getMessage(), error);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
