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

    public record CreateReview(@NotNull UUID orderId, @NotNull UUID skuId,
                               @Min(1) @Max(5) int rating, String content) {
    }

    @GetMapping("/api/v1/products/{skuId}/reviews")
    public List<ProductReviewService.Review> list(@PathVariable UUID skuId,
                                                  @RequestParam(defaultValue = "20") int limit) {
        return reviews.listForSku(skuId, limit);
    }

    @PostMapping("/api/v1/products/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> create(@RequestBody CreateReview body, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "MEMBER_REQUIRED", "需要会员登录");
        }
        UUID id = reviews.create(UUID.fromString(authentication.getName()),
                body.orderId(), body.skuId(), body.rating(), body.content());
        return Map.of("id", id);
    }
}
