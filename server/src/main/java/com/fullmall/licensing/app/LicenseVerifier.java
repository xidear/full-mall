package com.fullmall.licensing.app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * 许可证验签与有效性判断（无状态，便于测试）。
 *
 * <p>文件格式：
 * <pre>{"payload":"&lt;base64(JSON)&gt;","signature":"&lt;base64(Ed25519)&gt;"}</pre>
 * 签名覆盖 **payload 解码后的原始字节**，不依赖 JSON 字段顺序，避免"同样内容签名不一致"的坑。
 */
public final class LicenseVerifier {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    private LicenseVerifier() {
    }

    /** 许可证文件结构：字段名与线上文件格式一致（payload/signature），不要改名。 */
    public record Signed(
            @com.fasterxml.jackson.annotation.JsonProperty("payload") String payloadBase64,
            @com.fasterxml.jackson.annotation.JsonProperty("signature") String signatureBase64) {
    }

    /** 验签并解析正文；签名无效返回 null 调用方必须判空。 */
    public static LicensePayload verify(Signed signed, String publicKeyBase64) {
        try {
            byte[] payload = Base64.getDecoder().decode(signed.payloadBase64());
            byte[] signature = Base64.getDecoder().decode(signed.signatureBase64());
            PublicKey key = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(
                            Base64.getDecoder().decode(publicKeyBase64)));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(payload);
            if (!verifier.verify(signature)) {
                return null;
            }
            return MAPPER.readValue(payload, LicensePayload.class);
        } catch (Exception error) {
            // 解析/验签失败都视为无效许可证（调用方据此进入只读），但要把原因带出去给日志
            throw new IllegalArgumentException("许可证无法解析或验签失败：" + error.getMessage(), error);
        }
    }

    /** 域名匹配：支持 `*.example.com` 一级通配；localhost 永远放行（运维与演示）。 */
    public static boolean domainAllowed(List<String> domains, String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalized = host.trim().toLowerCase();
        int colon = normalized.indexOf(':');
        if (colon >= 0) {
            normalized = normalized.substring(0, colon);
        }
        if ("localhost".equals(normalized) || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized)) {
            return true;
        }
        if (domains == null || domains.isEmpty()) {
            return false;
        }
        for (String pattern : domains) {
            String candidate = pattern.trim().toLowerCase();
            if (candidate.equals(normalized)) {
                return true;
            }
            if (candidate.startsWith("*.")) {
                String suffix = candidate.substring(1);   // ".example.com"
                if (normalized.endsWith(suffix)) {
                    String prefix = normalized.substring(0, normalized.length() - suffix.length());
                    if (!prefix.isEmpty() && !prefix.contains(".")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 到期判断：`expiresAt + graceDays` 之后算过期；null 表示永久。 */
    public static boolean expired(LicensePayload payload, Instant now) {
        if (payload.expiresAt() == null) {
            return false;
        }
        Instant limit = payload.expiresAt().plus(Duration.ofDays(Math.max(0, payload.graceDays())));
        return now.isAfter(limit);
    }
}
