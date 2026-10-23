package com.fullmall.payment;

import com.fullmall.payment.callback.WechatV3CallbackVerifier;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 微信 v3 回调：真实 RSA 验签 + AES-GCM 解密 + 防重放。
 * 用运行时生成的密钥对，不依赖真实商户证书。
 */
class WechatV3CallbackVerifierTest {

    private static final String API_V3_KEY = "01234567890123456789012345678901"; // 32 字节

    private KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private WechatV3CallbackVerifier verifier(KeyPair pair) {
        String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        return new WechatV3CallbackVerifier(publicKey, API_V3_KEY, 300);
    }

    private String sign(KeyPair pair, String timestamp, String nonce, String body) throws Exception {
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(pair.getPrivate());
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private String encrypt(String plaintext, String nonce, String associatedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(API_V3_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(
                cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void acceptsValidSignatureAndDecryptsResource() throws Exception {
        KeyPair pair = keyPair();
        var verifier = verifier(pair);
        String body = "{\"id\":\"evt-1\"}";
        String ts = "1700000000";
        String nonce = "abc123";
        var headers = new WechatV3CallbackVerifier.Headers(ts, nonce, sign(pair, ts, nonce, body));

        assertThat(verifier.verify(headers, body, 1_700_000_010L)).isTrue();

        String resourceNonce = "nonce-123456";
        String plain = "{\"out_trade_no\":\"SO-1\",\"transaction_id\":\"T-1\","
                + "\"trade_state\":\"SUCCESS\",\"amount\":{\"total\":1990}}";
        String ciphertext = encrypt(plain, resourceNonce, "transaction");
        var decrypted = verifier.decrypt(ciphertext, resourceNonce, "transaction");
        var parsed = verifier.parseTransaction(decrypted.plaintextJson());

        assertThat(parsed).containsEntry("outTradeNo", "SO-1")
                .containsEntry("tradeState", "SUCCESS")
                .containsEntry("totalFen", 1990L);
    }

    @Test
    void rejectsForgedSignatureReplayAndTamperedBody() throws Exception {
        KeyPair pair = keyPair();
        var verifier = verifier(pair);
        String body = "{\"id\":\"evt-2\"}";
        String ts = "1700000000";
        String nonce = "n1";
        String goodSignature = sign(pair, ts, nonce, body);

        // 篡改 body
        assertThat(verifier.verify(new WechatV3CallbackVerifier.Headers(ts, nonce, goodSignature),
                "{\"id\":\"evt-2\",\"x\":1}", 1_700_000_000L)).isFalse();
        // 过期时间戳（重放）
        assertThat(verifier.verify(new WechatV3CallbackVerifier.Headers(ts, nonce, goodSignature),
                body, 1_700_100_000L)).isFalse();
        // 伪造签名
        assertThat(verifier.verify(new WechatV3CallbackVerifier.Headers(ts, nonce, "Zm9yZ2Vk"),
                body, 1_700_000_000L)).isFalse();
        // 缺字段
        assertThat(verifier.verify(new WechatV3CallbackVerifier.Headers("", nonce, goodSignature),
                body, 1_700_000_000L)).isFalse();
    }

    @Test
    void failsLoudlyWhenApiV3KeyMissingOrWrongLength() {
        var noKey = new WechatV3CallbackVerifier("", "", 300);
        assertThatThrownBy(() -> noKey.decrypt("abc", "nonce", "ad"))
                .hasMessageContaining("api-v3-key 未配置");

        var shortKey = new WechatV3CallbackVerifier("", "short", 300);
        assertThatThrownBy(() -> shortKey.decrypt("abc", "nonce", "ad"))
                .hasMessageContaining("32 字节");
    }
}
