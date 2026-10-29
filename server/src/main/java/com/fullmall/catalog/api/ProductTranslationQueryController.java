package com.fullmall.catalog.api;

import com.fullmall.catalog.app.ProductTranslationWorkflowService;
import com.fullmall.catalog.infra.ProductTranslationRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 译文查询与 AI 翻译（后台商品编辑页内嵌，不再是独立模块）。
 *
 * <p>「没有译文」是正常状态，用列表接口返回 present=false，而不是让前端把 404 当业务状态；
 * 单语言 GET 仍保留给旧调用方，但新页面不再依赖它的 404 语义。
 */
@RestController
@RequestMapping("/admin/v1/products")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class ProductTranslationQueryController {

    private final ProductTranslationRepository repository;
    private final ProductTranslationWorkflowService workflow;

    public ProductTranslationQueryController(ProductTranslationRepository repository,
                                            ProductTranslationWorkflowService workflow) {
        this.repository = repository;
        this.workflow = workflow;
    }

    @GetMapping("/{productId}/translations/{locale}")
    public ProductTranslationRepository.Row find(@PathVariable UUID productId,
                                                @PathVariable String locale) {
        return repository.find(productId, locale).orElseThrow(() ->
                com.fullmall.platform.error.ApiException.notFound("TRANSLATION_NOT_FOUND",
                        "该商品还没有 " + locale + " 译文"));
    }

    // 实际 SQL 只返回 product_translation 已有行（按 locale 升序），已启用但从未录入的语言不会出现，
    // 前端要拿「已启用语言」做差集才能显示“待翻译”。
    /** 全部译文（含已启用但还没录入的语言）。 */
    @GetMapping("/{productId}/translations")
    public List<ProductTranslationRepository.Row> list(@PathVariable UUID productId) {
        return repository.listByProduct(productId);
    }

    /**
     * 只翻译"富文本详情"：返回译文 HTML + 分段/命中记忆数，并写入草稿的 detail_html。
     *
     * <p>只替换文本节点，标签/图片/视频原样保留；改一段只翻一段（未变的段命中翻译记忆，不计费）。
     */
    @PostMapping("/{productId}/translations/{locale}/auto-translate-detail")
    public com.fullmall.catalog.app.RichTextTranslationService.Result autoTranslateDetail(
            @PathVariable UUID productId, @PathVariable String locale,
            Authentication authentication) {
        return workflow.autoTranslateDetail(productId, locale, authentication.getName());
    }

    /** AI 翻译：用默认语言生成目标语言草稿（DRAFT，需审核后发布）。 */
    /**
     * AI 翻译入口：外部 AI 失败直接抛出（不吞、不拿原文兜底），写入发生在拿到译文之后，重试安全。
     * 翻译记忆按 源文哈希 + locale + 术语版本 去重，重复调用不重复计费；发布前顾客看不到。
     */
    @PostMapping("/{productId}/translations/{locale}/auto-translate")
    public ProductTranslationRepository.Row autoTranslate(@PathVariable UUID productId,
                                                         @PathVariable String locale,
                                                         Authentication authentication) {
        return workflow.autoTranslate(productId, locale, authentication.getName());
    }
}
