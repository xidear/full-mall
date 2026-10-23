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
    private final com.fullmall.promotion.app.SeckillGate seckillGate;

    public PromotionPreviewController(PromotionPreviewService previews,
                                      com.fullmall.promotion.app.SeckillGate seckillGate) {
        this.previews = previews;
        this.seckillGate = seckillGate;
    }

    public record PreviewItem(@NotNull UUID skuId, @Min(1) int quantity) {
    }

    public record PreviewRequest(@NotEmpty List<@Valid PreviewItem> items, String locale, String channel) {
    }

    /**
     * 秒杀场景的试算入口：先过闸门（限流 + 并发上限），再进业务。
     *
     * <p>只在 `channel=POS` 之外的秒杀压测路径启用：默认试算不加限流，
     * 通过查询参数 `seckill=1` 显式开启，避免影响普通浏览。
     */
    @PostMapping("/preview")
    public PromotionPreviewService.Preview preview(@Valid @RequestBody PreviewRequest request,
                                                   @RequestParam(required = false) String seckill) {
        if ("1".equals(seckill)) {
            String key = request.channel() == null ? "ONLINE" : request.channel();
            seckillGate.acquire(key);
            seckillGate.enter(key);
            try {
                return previews.preview(request.items().stream()
                                .map(item -> new PromotionPreviewService.Item(item.skuId(), item.quantity()))
                                .toList(),
                        request.locale(), key);
            } finally {
                seckillGate.release(key);
            }
        }
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
