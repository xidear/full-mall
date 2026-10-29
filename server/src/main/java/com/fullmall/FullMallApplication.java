package com.fullmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动入口（模块化单体）。必须留在 {@code com.fullmall} 根包：各业务模块的 api/app/domain/infra
 * 子包只有在这个扫描根下才会被收录，{@code ModulithStructureTest} 也以本类为入口校验模块边界。
 * 启动失败不做兜底——缺配置（如 {@code SecretCipher} 缺 {@code APP_SECRET_KEY}）直接抛错退出。
 */
@SpringBootApplication
public class FullMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(FullMallApplication.class, args);
    }
}