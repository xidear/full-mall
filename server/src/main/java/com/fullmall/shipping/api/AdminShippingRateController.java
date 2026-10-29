package com.fullmall.shipping.api;

import com.fullmall.shipping.app.ShippingRateService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 运费档位管理（市场 × 目的地 × 重量）。 */
@RestController
public class AdminShippingRateController {

    private final ShippingRateService shipping;

    public AdminShippingRateController(ShippingRateService shipping) {
        this.shipping = shipping;
    }

    public record RateBody(@NotBlank String marketCode, String destinationCountry,
                           @Min(0) int minWeightGrams, Integer maxWeightGrams,
                           @Min(0) long feeAmount, Long freeOverAmount) {
    }

    @GetMapping("/admin/v1/shipping-rates")
    public List<ShippingRateService.RateRow> list(@RequestParam(required = false) String market) {
        return shipping.list(market);
    }

    /** id 为空 = 新建；带上已有 id = 更新。币种由后端按市场取，不接受前端传。 */
    @PutMapping({"/admin/v1/shipping-rates", "/admin/v1/shipping-rates/{id}"})
    public List<ShippingRateService.RateRow> upsert(@PathVariable(required = false) String id,
                                                   @RequestBody RateBody body) {
        String target = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        shipping.upsert(target, body.marketCode(), body.destinationCountry(), body.minWeightGrams(),
                body.maxWeightGrams(), body.feeAmount(), body.freeOverAmount());
        return shipping.list(body.marketCode());
    }

    @DeleteMapping("/admin/v1/shipping-rates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        shipping.delete(id);
    }
}
