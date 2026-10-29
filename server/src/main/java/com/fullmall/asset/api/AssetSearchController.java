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

    public AssetSearchController(AssetSearchService search) {
        this.search = search;
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
