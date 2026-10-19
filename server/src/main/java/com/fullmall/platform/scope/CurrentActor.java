package com.fullmall.platform.scope;

import com.fullmall.platform.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 当前操作用户（用于审计）。
 *
 * <p>优先取 {@link ActorDetails}（由鉴权过滤器写入），否则取 SecurityContext 的认证名。
 * **没有认证时抛 401，不返回 “system”/“anonymous” 之类假身份**——审计记不到人就应该失败。
 *
 * <p>后台任务/定时任务如需记录审计，必须显式传入系统身份，不走本类。
 */
@Component
public class CurrentActor {

    public String requireUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                    "该操作需要记录审计人，但当前请求没有认证信息");
        }
        if (authentication.getDetails() instanceof ActorDetails actor
                && actor.username() != null && !actor.username().isBlank()) {
            return actor.username();
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                    "该操作需要记录审计人，但当前认证主体没有用户名");
        }
        return name;
    }
}