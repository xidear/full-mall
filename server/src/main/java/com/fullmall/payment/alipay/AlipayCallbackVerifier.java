package com.fullmall.payment.alipay;

import java.security.PublicKey;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付宝异步通知（notify）验签。
 *
 * <p>验签内容：**除 sign 与 sign_type 外的全部参数**按字典序拼接。
 * 验签失败 → 必须拒绝；成功后再校验 `app_id`、`trade_status`、`total_amount` 与本地订单一致。
 */
public class AlipayCallbackVerifier {

    private final PublicKey alipayPublicKey;
    private final String expectedAppId;

    public AlipayCallbackVerifier(String alipayPublicKeyPem, String expectedAppId) {
        if (alipayPublicKeyPem == null || alipayPublicKeyPem.isBlank()) {
            throw new IllegalStateException("支付宝公钥未配置，无法验签回调");
        }
        if (expectedAppId == null || expectedAppId.isBlank()) {
            throw new IllegalStateException("app-id 未配置，无法校验回调来源");
        }
        this.alipayPublicKey = AlipaySigner.publicKeyFromPem(alipayPublicKeyPem);
        this.expectedAppId = expectedAppId;
    }

    public record Verdict(boolean valid, String reason, String outTradeNo, String tradeStatus,
                          long totalFen) {
    }

    public Verdict verify(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return new Verdict(false, "回调参数为空", null, null, 0);
        }
        Map<String, String> copy = new LinkedHashMap<>(params);
        String sign = copy.remove("sign");
        copy.remove("sign_type");
        if (sign == null || sign.isBlank()) {
            return new Verdict(false, "缺少签名", null, null, 0);
        }
        if (!AlipaySigner.verify(AlipaySigner.signContent(copy), sign, alipayPublicKey)) {
            return new Verdict(false, "验签失败", null, null, 0);
        }
        if (!expectedAppId.equals(params.getOrDefault("app_id", ""))) {
            return new Verdict(false, "app_id 不匹配", null, null, 0);
        }
        String tradeStatus = params.getOrDefault("trade_status", "");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return new Verdict(false, "交易状态非成功：" + tradeStatus, null, tradeStatus, 0);
        }
        long fen;
        try {
            fen = new java.math.BigDecimal(params.getOrDefault("total_amount", "0"))
                    .movePointRight(2).longValueExact();
        } catch (RuntimeException error) {
            return new Verdict(false, "金额格式非法", null, tradeStatus, 0);
        }
        return new Verdict(true, "ok", params.get("out_trade_no"), tradeStatus, fen);
    }
}
