package com.fullmall.catalog.api;

import com.fullmall.catalog.infra.ProductTranslationRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** 译文查询（后台编辑页回显用）：没有译文时返回 404，让前端显示空表单。 */
@RestController
@RequestMapping("/admin/v1/products")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class ProductTranslationQueryController {

    private final ProductTranslationRepository repository;

    public ProductTranslationQueryController(ProductTranslationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{productId}/translations/{locale}")
    public ProductTranslationRepository.Row find(@PathVariable UUID productId,
                                                @PathVariable String locale) {
        return repository.find(productId, locale).orElseThrow(() ->
                com.fullmall.platform.error.ApiException.notFound("TRANSLATION_NOT_FOUND",
                        "该商品还没有 " + locale + " 译文"));
    }
}
