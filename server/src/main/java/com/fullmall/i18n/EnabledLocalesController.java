package com.fullmall.i18n;

import com.fullmall.setting.app.SystemSettingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 已启用语言（唯一来源：系统设置 enabled_locales）。
 *
 * <p>后台译文的语言 tab、前台语言切换、移动端语言包都从这里取，
 * 禁止各端硬编码 ['en','ja']——否则配置改了页面不会变，属于典型"双份真相"。
 */
@RestController
@RequestMapping("/admin/v1/i18n")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class EnabledLocalesController {

    private final SystemSettingService settings;

    public EnabledLocalesController(SystemSettingService settings) {
        this.settings = settings;
    }

    public record LocalesView(String defaultLocale, List<String> enabledLocales,
                              List<String> translationLocales) {
    }

    @GetMapping("/locales")
    public LocalesView locales() {
        return new LocalesView(settings.defaultLocale(), settings.enabledLocales(),
                settings.translationLocales());
    }
}
