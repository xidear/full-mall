package com.fullmall.platform.secret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 加密：主密钥缺失/长度不对即失败；往返正确；篡改必失败。 */
class SecretCipherTest {

    @Test
    void missingOrInvalidMasterKeyFailsFast() {
        assertThatThrownBy(() -> new SecretCipher(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_SECRET_KEY 未配置");
        assertThatThrownBy(() -> new SecretCipher("not-base64!!"))
                .hasMessageContaining("Base64");
        assertThatThrownBy(() -> new SecretCipher("c2hvcnQ="))
                .hasMessageContaining("32 字节");
    }

    @Test
    void roundTripAndTamperDetection() {
        var cipher = new SecretCipher(SecretCipher.generateKey());
        String secret = "{\"accessKey\":\"ak\",\"secretKey\":\"sk\"}";

        String encrypted = cipher.encrypt(secret);
        assertThat(encrypted).doesNotContain("secretKey");
        assertThat(cipher.decrypt(encrypted)).isEqualTo(secret);

        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";
        assertThatThrownBy(() -> cipher.decrypt(tampered)).hasMessageContaining("解密失败");
    }
}
