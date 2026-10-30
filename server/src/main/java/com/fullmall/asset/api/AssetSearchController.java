package com.fullmall.asset.api;

import com.fullmall.asset.app.AssetSearchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 素材检索与标注（分页 / 标签 / 关键词 / 语义）。
 *
 * <p>与 AssetController（通用 CRUD）并存：这里只做"找素材"这类专用动作，
 * 避免把标签数组、语义检索塞进通用 CRUD 的 page/sort 语义里。
 */
@RestController
@RequestMapping("/admin/v1/assets")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class AssetSearchController {

    private final AssetSearchService search;
    private final com.fullmall.asset.app.AssetImageIndexService imageIndex;
    private final com.fullmall.asset.app.AssetLocaleVariantService variants;

    public AssetSearchController(AssetSearchService search,
                                 com.fullmall.asset.app.AssetImageIndexService imageIndex,
                                 com.fullmall.asset.app.AssetLocaleVariantService variants) {
        this.search = search;
        this.imageIndex = imageIndex;
        this.variants = variants;
    }

    /** 某素材的语言变体列表（同一逻辑素材在各语言下用哪张图）。 */
    @GetMapping("/{assetId}/variants")
    public List<com.fullmall.asset.app.AssetLocaleVariantService.Variant> listVariants(
            @PathVariable UUID assetId) {
        return variants.list(assetId);
    }

    public record VariantRequest(String url) {
    }

    /** 设置某语言的变体图片（同一素材同一语言只有一条）。 */
    @org.springframework.web.bind.annotation.PutMapping("/{assetId}/variants/{locale}")
    public com.fullmall.asset.app.AssetLocaleVariantService.Variant upsertVariant(
            @PathVariable UUID assetId, @PathVariable String locale,
            @RequestBody VariantRequest body) {
        return variants.upsert(assetId, locale, body == null ? null : body.url());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{assetId}/variants/{locale}")
    public Map<String, Object> deleteVariant(@PathVariable UUID assetId, @PathVariable String locale) {
        variants.delete(assetId, locale);
        return Map.of("deleted", true);
    }

    /** 多模态能力是否就绪（前端据此显示"按图片内容搜索"）。 */
    @GetMapping("/image-search/status")
    public Map<String, Object> imageSearchStatus() {
        return Map.of("available", imageIndex.available());
    }

    /** 用一句话搜图片内容（CLIP：文字与图片同一向量空间）。 */
    @GetMapping("/image-search")
    public List<Map<String, Object>> imageSearch(@RequestParam String q,
                                                @RequestParam(defaultValue = "24") int limit,
                                                @RequestParam(required = false) String providerCode) {
        return imageIndex.search(q, limit, providerCode).stream()
                .map(hit -> Map.<String, Object>of(
                        "asset", search.find(hit.assetId()),
                        "score", Math.round(hit.score() * 1000) / 1000.0))
                .toList();
    }

    /** 给单张素材建图像索引（上传/AI 生成后自动调用，也可手动补建）。 */
    @PostMapping("/{assetId}/image-index")
    public Map<String, Object> indexImage(@PathVariable UUID assetId,
                                         @RequestParam(required = false) String providerCode) {
        imageIndex.index(assetId, providerCode);
        return Map.of("indexed", true, "assetId", assetId);
    }

    /** 全量重建图像索引（耗时与素材数量成正比，管理员手动触发）。 */
    @PostMapping("/image-index/reindex")
    public Map<String, Object> reindexImages(@RequestParam(required = false) String providerCode) {
        return Map.of("indexed", imageIndex.reindexAll(providerCode));
    }

    @GetMapping("/search")
    public AssetSearchService.AssetPage searchAssets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String kind,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return search.search(keyword, tag, purpose, kind, page, size);
    }

    @GetMapping("/tags")
    public List<Map<String, Object>> tags() {
        return search.tags();
    }

    public record MetaRequest(List<String> tags, String caption) {
    }

    @PatchMapping("/{assetId}/meta")
    public AssetSearchService.AssetRow updateMeta(@PathVariable UUID assetId,
                                                 @RequestBody MetaRequest body) {
        return search.updateMeta(assetId, body == null ? List.of() : body.tags(),
                body == null ? null : body.caption());
    }

    /** 用视觉模型识别图片内容，生成描述与标签并重建索引（需要 vision=true 的供应商）。 */
    @PostMapping("/{assetId}/describe")
    public AssetSearchService.DescribeResult describe(@PathVariable UUID assetId,
                                                     @RequestParam(required = false) String providerCode) {
        return search.describeByVision(assetId, providerCode);
    }

    /** 语义检索：自然语言 → 向量 → 最相近的素材（需要 embedding 供应商 + 已建索引）。 */
    @GetMapping("/semantic")
    public List<Map<String, Object>> semantic(@RequestParam String q,
                                             @RequestParam(defaultValue = "24") int limit) {
        return search.semanticSearch(q, limit);
    }

    /** 重建索引（首次启用语义检索、或批量补标签之后）。 */
    @PostMapping("/reindex")
    public Map<String, Object> reindex() {
        return Map.of("indexed", search.reindexAll());
    }
}
