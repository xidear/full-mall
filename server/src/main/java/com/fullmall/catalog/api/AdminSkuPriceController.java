package com.fullmall.catalog.api;

import com.fullmall.catalog.app.SkuPricingService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 按市场定价（ADR-0013 下游）：覆盖价管理。不写覆盖价时按 SKU 基础价（币种需与市场一致）。 */
@RestController
public class AdminSkuPriceController {

    private final SkuPricingService pricing;

    public AdminSkuPriceController(SkuPricingService pricing) {
        this.pricing = pricing;
    }

    public record PriceBody(@NotBlank String currency, @Min(1) long amount, Long compareAtAmount) {
    }

    @GetMapping("/admin/v1/skus/{skuId}/prices")
    public List<SkuPricingService.PriceRow> list(@PathVariable UUID skuId) {
        return pricing.list(skuId);
    }

    @PutMapping("/admin/v1/skus/{skuId}/prices/{marketCode}")
    public List<SkuPricingService.PriceRow> upsert(@PathVariable UUID skuId,
                                                  @PathVariable String marketCode,
                                                  @RequestBody PriceBody body) {
        pricing.upsert(skuId, marketCode, body.currency(), body.amount(), body.compareAtAmount());
        return pricing.list(skuId);
    }

    @DeleteMapping("/admin/v1/skus/{skuId}/prices/{marketCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID skuId, @PathVariable String marketCode) {
        pricing.remove(skuId, marketCode);
    }
}
