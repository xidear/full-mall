package com.fullmall.catalog.api;

import com.fullmall.catalog.app.CatalogService;
import com.fullmall.catalog.domain.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ProductPage list(
            @RequestParam(defaultValue = "zh-CN") String locale,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return catalogService.listProducts(locale, keyword, categoryId, page, size, sort, direction);
    }

    /**
     * 商品富文本详情（商城详情页用）。
     *
     * <p>单独一个接口而不是塞进 Product：详情是大字段，列表接口不需要；
     * 且它要按 locale 取"已发布译文"，与商品主数据的读取规则不同。
     */
    @GetMapping("/{productId}/detail")
    public java.util.Map<String, Object> detail(@PathVariable UUID productId,
                                               @RequestParam(defaultValue = "zh-CN") String locale) {
        String html = catalogService.detailHtmlFor(productId, locale);
        return java.util.Map.of("html", html == null ? "" : html);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> get(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "zh-CN") String locale) {
        return ResponseEntity.ok(catalogService.getProduct(productId, locale));
    }
}