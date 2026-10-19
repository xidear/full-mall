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
