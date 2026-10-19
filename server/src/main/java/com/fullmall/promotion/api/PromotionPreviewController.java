package com.fullmall.promotion.api;

import com.fullmall.promotion.app.PromotionPreviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** 促销试算：下单前预览命中的促销与应付金额（PRO-008）。 */
@RestController
@RequestMapping("/api/v1/promotions")
public class PromotionPreviewController {

    private final PromotionPreviewService previews;

    public PromotionPreviewController(PromotionPreviewService previews) {
        this.previews = previews;
    }

    public record PreviewItem(@NotNull UUID skuId, @Min(1) int quantity) {
    }

    public record PreviewRequest(@NotEmpty List<@Valid PreviewItem> items, String locale, String channel) {
    }

    @PostMapping("/preview")
    public PromotionPreviewService.Preview preview(@Valid @RequestBody PreviewRequest request) {
        String channel = request.channel() == null || request.channel().isBlank()
                ? "ONLINE" : request.channel().trim().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.List.of("ONLINE", "POS", "PICKUP").contains(channel)) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "INVALID_CHANNEL", "渠道只能是 ONLINE / POS / PICKUP");
        }
        return previews.preview(
                request.items().stream()
                        .map(item -> new PromotionPreviewService.Item(item.skuId(), item.quantity()))
                        .toList(),
                request.locale(), channel);
    }
}
