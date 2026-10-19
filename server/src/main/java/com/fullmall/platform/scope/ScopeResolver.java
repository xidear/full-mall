package com.fullmall.platform.scope;

import com.fullmall.platform.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 从 Spring Security 上下文解析行级范围。
 *
 * <p>当前角色模型：ADMIN / OPERATOR 为全量，其余角色按用户名归属过滤。
 * 越权不返回 403 而是 404（避免泄露资源是否存在），与架构文档要求一致。
 */
@Component
public class ScopeResolver {

    private static final Set<String> FULL_SCOPE_ROLES = Set.of("ROLE_ADMIN", "ROLE_OPERATOR");

    public Scope resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "请先登录");
        }
        boolean fullScope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(FULL_SCOPE_ROLES::contains);
        if (fullScope) {
            return Scope.all();
        }
        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("已认证但用户名为空的请求无法确定行级范围");
        }
        return Scope.owner(name);
    }
}