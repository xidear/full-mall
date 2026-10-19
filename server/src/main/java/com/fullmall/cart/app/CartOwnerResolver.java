package com.fullmall.cart.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 购物车归属解析：登录用户优先，否则必须带游客 token。
 *
 * <p>规则：
 * <ul>
 *   <li>已认证 → `member:<用户名>`；</li>
 *   <li>未认证 → 必须提供 `X-Cart-Token`，解析为 `guest:<token>`；</li>
 *   <li>两者都没有 → 400（不创建"匿名共享购物车"，避免串车）。</li>
 * </ul>
 * token 只做格式校验（长度与字符），不在这里校验是否存在——购物车不存在由服务层处理。
 */
@Component
public class CartOwnerResolver {

    public static final String GUEST_TOKEN_HEADER = "X-Cart-Token";
    private static final int MAX_TOKEN_LENGTH = 128;

    public String memberKey(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw ApiException.badRequest("CART_OWNER_REQUIRED", "缺少登录身份或游客购物车 token");
        }
        return "member:" + authentication.getName();
    }

    public String guestKey(String token) {
        if (token == null || token.isBlank()) {
            throw ApiException.badRequest("CART_TOKEN_REQUIRED",
                    "未登录时必须提供 " + GUEST_TOKEN_HEADER + " 头");
        }
        String trimmed = token.trim();
        if (trimmed.length() > MAX_TOKEN_LENGTH) {
            throw ApiException.badRequest("CART_TOKEN_INVALID", "购物车 token 过长");
        }
        if (!trimmed.matches("^[A-Za-z0-9_-]+$")) {
            throw ApiException.badRequest("CART_TOKEN_INVALID", "购物车 token 只允许字母数字、- 和 _");
        }
        return "guest:" + trimmed;
    }

    /** 任意身份（登录或游客）对应的归属键。 */
    public String ownerKey(Authentication authentication, String guestToken) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null && !authentication.getName().isBlank()
                && !"anonymousUser".equals(authentication.getName())) {
            return memberKey(authentication);
        }
        return guestKey(guestToken);
    }
}
