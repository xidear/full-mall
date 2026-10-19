package com.fullmall.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * 开启方法级授权（TECHNICAL_ARCHITECTURE_V2 §2.2）。
 *
 * <p>请求级粗粒度由 {@code StaffAuthFilter} + SecurityFilterChain 负责；
 * 这里只提供方法级细粒度能力，具体权限用 {@code @PreAuthorize} 标注在控制器/服务方法上。
 *
 * <p>注意：没有认证时调用会被拒绝，而不是静默放行——这正是避免“前端隐藏即安全”的关键。
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}