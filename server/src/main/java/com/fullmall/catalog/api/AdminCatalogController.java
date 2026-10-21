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
    private final com.fullmall.catalog.app.SkuCostService skuCostService;
    private final com.fullmall.catalog.app.ProductTranslationWorkflowService translationWorkflow;

    public AdminCatalogController(CatalogService catalogService,
                                  com.fullmall.catalog.app.ProductTranslationWorkflowService translationWorkflow,
                                  com.fullmall.catalog.app.SkuCostService skuCostService) {
        this.catalogService = catalogService;
        this.translationWorkflow = translationWorkflow;
        this.skuCostService = skuCostService;
    }

    /**
     * 成本只读视图（仅 ADMIN）：均价 + 最近批次成本 + 来源。
     *
     * <p>无成本记录时 `unknown=true`，**不返回 0**，避免毛利被算成 100%。
     */
    @org.springframework.web.bind.annotation.GetMapping("/skus/{skuId}/cost-view")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public java.util.Map<String, Object> costView(@PathVariable UUID skuId) {
        var snapshot = skuCostService.current(skuId);
        var batches = skuCostService.latestBatch(skuId);
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("skuId", skuId);
        body.put("unknown", snapshot == null);
        if (snapshot != null) {
            body.put("quantity", snapshot.quantity());
            body.put("amount", snapshot.amount());
            body.put("avgCost", snapshot.avgCost());
            body.put("stale", snapshot.stale());
        }
        body.put("latestBatch", batches);
        return body;
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
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return catalogService.listAllProducts(locale, keyword, page, size, sort, direction);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductUpsertRequest request) {
        return catalogService.createProduct(request);
    }

    public record CostRequest(Long costAmount) {
    }

    /**
     * 维护 SKU 成本（仅 ADMIN）。
     *
     * <p>成本用于毛利、佣金 PROFIT 模式与砍价底价校验，属于敏感数据，
     * 不对 OPERATOR 开放，也不进入任何顾客侧 DTO。
     */
    @org.springframework.web.bind.annotation.PutMapping("/skus/{skuId}/cost")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public void updateCost(@PathVariable UUID skuId,
                           @org.springframework.web.bind.annotation.RequestBody CostRequest body) {
        catalogService.updateCost(skuId, body == null ? null : body.costAmount());
    }

    /** 读取成本（仅 ADMIN；未维护返回 null，不返回 0）。 */
    @org.springframework.web.bind.annotation.GetMapping("/skus/{skuId}/cost")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public java.util.Map<String, Object> cost(@PathVariable UUID skuId) {
        Long cost = catalogService.cost(skuId);
        return java.util.Collections.singletonMap("costAmount", cost);
    }

    @PatchMapping("/{productId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@PathVariable UUID productId,
                             @Valid @RequestBody ProductStatusUpdateRequest request) {
        catalogService.updateStatus(productId, request.status());
    }
}