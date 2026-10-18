package com.fullmall.theme.api;

import com.fullmall.theme.app.ThemeConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ThemeConfigController {

    private final ThemeConfigService service;

    public ThemeConfigController(ThemeConfigService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/theme")
    public ThemeResponse get(@RequestParam(defaultValue = "PC") String channel,
                             @RequestParam(defaultValue = "CN") String market) {
        return service.get(channel, market);
    }

    @PutMapping("/admin/v1/theme")
    public ThemeResponse save(@Valid @RequestBody ThemeSaveRequest request) {
        return service.save(request.channel(), request.market(), request.config());
    }
}