package com.fullmall.catalog.api;

import com.fullmall.catalog.app.SerialNumberService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 序列号（IMEI）：作为**仓储作业字段**管理。
 *
 * <p>不提供"按号追溯卖给了谁"的独立查询——那属于品牌方售后职责。
 * 序列号只在入库登记、批次盘点、发货绑定时使用，随作业单据带出。
 */
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

    /** 入库登记：把序列号登记到指定批次。 */
    @PostMapping("/serials")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> register(@RequestBody RegisterRequest body) {
        return Map.of("created", serials.register(body.batchId(), body.serialNumbers()).created());
    }

    /** 出库绑定：发货时把序列号绑到订单行（校验 SKU 一致、未售出）。 */
    @PostMapping("/order-lines/{orderLineId}/serials")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> bind(@PathVariable UUID orderLineId, @RequestBody BindRequest body) {
        return Map.of("bound", serials.bindToOrderLine(orderLineId, body.serialNumbers()));
    }

    /** 批次序列号清单：盘库/发货核对用（不含顾客信息）。 */
    @GetMapping("/batches/{batchId}/serials")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<Map<String, Object>> listByBatch(@PathVariable UUID batchId) {
        return serials.listByBatch(batchId);
    }
}
