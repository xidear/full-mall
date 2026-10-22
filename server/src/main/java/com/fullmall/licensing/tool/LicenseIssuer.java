package com.fullmall.licensing.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.licensing.app.LicensePayload;
import com.fullmall.licensing.app.LicenseVerifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * 许可证签发（打包/交付用）。
 *
 * <p>用法示例：
 * <pre>
 * java -cp app.jar com.fullmall.licensing.tool.LicenseIssuer \
 *      --customer "示例客户" --days 7 --domains "shop.example.com" --out /secure/license.json
 * </pre>
 *
 * <p>安全约定：
 * <ul>
 *   <li>私钥**只落盘到指定路径**（默认 `~/.dsh-license/private.key`），绝不进入仓库；</li>
 *   <li>`--days 0` 表示永久（`expiresAt = null`）；</li>
 *   <li>签完立即用 `LicenseVerifier` 自检，签错直接失败（不允许产出无法验证的证书）。</li>
 * </ul>
 */
public final class LicenseIssuer {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private LicenseIssuer() {
    }

    public record Issued(String licenseJson, String publicKeyBase64, LicensePayload payload) {
    }

    public static Issued issue(String customer, int days, List<String> domains,
                               String edition, List<String> features, KeyPair keyPair,
                               Instant now) throws Exception {
        LicensePayload payload = new LicensePayload(
                "LIC-" + now.toEpochMilli(),
                customer,
                edition == null || edition.isBlank() ? "ENTERPRISE" : edition,
                features == null ? List.of() : features,
                domains == null ? List.of() : domains,
                now,
                days <= 0 ? null : now.plus(Duration.ofDays(days)),
                0);
        byte[] payloadBytes = MAPPER.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(keyPair.getPrivate());
        signature.update(payloadBytes);

        String licenseJson = MAPPER.writeValueAsString(new SignedFile(
                Base64.getEncoder().encodeToString(payloadBytes),
                Base64.getEncoder().encodeToString(signature.sign())));
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        LicensePayload verified = LicenseVerifier.verify(
                new LicenseVerifier.Signed(
                        MAPPER.readTree(licenseJson).path("payload").asText(),
                        MAPPER.readTree(licenseJson).path("signature").asText()),
                publicKey);
        if (verified == null) {
            throw new IllegalStateException("签发自检失败：许可证无法通过验签");
        }
        return new Issued(licenseJson, publicKey, verified);
    }

    public record SignedFile(String payload, String signature) {
    }

    public static KeyPair newKeyPair() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    public static void main(String[] args) throws Exception {
        String customer = arg(args, "--customer", "示例客户");
        int days = Integer.parseInt(arg(args, "--days", "0"));
        String domainArg = arg(args, "--domains", "localhost");
        String out = arg(args, "--out", "license.json");
        String keyOut = arg(args, "--private-key", Path.of(System.getProperty("user.home"),
                ".dsh-license", "private.key").toString());

        KeyPair keyPair = newKeyPair();
        Path keyPath = Path.of(keyOut);
        if (keyPath.getParent() != null) {
            Files.createDirectories(keyPath.getParent());
        }
        Files.write(keyPath, keyPair.getPrivate().getEncoded());
        keyPath.toFile().setReadable(false, false);
        keyPath.toFile().setReadable(true, true);

        Issued issued = issue(customer, days,
                List.of(domainArg.split(",")), "ENTERPRISE", List.of("ai"), keyPair, Instant.now());
        Files.writeString(Path.of(out), issued.licenseJson());
        System.out.println("license: " + out);
        System.out.println("private key: " + keyPath + "（不入库，请离线保管）");
        System.out.println("public key(app config app.license.public-key): " + issued.publicKeyBase64());
    }

    private static String arg(String[] args, String name, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                return args[i + 1];
            }
        }
        return fallback;
    }
}
