package com.fullmall.inventory.api;

import com.fullmall.inventory.app.ExpiryAlertService;
import com.fullmall.inventory.app.InventoryService;
import com.fullmall.inventory.domain.InventoryBatch;
import com.fullmall.inventory.domain.LocationType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 库存后台只读接口：批次查询与临期清单，均不写库、不加锁，结果不参与下单判定。
 *
 * <p>真正的扣减/预占是 {@code InventoryRepository} 里的原子条件更新（{@code WHERE quantity - reserved_quantity >= ?}，
 * 影响行数不足即失败），所以这里读到的数量随时可能已过期。
 */
@RestController
@RequestMapping("/admin/v1/inventory/batches")
public class InventoryAdminController {

    private final InventoryService inventoryService;
    private final ExpiryAlertService expiryAlerts;

    public InventoryAdminController(InventoryService inventoryService, ExpiryAlertService expiryAlerts) {
        this.inventoryService = inventoryService;
        this.expiryAlerts = expiryAlerts;
    }

    /**
     * 批次查询：四个筛选参数都可为空（为空即不过滤），返回结果未分页。
     *
     * <p>每条批次的 {@code quantity} 是账面在库，{@code reserved_quantity} 是已被订单预占但未出库，
     * 可售量 = 两者之差；后台不要把 quantity 当可售量看，否则会跟下单时的可用性判断对不上。
     */
    @GetMapping
    public List<InventoryBatch> list(
            @RequestParam(required = false) LocationType locationType,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID skuId,
            @RequestParam(required = false) Integer expiringInDays) {
        return inventoryService.findBatches(locationType, locationId, skuId, expiringInDays);
    }

    /**
     * 临期/已过期批次清单（后台「临期商品」页）。
     *
     * <p>前端一直调用本路径，但后端没实现 → 404，页面只能弹一条 toast。
     * 复用每日巡检的同一套查询，避免"后台看到的临期清单"和"告警里的临期清单"不一致。
     *
     * <p>注意 {@code limit} 是**返回行数上限**（服务端夹取 1..500），不是"临期天数"；
     * 临期窗口固定取配置 {@code app.inventory.expiry-warn-days}（默认 30 天），
     * 这样页面与每日告警不会给出两个不同的"临期"口径。
     */
    @GetMapping("/expiring")
    public List<java.util.Map<String, Object>> expiring(
            @RequestParam(defaultValue = "200") int limit) {
        return expiryAlerts.expiringBatches(limit);
    }
}