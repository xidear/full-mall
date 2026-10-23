package com.fullmall.payment.callback;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * 微信支付 v3 回调校验：**验签 + 解密 + 防重放**。
 *
 * <p>验签报文 = `timestamp\nnonce\nbody\n`，算法 `SHA256withRSA`，签名用平台证书私钥对应的公钥校验。
 * 解密用 APIv3 密钥做 **AES-256-GCM**（nonce + associated_data + ciphertext）。
 *
 * <p>红线：签名不对、时间戳超出窗口、解密失败 → **一律拒绝**，绝不"先入账再验"。
 */
@Component
public class WechatV3CallbackVerifier {

    private final String publicKeyBase64;
    private final String apiV3Key;
    private final long maxSkewSeconds;

    public WechatV3CallbackVerifier(
            @Value("${app.payment.wechat.platform-public-key:}") String publicKeyBase64,
            @Value("${app.payment.wechat.api-v3-key:}") String apiV3Key,
            @Value("${app.payment.wechat.callback-max-skew-seconds:300}") long maxSkewSeconds) {
        this.publicKeyBase64 = publicKeyBase64;
        this.apiV3Key = apiV3Key;
        this.maxSkewSeconds = maxSkewSeconds;
    }

    public record Headers(String timestamp, String nonce, String signature) {
    }

    public record Decrypted(String plaintextJson) {
    }

    public boolean verify(Headers headers, String body, long nowEpochSeconds) {
        if (headers == null || isBlank(headers.timestamp()) || isBlank(headers.nonce())
                || isBlank(headers.signature())) {
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(headers.timestamp().trim());
        } catch (NumberFormatException error) {
            return false;
        }
        if (Math.abs(nowEpochSeconds - ts) > maxSkewSeconds) {
            return false;   // 防重放
        }
        if (isBlank(publicKeyBase64) || isBlank(body)) {
            return false;
        }
        try {
            String message = headers.timestamp() + "\n" + headers.nonce() + "\n" + body + "\n";
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey());
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(headers.signature()));
        } catch (Exception error) {
            return false;
        }
    }

    /** 解密 `resource`：ciphertext/nonce/associated_data（均来自回调体）。 */
    public Decrypted decrypt(String ciphertextBase64, String nonce, String associatedData) {
        if (isBlank(apiV3Key)) {
            throw new IllegalStateException(
                    "app.payment.wechat.api-v3-key 未配置：无法解密微信回调");
        }
        if (apiV3Key.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("APIv3 密钥必须是 32 字节");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES"),
                    new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
            if (associatedData != null && !associatedData.isEmpty()) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertextBase64));
            return new Decrypted(new String(plain, StandardCharsets.UTF_8));
        } catch (Exception error) {
            throw new IllegalStateException("微信回调解密失败（密钥或报文不匹配）", error);
        }
    }

    /** 便捷解析：从解密后的 JSON 里取关键字段（amount.total 为分）。 */
    public Map<String, Object> parseTransaction(String plaintextJson) {
        try {
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(plaintextJson);
            return Map.of(
                    "outTradeNo", node.path("out_trade_no").asText(),
                    "transactionId", node.path("transaction_id").asText(),
                    "tradeState", node.path("trade_state").asText(),
                    "totalFen", node.path("amount").path("total").asLong());
        } catch (Exception error) {
            throw new IllegalStateException("微信回调报文解析失败", error);
        }
    }

    private PublicKey publicKey() throws Exception {
        String normalized = publicKeyBase64
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
