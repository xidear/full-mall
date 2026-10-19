package com.fullmall.article.api;

import com.fullmall.article.app.ArticleAdminService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** 文章后台：创建 / 写译文 / 送审 / 发布。全部要求 ADMIN。 */
@RestController
@RequestMapping("/admin/v1/articles")
public class AdminArticleController {

    private final ArticleAdminService service;

    public AdminArticleController(ArticleAdminService service) {
        this.service = service;
    }

    public record CreateArticle(@NotBlank String slug, @NotBlank String type) {
    }

    public record TranslationBody(@NotBlank String title, String summary, @NotBlank String content) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, UUID> create(@RequestBody CreateArticle body, Authentication authentication) {
        return Map.of("id", service.create(body.slug(), body.type(), authentication.getName()));
    }

    @PutMapping("/{articleId}/translations/{locale}")
    @PreAuthorize("hasRole('ADMIN')")
    public void saveTranslation(@PathVariable UUID articleId, @PathVariable String locale,
                                @RequestBody TranslationBody body, Authentication authentication) {
        service.upsertTranslation(articleId, locale, body.title(), body.summary(),
                body.content(), authentication.getName());
    }

    @PostMapping("/{articleId}/translations/{locale}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public void review(@PathVariable UUID articleId, @PathVariable String locale,
                       Authentication authentication) {
        service.review(articleId, locale, authentication.getName());
    }

    @PostMapping("/{articleId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public void publish(@PathVariable UUID articleId, Authentication authentication) {
        service.publish(articleId, authentication.getName());
    }
}
