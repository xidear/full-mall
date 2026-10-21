package com.fullmall.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 企业版装配入口；`app.ai.enabled=false` 时控制器仍存在但会统一返回 503（见 AdminAiController）。 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {
}
