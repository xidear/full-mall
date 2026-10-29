package com.fullmall.review.api;

import com.fullmall.review.app.ProductReviewService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 商品评价：公开列表 + 会员发表。 */
@RestController
public class ProductReviewController {

    private final ProductReviewService reviews;

    public ProductReviewController(ProductReviewService reviews) {
        this.reviews = reviews;
    }

    /** mediaUrls：先调 POST /api/v1/media 上传拿到 URL，再把 URL 随评价一起提交；条数上限见 ValidationRules.REVIEW_MEDIA_MAX。 */
    public record CreateReview(@NotNull UUID orderId, @NotNull UUID skuId,
                               @Min(1) @Max(5) int rating, String content,
                               List<String> mediaUrls) {
    }

    @GetMapping("/api/v1/products/{skuId}/reviews")
    public List<ProductReviewService.Review> list(@PathVariable UUID skuId,
                                                  @RequestParam(defaultValue = "20") int limit) {
        return reviews.listForSku(skuId, limit);
    }

    /** 后台评价列表（含已隐藏）。 */
    /** 后台评价 keyset 分页（深页推荐）。 */
    @GetMapping("/admin/v1/reviews/keyset")
    public Map<String, Object> adminKeyset(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) String cursorCreatedAt,
                                           @RequestParam(required = false) UUID cursorId,
                                           @RequestParam(defaultValue = "20") int size) {
        return reviews.listForAdminKeyset(status, cursorCreatedAt, cursorId, size);
    }

    @GetMapping("/admin/v1/reviews")
    public Map<String, Object> adminList(@RequestParam(required = false) String status,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return Map.of(
                "items", reviews.listForAdmin(status, page, size),
                "page", Math.max(page, 0),
                "size", size,
                "total", reviews.countForAdmin(status));
    }

    public record ModerateReview(@NotNull String status) {
    }

    /** 隐藏或恢复评价（PUBLISHED ↔ HIDDEN）。 */
    @PostMapping("/admin/v1/reviews/{reviewId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moderate(@PathVariable UUID reviewId, @RequestBody ModerateReview request) {
        reviews.moderate(reviewId, request.status());
    }

    @PostMapping("/api/v1/products/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> create(@RequestBody CreateReview body, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "MEMBER_REQUIRED", "需要会员登录");
        }
        UUID id = reviews.create(UUID.fromString(authentication.getName()),
                body.orderId(), body.skuId(), body.rating(), body.content(), body.mediaUrls());
        return Map.of("id", id);
    }
}
