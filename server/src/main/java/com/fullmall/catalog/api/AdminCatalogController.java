package com.fullmall.catalog.api;

import com.fullmall.catalog.app.CatalogService;
import com.fullmall.catalog.domain.Product;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/products")
public class AdminCatalogController {

    private final CatalogService catalogService;
    private final com.fullmall.catalog.app.ProductTranslationWorkflowService translationWorkflow;

    public AdminCatalogController(CatalogService catalogService,
                                  com.fullmall.catalog.app.ProductTranslationWorkflowService translationWorkflow) {
        this.catalogService = catalogService;
        this.translationWorkflow = translationWorkflow;
    }

    /** 译文送审（I18N-003）。 */
    @PostMapping("/{productId}/translations/{locale}/review")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public com.fullmall.catalog.infra.ProductTranslationRepository.Row reviewTranslation(
            @PathVariable UUID productId, @PathVariable String locale,
            org.springframework.security.core.Authentication authentication) {
        return translationWorkflow.review(productId, locale, authentication.getName());
    }

    /** 译文发布（I18N-004）：只有已审校可发布。 */
    @PostMapping("/{productId}/translations/{locale}/publish")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public com.fullmall.catalog.infra.ProductTranslationRepository.Row publishTranslation(
            @PathVariable UUID productId, @PathVariable String locale,
            @RequestParam(defaultValue = "false") boolean sensitive,
            org.springframework.security.core.Authentication authentication) {
        return translationWorkflow.publish(productId, locale, sensitive, authentication.getName());
    }

    @GetMapping
    public ProductPage list(
            @RequestParam(defaultValue = "zh-CN") String locale,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return catalogService.listAllProducts(locale, keyword, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductUpsertRequest request) {
        return catalogService.createProduct(request);
    }

    @PatchMapping("/{productId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@PathVariable UUID productId,
                             @Valid @RequestBody ProductStatusUpdateRequest request) {
        catalogService.updateStatus(productId, request.status());
    }
}