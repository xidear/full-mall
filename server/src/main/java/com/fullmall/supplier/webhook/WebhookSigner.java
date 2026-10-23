package com.fullmall.supplier.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Webhook 签名：`HMAC-SHA256(secret, timestamp + "." + body)`，十六进制小写。
 *
 * <p>把 timestamp 纳入签名可防重放（对端校验时间窗）；
 * 这是业界通行做法（与 GitHub/Stripe 的 scheme 同构）。
 */
public final class WebhookSigner {

    private WebhookSigner() {
    }

    public static String sign(String secret, long timestampSeconds, String body) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("webhook secret 不能为空");
        }
        return com.fullmall.platform.crypto.Hmac.hex(
                secret, timestampSeconds + "." + (body == null ? "" : body));
    }
}
