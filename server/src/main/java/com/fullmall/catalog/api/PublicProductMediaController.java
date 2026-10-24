package com.fullmall.catalog.api;

import com.fullmall.catalog.app.ProductMediaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 前台商品素材（主图/图集/详情图/视频），按角色与排序输出。 */
@RestController
@RequestMapping("/api/v1/products/{productId}/media")
public class PublicProductMediaController {

    private final ProductMediaService media;

    public PublicProductMediaController(ProductMediaService media) {
        this.media = media;
    }

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable UUID productId) {
        return media.list(productId);
    }
}
