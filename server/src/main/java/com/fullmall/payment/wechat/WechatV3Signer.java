package com.fullmall.payment.wechat;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 微信支付 v3 请求签名：`HTTP方法\nURL路径\n时间戳\n随机串\n请求体\n` + `SHA256withRSA`（商户私钥）。
 */
public final class WechatV3Signer {

    private WechatV3Signer() {
    }

    public record Signed(String authorization, String timestamp, String nonce) {
    }

    public static Signed sign(String method, String path, String body,
                              String merchantId, String serialNumber,
                              PrivateKey privateKey, long timestamp, String nonce) {
        String message = method.toUpperCase(java.util.Locale.ROOT) + "\n" + path + "\n"
                + timestamp + "\n" + nonce + "\n" + (body == null ? "" : body) + "\n";
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            String value = Base64.getEncoder().encodeToString(signature.sign());
            return new Signed("WECHATPAY2-SHA256-RSA2048 "
                    + "mchid=\"" + merchantId + "\","
                    + "nonce_str=\"" + nonce + "\","
                    + "signature=\"" + value + "\","
                    + "timestamp=\"" + timestamp + "\","
                    + "serial_no=\"" + serialNumber + "\"",
                    String.valueOf(timestamp), nonce);
        } catch (Exception error) {
            throw new IllegalStateException("微信支付请求签名失败", error);
        }
    }

    public static PrivateKey privateKeyFromPem(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("商户私钥为空");
        }
        try {
            String normalized = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(normalized)));
        } catch (Exception error) {
            throw new IllegalStateException("商户私钥解析失败（需 PKCS#8）", error);
        }
    }

    public static String randomNonce() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes).toUpperCase(java.util.Locale.ROOT);
    }
}
