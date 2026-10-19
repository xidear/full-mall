package com.fullmall.platform.identity;

import com.fullmall.platform.error.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 归属标识解析（购物车、地址等按人隔离的数据共用）。
 *
 * <p>放在 platform 是为了让各业务模块共用同一套规则，**避免模块之间互相依赖形成环**。
 * 规则：已登录 → `member:<用户名>`；未登录 → 必须提供 `X-Guest-Token` → `guest:<token>`。
 */
@Component
public class OwnerKeyResolver {

    public static final String GUEST_TOKEN_HEADER = "X-Cart-Token";
    private static final int MAX_TOKEN_LENGTH = 128;

    public String memberKey(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw ApiException.badRequest("OWNER_REQUIRED", "缺少登录身份或游客 token");
        }
        return "member:" + authentication.getName();
    }

    public String guestKey(String token) {
        if (token == null || token.isBlank()) {
            throw ApiException.badRequest("GUEST_TOKEN_REQUIRED",
                    "未登录时必须提供 " + GUEST_TOKEN_HEADER + " 头");
        }
        String trimmed = token.trim();
        if (trimmed.length() > MAX_TOKEN_LENGTH) {
            throw ApiException.badRequest("GUEST_TOKEN_INVALID", "游客 token 过长");
        }
        if (!trimmed.matches("^[A-Za-z0-9_-]+$")) {
            throw ApiException.badRequest("GUEST_TOKEN_INVALID", "游客 token 只允许字母数字、- 和 _");
        }
        return "guest:" + trimmed;
    }

    public String ownerKey(Authentication authentication, String guestToken) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null && !authentication.getName().isBlank()
                && !"anonymousUser".equals(authentication.getName())) {
            return memberKey(authentication);
        }
        return guestKey(guestToken);
    }
}
