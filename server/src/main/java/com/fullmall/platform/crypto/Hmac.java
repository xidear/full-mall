package com.fullmall.platform.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * HMAC-SHA256 统一实现（供应商签名、Webhook 签名共用）。
 * 此前两处各自 Mac.getInstance + 十六进制格式化，容易改一处忘另一处。
 */
public final class Hmac {

    private Hmac() {
    }

    public static String hex(String secret, String content) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("HMAC 密钥不能为空");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal((content == null ? "" : content).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("HMAC 计算失败", error);
        }
    }
}
