# ADR-0005: 库存粒度、批次与保质期

- 状态：Accepted
- 日期：2026-10-18

## 背景

门店和仓库中存在保质期商品，同一 SKU 的不同批次效期不同；部分商品的库存管理不需要细分规格。拣货时需要按类目决定是否优先临期批次。

## 决策

- 库存原子单位：`商品/SKU + 地点（仓库/门店/供应商虚拟仓） + 批次（可选）`。
- 商品配置 `inventory_tracking_level`：
  - `VARIANT`：默认，按 SKU 管理。
  - `PRODUCT`：按 SPU 管理，不细分规格，适用于部分商品。
- 商品/类目配置 `shelf_life_policy`：
  - `batch_tracking`：是否强制批次。
  - `picking_strategy`：`FEFO` / `FIFO` / `NONE`。
  - `min_remaining_shelf_life_days`：销售/发货时最低剩余保质期。
  - `near_expiry_threshold_days`：临期阈值。
  - `near_expiry_action`：正常、折扣、下架、报损。
  - `auto_block_expired`：过期自动阻断。
- `stock_move` 不可变；`stock_level` 是投影；预占有 TTL；扣减使用原子更新或行锁。
- 收货、调拨、盘点、退货、报损全部按批次记录。
- 拣货时系统给出建议批次，门店扫码校验；FEFO 类目优先早效期。

## 后果

- 数据模型和 UI 比普通 SKU 库存复杂，但能支撑门店保质期管理。
- 需要批次效期看板、FEFO 合规率和报损分析。
- 非保质期商品可以不启用批次，避免无谓操作。

## 替代方案

- 只按 SKU 管理：无法满足门店效期和临期优先。
- 只按批次管理所有商品：操作成本过高。