package com.fullmall.article.api;

import com.fullmall.article.app.ArticleService;
import com.fullmall.setting.app.SystemSettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 商城文章：公告 / 帮助文档 / 单页。只读已发布内容。 */
@RestController
public class ArticleController {

    private final ArticleService articleService;
    private final SystemSettingService settings;

    public ArticleController(ArticleService articleService, SystemSettingService settings) {
        this.articleService = articleService;
        this.settings = settings;
    }

    @GetMapping("/api/v1/articles")
    public List<ArticleService.ArticleSummary> list(
            @RequestParam(defaultValue = "ANNOUNCEMENT") String type,
            @RequestParam(required = false) String locale,
            @RequestParam(defaultValue = "20") int limit) {
        return articleService.listPublished(type, locale, settings.defaultLocale(), limit);
    }

    @GetMapping("/api/v1/articles/{slug}")
    public ArticleService.ArticleView detail(@PathVariable String slug,
                                             @RequestParam(required = false) String locale) {
        return articleService.findBySlug(slug, locale, settings.defaultLocale());
    }
}
