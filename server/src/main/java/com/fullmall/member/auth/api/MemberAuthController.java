package com.fullmall.member.auth.api;

import com.fullmall.member.auth.MemberAuthService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 会员手机 OTP 登录。演示/开发环境可回传验证码（生产关闭）。 */
@RestController
@RequestMapping("/api/v1/member/auth")
public class MemberAuthController {

    private final MemberAuthService auth;

    public MemberAuthController(MemberAuthService auth) {
        this.auth = auth;
    }

    public record RequestOtp(@NotBlank String phone) {
    }

    public record VerifyOtp(@NotBlank String phone, @NotBlank String code) {
    }

    /** 当前登录会员资料（需 Bearer token）。 */
    @GetMapping("/me")
    public MemberAuthService.MemberProfile me(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "MEMBER_REQUIRED", "需要会员登录");
        }
        return auth.profile(java.util.UUID.fromString(authentication.getName()));
    }

    /** 登出：吊销当前 Bearer token。 */
    @PostMapping("/logout")
    public Map<String, Boolean> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Map.of("revoked", false);
        }
        return Map.of("revoked", auth.logout(authorization.substring("Bearer ".length())));
    }

    @PostMapping("/otp")
    public Map<String, Object> requestOtp(@RequestBody RequestOtp body) {
        var issued = auth.requestOtp(body.phone());
        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("phone", issued.phone());
        payload.put("expiresAt", issued.expiresAt());
        if (issued.code() != null) {
            payload.put("debugCode", issued.code());
        }
        return payload;
    }

    @PostMapping("/verify")
    public MemberAuthService.SessionIssued verify(@RequestBody VerifyOtp body) {
        return auth.verifyOtp(body.phone(), body.code());
    }

    public record EmailOtp(@NotBlank String email) {
    }

    public record VerifyEmailOtp(@NotBlank String email, @NotBlank String code) {
    }

    /**
     * 邮箱验证码（批次 F1）：海外用户很多国家短信不可达或成本高，
     * 邮箱登录让"换了手机号/收不到短信"的会员仍能登录。
     */
    @PostMapping("/email/otp")
    public Map<String, Object> requestEmailOtp(@RequestBody EmailOtp body) {
        var issued = auth.requestEmailOtp(body.email());
        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("email", issued.email());
        payload.put("expiresAt", issued.expiresAt());
        if (issued.code() != null) {
            payload.put("debugCode", issued.code());
        }
        return payload;
    }

    @PostMapping("/email/verify")
    public MemberAuthService.SessionIssued verifyEmail(@RequestBody VerifyEmailOtp body) {
        return auth.verifyEmailOtp(body.email(), body.code());
    }

    public record BindEmail(@NotBlank String email) {
    }

    /** 绑定/更换邮箱（需 Bearer token）：先绑定才能用邮箱登录。 */
    @PutMapping("/email")
    public MemberAuthService.MemberProfile bindEmail(
            @RequestBody BindEmail body, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest("MEMBER_REQUIRED", "需要会员登录");
        }
        java.util.UUID memberId = java.util.UUID.fromString(authentication.getName());
        auth.bindEmail(memberId, body.email());
        return auth.profile(memberId);
    }
}
