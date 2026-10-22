package com.fullmall.catalog.api;

import com.fullmall.catalog.app.SerialNumberService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 单件序列号（IMEI/唯一码）：登记、出库绑定、按号追溯。 */
@RestController
@RequestMapping("/admin/v1")
public class AdminSerialController {

    private final SerialNumberService serials;

    public AdminSerialController(SerialNumberService serials) {
        this.serials = serials;
    }

    public record RegisterRequest(UUID batchId, List<String> serialNumbers) {
    }

    public record BindRequest(List<String> serialNumbers) {
    }

    /** 入库登记：把序列号登记到指定批次（数量不能超批次现存）。 */
    @PostMapping("/serials")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> register(@RequestBody RegisterRequest body) {
        var result = serials.register(body.batchId(), body.serialNumbers());
        return Map.of("created", result.created());
    }

    /** 出库绑定：把序列号绑到订单行（校验 SKU 一致、未售出）。 */
    @PostMapping("/order-lines/{orderLineId}/serials")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> bind(@PathVariable UUID orderLineId, @RequestBody BindRequest body) {
        return Map.of("bound", serials.bindToOrderLine(orderLineId, body.serialNumbers()));
    }

    /** 按序列号追溯：在哪批、什么状态、卖给哪张订单行。 */
    @GetMapping("/serials/{serialNo}")
    public Map<String, Object> lookup(@PathVariable String serialNo) {
        return serials.lookup(serialNo);
    }

    /** 支持以 query 形式查询（条码枪扫出来常带前缀）。 */
    @GetMapping("/serials")
    public Map<String, Object> lookupByQuery(@RequestParam String serialNo) {
        return serials.lookup(serialNo);
    }
}
