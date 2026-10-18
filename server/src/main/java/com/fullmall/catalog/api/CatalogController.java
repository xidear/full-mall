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
            @RequestParam(defaultValue = "20") int size) {
        return catalogService.listProducts(locale, keyword, categoryId, page, size);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> get(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "zh-CN") String locale) {
        return ResponseEntity.ok(catalogService.getProduct(productId, locale));
    }
}