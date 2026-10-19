package com.fullmall.platform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关。
 *
 * <p>**用显式属性控制，不用 profile 表达式**：`@Profile("!test")` 在 `["test","dev"]` 下仍会匹配
 * （dev 满足"非 test"），导致测试里也跑定时任务、在无迁移的表上报 SQL 错误。
 *
 * <p>开关：`app.scheduling.enabled`（默认 true）。测试环境在 `application-test.yml` 关闭，
 * 需要调度的测试单独打开。
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
