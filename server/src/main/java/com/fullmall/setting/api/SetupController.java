package com.fullmall.setting.api;

import com.fullmall.setting.app.SetupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 首次配置引导：状态查询与"标记完成"。 */
@RestController
@RequestMapping("/admin/v1/setup")
public class SetupController {

    private final SetupService setup;

    public SetupController(SetupService setup) {
        this.setup = setup;
    }

    @GetMapping("/status")
    public SetupService.SetupStatus status() {
        return setup.status();
    }

    @PostMapping("/complete")
    public SetupService.SetupStatus complete() {
        setup.markCompleted();
        return setup.status();
    }
}
