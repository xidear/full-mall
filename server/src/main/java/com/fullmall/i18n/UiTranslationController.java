package com.fullmall.i18n;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 多语言文案：前台取 bundle，后台维护。 */
@RestController
public class UiTranslationController {

    private final UiTranslationService translations;

    public UiTranslationController(UiTranslationService translations) {
        this.translations = translations;
    }

    @GetMapping("/api/v1/i18n/{locale}")
    public Map<String, String> bundle(@PathVariable String locale) {
        return translations.bundle(locale);
    }

    @PutMapping("/admin/v1/i18n/{locale}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> upsert(@PathVariable String locale,
                                      @RequestBody Map<String, String> entries) {
        translations.upsert(locale, entries);
        return Map.of("updated", entries.size());
    }

    @GetMapping("/admin/v1/i18n/coverage")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<Map<String, Object>> coverage() {
        return translations.coverage();
    }
}
