package com.fullmall.catalog.api;

import org.springframework.web.bind.annotation.PutMapping;
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
    private final com.fullmall.catalog.app.SkuMatrixService skuMatrixService;
    private final com.fullmall.catalog.app.SkuMatrixMultiService skuMatrixMultiService;
    private final com.fullmall.catalog.app.SkuPriceRuleService skuPriceRuleService;
    private final com.fullmall.catalog.app.SkuOptionsMigrationService skuOptionsMigrationService;
    private final com.fullmall.catalog.app.ProductCsvImporter productCsvImporter;
    private final com.fullmall.catalog.app.ProductTranslationWorkflowService translationWorkflow;

    public AdminCatalogController(CatalogService catalogService,
                                  com.fullmall.catalog.app.ProductTranslationWorkflowService translationWorkflow,
                                  com.fullmall.catalog.app.SkuCostService skuCostService,
                                  com.fullmall.catalog.app.SkuMatrixService skuMatrixService,
                                  com.fullmall.catalog.app.SkuMatrixMultiService skuMatrixMultiService,
                                  com.fullmall.catalog.app.SkuPriceRuleService skuPriceRuleService,
                                  com.fullmall.catalog.app.SkuOptionsMigrationService skuOptionsMigrationService,
                                  com.fullmall.catalog.app.ProductCsvImporter productCsvImporter) {
        this.catalogService = catalogService;
        this.translationWorkflow = translationWorkflow;
        this.skuCostService = skuCostService;
        this.skuMatrixService = skuMatrixService;
        this.skuMatrixMultiService = skuMatrixMultiService;
        this.skuPriceRuleService = skuPriceRuleService;
        this.skuOptionsMigrationService = skuOptionsMigrationService;
        this.productCsvImporter = productCsvImporter;
    }

    /**
     * 成本只读视图（仅 ADMIN）：均价 + 最近批次成本 + 来源。
     *
     * <p>无成本记录时 `unknown=true`，**不返回 0**，避免毛利被算成 100%。
     */
    @GetMapping("/skus/{skuId}/cost-view")
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
    public record TranslationDraft(String title, String description) {
    }

    /** 录入译文（运营手填或机翻结果），进入 DRAFT 待审核。 */
    @PutMapping("/{productId}/translations/{locale}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public com.fullmall.catalog.infra.ProductTranslationRepository.Row saveTranslation(
            @PathVariable UUID productId, @PathVariable String locale,
            @RequestBody TranslationDraft body,
            org.springframework.security.core.Authentication authentication) {
        return translationWorkflow.saveDraft(productId, locale, body.title(), body.description(),
                authentication.getName());
    }

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
    @PutMapping("/skus/{skuId}/cost")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public void updateCost(@PathVariable UUID skuId,
                           @RequestBody CostRequest body) {
        catalogService.updateCost(skuId, body == null ? null : body.costAmount());
    }

    /** 读取成本（仅 ADMIN；未维护返回 null，不返回 0）。 */
    @GetMapping("/skus/{skuId}/cost")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public java.util.Map<String, Object> cost(@PathVariable UUID skuId) {
        Long cost = catalogService.cost(skuId);
        return java.util.Collections.singletonMap("costAmount", cost);
    }

    public record MatrixRequest(String prefix, String optionName, java.util.List<String> values,
                                long priceAmount, String currency) {
    }

    /** 规格矩阵批量生成 SKU（幂等：已存在编码跳过）。 */
    @PostMapping("/{productId}/sku-matrix")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public com.fullmall.catalog.app.SkuMatrixService.Result generateMatrix(
            @PathVariable UUID productId, @RequestBody MatrixRequest body) {
        return skuMatrixService.generate(productId, body.prefix(), body.optionName(),
                body.values(), body.priceAmount(), body.currency());
    }

    public record BatchPriceItem(java.util.UUID skuId, long priceAmount) {
    }

    public record BatchPriceRequest(java.util.List<BatchPriceItem> items) {
    }

    /** SKU 批量改价（整批成功或整批失败）。 */
    @PostMapping("/skus/batch-price")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public java.util.Map<String, Object> batchPrice(@RequestBody BatchPriceRequest body) {
        java.util.Map<java.util.UUID, Long> prices = new java.util.LinkedHashMap<>();
        for (BatchPriceItem item : body.items()) {
            prices.put(item.skuId(), item.priceAmount());
        }
        int updated = catalogService.batchUpdatePrices(prices);
        return java.util.Map.of("updated", updated);
    }

    public record BatchStatusRequest(java.util.List<java.util.UUID> productIds, String status) {
    }

    /** 商品批量上下架（整批成功或整批失败）。 */
    @PostMapping("/batch-status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public java.util.Map<String, Object> batchStatus(@RequestBody BatchStatusRequest body) {
        com.fullmall.catalog.domain.ProductStatus target;
        try {
            target = com.fullmall.catalog.domain.ProductStatus.valueOf(
                    body.status() == null ? "" : body.status().trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "BATCH_STATUS_INVALID", "状态只能是 DRAFT / ACTIVE / ARCHIVED");
        }
        int updated = catalogService.batchUpdateStatus(body.productIds(), target);
        return java.util.Map.of("updated", updated);
    }

    public record MatrixMultiRequest(String prefix,
                                     java.util.Map<String, java.util.List<String>> dimensions,
                                     java.util.List<java.util.Map<String, String>> disabled,
                                     long priceAmount) {
    }

    /** 把既有 options_json 回填到关联表（ADR-003 第 2 步）。 */
    @PostMapping("/{productId}/sku-options/backfill")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public com.fullmall.catalog.app.SkuOptionsMigrationService.Result backfillSkuOptions(
            @PathVariable UUID productId) {
        return skuOptionsMigrationService.backfillProduct(productId);
    }

    /** 多维度规格生成（颜色 × 内存 × 存储，支持组合禁用）。 */
    @PostMapping("/{productId}/sku-matrix-multi")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public com.fullmall.catalog.app.SkuMatrixMultiService.Result generateMatrixMulti(
            @PathVariable UUID productId, @RequestBody MatrixMultiRequest body) {
        return skuMatrixMultiService.generate(productId, body.prefix(), body.dimensions(),
                body.disabled(), body.priceAmount());
    }

    /** 各 SKU 可用量（前台禁用缺货组合用）。 */
    @GetMapping("/{productId}/availability")
    public java.util.List<java.util.Map<String, Object>> availability(@PathVariable UUID productId) {
        return catalogService.availability(productId);
    }

    public record PriceRuleRequest(java.util.Map<String, String> match, String mode, long value) {
    }

    private com.fullmall.catalog.app.SkuPriceRuleService.Mode priceMode(String raw) {
        try {
            return com.fullmall.catalog.app.SkuPriceRuleService.Mode.valueOf(
                    raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "SKU_PRICE_MODE_INVALID", "模式只能是 ADD 或 SET");
        }
    }

    /** 预览：只计算不落库（改价前必须看一眼）。 */
    @PostMapping("/{productId}/price-rule/preview")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public java.util.List<com.fullmall.catalog.app.SkuPriceRuleService.Change> previewPriceRule(
            @PathVariable UUID productId, @RequestBody PriceRuleRequest body) {
        return skuPriceRuleService.preview(productId, body.match(), priceMode(body.mode()),
                body.value());
    }

    /** 应用：真正落库，返回改动条数。 */
    @PostMapping("/{productId}/price-rule/apply")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public java.util.Map<String, Object> applyPriceRule(
            @PathVariable UUID productId, @RequestBody PriceRuleRequest body) {
        int updated = skuPriceRuleService.apply(productId, body.match(), priceMode(body.mode()),
                body.value());
        return java.util.Map.of("updated", updated);
    }

    public record ImportCsv(String content) {
    }

    /** 商品 CSV 导入（title,skuCode,price）。任一行不合法则整批不落库。 */
    @PostMapping("/products/import")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public com.fullmall.catalog.app.ProductCsvImporter.Result importCsv(@RequestBody ImportCsv body) {
        return productCsvImporter.importCsv(body.content());
    }

    public record BasicsRequest(String title, String description, UUID categoryId) {
    }

    /** 编辑商品基础信息（标题/描述/类目）。 */
    @PutMapping("/{productId}")
    public java.util.Map<String, Object> updateBasics(@PathVariable UUID productId,
                                                      @RequestBody BasicsRequest body) {
        catalogService.updateBasics(productId, body.title(), body.description(), body.categoryId());
        return java.util.Map.of("updated", true);
    }

    @PatchMapping("/{productId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@PathVariable UUID productId,
                             @Valid @RequestBody ProductStatusUpdateRequest request) {
        catalogService.updateStatus(productId, request.status());
    }
}