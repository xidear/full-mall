package com.fullmall.licensing.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 许可配置装配（默认关闭，不影响开源/开发环境）。 */
@Configuration
@EnableConfigurationProperties(LicenseProperties.class)
public class LicenseConfiguration {
}
