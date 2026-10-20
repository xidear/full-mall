package com.fullmall.platform.config.api;

import com.fullmall.platform.config.ProductionReadinessChecker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台「系统就绪」：展示各能力是否已配置真实服务商。
 *
 * <p>缺配置不影响启动，管理员在这里看到缺口后去「系统设置」填写即可。
 */
@RestController
@RequestMapping("/admin/v1/system")
public class SystemReadinessController {

    private final ProductionReadinessChecker checker;

    public SystemReadinessController(ProductionReadinessChecker checker) {
        this.checker = checker;
    }

    @GetMapping("/readiness")
    public ProductionReadinessChecker.Report readiness() {
        return checker.check();
    }
}
