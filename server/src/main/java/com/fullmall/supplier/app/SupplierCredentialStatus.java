package com.fullmall.supplier.app;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 供应商凭证健康度与风险提示（SUP-031）。
 *
 * <p>规则是可测的纯函数，不查库；调用方把当前状态传进来。
 * 原则：**只提示、不伪造**——不知道该算的风险就说不知道，不返回“安全”结论。
 */
public final class SupplierCredentialStatus {

    private SupplierCredentialStatus() {
    }

    /**
     * @param authMode       HMAC_ONLY / TOKEN_ONLY / BOTH
     * @param ipAllowlist    逗号分隔 CIDR，空白表示未配置
     * @param tokenExpiresAt token 过期时间，未签发时为 null
     * @param now            当前时间（便于测试）
     */
    public static List<String> warnings(String authMode, String ipAllowlist,
                                        Instant tokenExpiresAt, Instant now) {
        List<String> warnings = new ArrayList<>();

        if ("HMAC_ONLY".equals(authMode)) {
            warnings.add("当前仅启用 HMAC 签名认证；建议同时启用 Bearer Token（SUP-031）");
        } else if ("TOKEN_ONLY".equals(authMode)) {
            warnings.add("当前仅启用 Bearer Token；服务端到服务端写操作建议使用 HMAC 签名");
        }

        if (ipAllowlist == null || ipAllowlist.isBlank()) {
            warnings.add("未配置 IP 白名单；建议限制调用来源，降低凭证泄露后的影响面");
        }

        if ("TOKEN_ONLY".equals(authMode) || "BOTH".equals(authMode)) {
            if (tokenExpiresAt == null) {
                warnings.add("认证模式包含 Bearer Token，但尚未签发 token");
            } else if (!tokenExpiresAt.isAfter(now)) {
                warnings.add("token 已过期，需要重新签发");
            } else if (tokenExpiresAt.isBefore(now.plus(Duration.ofDays(7)))) {
                warnings.add("token 将在 7 天内过期，建议安排轮换");
            }
        }

        return warnings;
    }
}