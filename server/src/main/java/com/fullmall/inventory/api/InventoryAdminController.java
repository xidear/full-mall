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
 * 库存后台只读接口（不写库、不加锁），读到的数量随时可能过期：扣减/预占是
 * InventoryRepository 的原子条件更新，影响行数不足即失败。
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
     * 批次查询（筛选参数为空即不过滤，未分页）。可售量 = quantity - reserved_quantity，
     * 把 quantity 当可售量看会与下单时的可用性判断对不上。
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