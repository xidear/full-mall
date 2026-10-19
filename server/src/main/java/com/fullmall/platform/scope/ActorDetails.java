package com.fullmall.platform.scope;

import java.util.UUID;

/**
 * 认证主体的附加信息，放进 Spring Security 的 {@code details}。
 *
 * <p>放在 platform（OPEN 模块）是为了让鉴权过滤器与基类控制器共享，不产生模块环依赖。
 *
 * @param id       员工 UUID，未知时为 null（不伪造）
 * @param username 用户名
 * @param role     角色编码
 */
public record ActorDetails(UUID id, String username, String role) {
}