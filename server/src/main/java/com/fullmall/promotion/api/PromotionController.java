package com.fullmall.promotion.api;

import com.fullmall.promotion.api.PromotionDtos.CreateGroupBuyRequest;
import com.fullmall.promotion.api.PromotionDtos.GroupBuyView;
import com.fullmall.promotion.api.PromotionDtos.PromotionView;
import com.fullmall.promotion.app.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/promotions")
    public List<PromotionView> active() {
        return promotionService.listActive();
    }

    @PostMapping("/group-buys")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupBuyView createGroupBuy(@Valid @RequestBody CreateGroupBuyRequest request) {
        return promotionService.createGroupBuy(request);
    }

    @GetMapping("/group-buys/{code}")
    public GroupBuyView groupBuy(@PathVariable String code) {
        return promotionService.findGroupBuy(code);
    }
}
