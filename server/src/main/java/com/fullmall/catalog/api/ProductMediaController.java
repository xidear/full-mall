package com.fullmall.catalog.api;

import com.fullmall.catalog.app.ProductMediaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 商品素材编排：主图/图集/详情图/视频/规格专用图。 */
@RestController
@RequestMapping("/admin/v1/products/{productId}/media")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class ProductMediaController {

    private final ProductMediaService media;

    public ProductMediaController(ProductMediaService media) {
        this.media = media;
    }

    public record AttachRequest(UUID skuId, UUID assetId, String role, Integer sortOrder,
                                String source) {
    }

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable UUID productId) {
        return media.list(productId);
    }

    @PostMapping
    public Map<String, Object> attach(@PathVariable UUID productId,
                                      @RequestBody AttachRequest body,
                                      Authentication authentication) {
        UUID id = media.attach(productId, body.skuId(), body.assetId(), body.role(),
                body.sortOrder() == null ? 0 : body.sortOrder(), body.source(),
                authentication.getName());
        return Map.of("id", id.toString());
    }

    @DeleteMapping("/{mediaId}")
    public Map<String, Object> detach(@PathVariable UUID productId, @PathVariable UUID mediaId) {
        media.detach(productId, mediaId);
        return Map.of("removed", true);
    }
}
