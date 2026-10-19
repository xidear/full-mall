package com.fullmall.promotion.api;

import com.fullmall.promotion.api.PromotionDtos.CreatePromotionRequest;
import com.fullmall.promotion.api.PromotionDtos.GroupBuyView;
import com.fullmall.promotion.api.PromotionDtos.PromotionView;
import com.fullmall.promotion.app.PromotionService;
import com.fullmall.promotion.domain.PromotionStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/promotions")
public class AdminPromotionController {

    private final PromotionService promotionService;

    public AdminPromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public List<PromotionView> list() {
        return promotionService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionView create(@Valid @RequestBody CreatePromotionRequest request) {
        return promotionService.create(request);
    }

    @PostMapping("/{promotionId}/status")
    public PromotionView updateStatus(@PathVariable UUID promotionId,
                                      @RequestParam PromotionStatus status) {
        return promotionService.updateStatus(promotionId, status);
    }

    @GetMapping("/group-buys")
    public List<GroupBuyView> groupBuys(@RequestParam(required = false) UUID promotionId) {
        return promotionService.listGroupBuys(promotionId);
    }
}
