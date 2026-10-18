package com.fullmall.setting.api;

import com.fullmall.setting.app.SystemSettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SystemSettingController {

    private final SystemSettingService service;

    public SystemSettingController(SystemSettingService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/site/settings")
    public Map<String, String> publicSettings() {
        return service.publicSettings();
    }

    @GetMapping("/admin/v1/settings")
    public Map<String, String> adminSettings() {
        return service.adminSettings();
    }

    @PutMapping("/admin/v1/settings")
    public Map<String, String> save(@RequestBody Map<String, String> values) {
        return service.save(values);
    }
}