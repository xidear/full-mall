package com.fullmall.payment.alipay;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 支付宝 RSA2 签名与验签。
 *
 * <p>签名串：按参数名**字典序**拼接 `k=v` 用 `&` 连接（**排除 sign 与空值**），
 * 算法 `SHA256withRSA`。请求与响应都用同一套规则。
 */
public final class AlipaySigner {

    private AlipaySigner() {
    }

    public static String signContent(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .filter(entry -> !"sign".equals(entry.getKey()))
                .filter(entry -> !"sign_type".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    public static String sign(Map<String, String> params, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(signContent(params).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception error) {
            throw new IllegalStateException("支付宝请求签名失败", error);
        }
    }

    public static boolean verify(String content, String base64Signature, PublicKey publicKey) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(content.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(base64Signature));
        } catch (Exception error) {
            return false;
        }
    }

    public static PrivateKey privateKeyFromPem(String pem) {
        try {
            String normalized = strip(pem, "PRIVATE KEY");
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(normalized)));
        } catch (Exception error) {
            throw new IllegalStateException("支付宝应用私钥解析失败（需 PKCS#8）", error);
        }
    }

    public static PublicKey publicKeyFromPem(String pem) {
        try {
            String normalized = strip(pem, "PUBLIC KEY");
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(normalized)));
        } catch (Exception error) {
            throw new IllegalStateException("支付宝公钥解析失败", error);
        }
    }

    private static String strip(String pem, String label) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("密钥为空");
        }
        return pem.replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
    }
}
