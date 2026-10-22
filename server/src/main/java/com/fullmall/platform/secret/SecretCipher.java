package com.fullmall.platform.secret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 第三方凭据加密（AES-256-GCM）。
 *
 * <p>主密钥来自环境变量 `APP_SECRET_KEY`（Base64 的 32 字节）。
 * **缺失或长度不对 → 启动即失败**，绝不退化成明文存储。
 */
@Component
public class SecretCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecretCipher(@Value("${app.secret-key:}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "APP_SECRET_KEY 未配置：第三方配置必须加密存储，不允许明文");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("APP_SECRET_KEY 必须是 Base64 编码", error);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "APP_SECRET_KEY 必须是 32 字节（AES-256），实际 " + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /** 加密：输出 iv || ciphertext（Base64）。 */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("待加密内容不能为 null");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception error) {
            throw new IllegalStateException("加密失败：" + error.getMessage(), error);
        }
    }

    /** 解密：校验失败（被篡改/密钥不符）抛错，不返回空值。 */
    public String decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null || ciphertextBase64.isBlank()) {
            throw new IllegalArgumentException("密文不能为空");
        }
        try {
            byte[] raw = Base64.getDecoder().decode(ciphertextBase64);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(raw, 0, iv, 0, IV_LENGTH);
            byte[] payload = new byte[raw.length - IV_LENGTH];
            System.arraycopy(raw, IV_LENGTH, payload, 0, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
        } catch (Exception error) {
            throw new IllegalStateException("解密失败（密钥不符或数据被篡改）", error);
        }
    }

    /** 生成一把新主密钥（运维初始配置用）。 */
    public static String generateKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
