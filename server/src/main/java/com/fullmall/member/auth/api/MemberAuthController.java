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
}
