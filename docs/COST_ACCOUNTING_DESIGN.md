# 成本核算设计（批次成本 + 移动加权平均）

> 纠正：成本**不允许手填**。手填会与库存批次脱节，毛利与砍价底价都不可信。
> 成本由收货/入库自动产生，出库按加权平均结转。

## 1. 两种成本口径

| 口径 | 定义 | 用途 |
|---|---|---|
| **批次成本**（batch cost） | 每个入库批次的单位成本 = 采购单价 + 分摊运费/关税 | FEFO 出库时的真实成本；退货回冲依据 |
| **移动加权平均成本**（weighted average） | 每次入库后重算：`(期初金额 + 入库金额) / (期初数量 + 入库数量)` | 毛利、佣金 PROFIT 模式、砍价底价校验 |

出库**不改变**加权平均单价，只减少库存数量与金额（数量×当前均价）。

## 2. 数据模型（待建）

```
inventory_batch
  + unit_cost_amount BIGINT        -- 批次单位成本（收货时写入）
  + cost_currency CHAR(3)

stock_cost_movement              -- 成本流水，可追溯每一次变化
  id, sku_id, location_type, location_id
  movement_type (RECEIPT|ISSUE|RETURN|ADJUST|WRITE_OFF)
  quantity, unit_cost_amount, amount, currency
  reference_type, reference_id    -- 关联收货单/订单/盘点单
  occurred_at

sku_cost
  sku_id, avg_cost_amount, currency, quantity_on_hand, amount_on_hand, updated_at
```

## 3. 计算规则

1. **收货**：写批次成本 → 更新 `sku_cost`：
   `newAvg = (amountOnHand + qty×unitCost) / (qtyOnHand + qty)`（四舍五入到分，余差留在金额字段）；
2. **出库/销售**：`amountOnHand -= qty × avgCost`，均价不变；数量归零时**保留**最后的均价并标记 `stale=true`；
3. **退货入库**：按**原批次成本**回冲（不是按当前均价），否则退货会凭空产生利润；
4. **盘盈/盘亏**：按当前均价调整金额，均价不变；
5. **报损**：按批次成本或均价（可配），必须留原因。

## 4. 成本未知时的行为（关键）

- `sku_cost` 无记录或 `quantity_on_hand=0 且从未入库` → 成本为**未知**；
- 依赖成本的功能**必须明确拒绝**并提示"该 SKU 无入库成本记录"：
  - 砍价活动启用；
  - 佣金 `PROFIT` 模式结算；
  - 毛利报表（该 SKU 显示"成本未维护"而不是 0）。
- **绝不**用 0 或售价×固定比例兜底。

## 5. 与现有实现的差距

| 现状 | 缺口 |
|---|---|
| V78 有 `sku.cost_amount` | 它是**存储位**，但没有写入来源；手填接口应当下线或限制为 ADMIN 应急修正（需审计） |
| 收货/入库流程 | 未记录单位成本（批次表需加 `unit_cost_amount`） |
| 出库/退货 | 未结转成本 |
| 报表 | 无毛利口径 |

## 6. 实施顺序

1. 迁移：批次加单位成本；建 `stock_cost_movement`、`sku_cost`；
2. 收货接口写入批次成本 + 重算均价（含运费分摊，先按整单均摊）；
3. 销售出库/售后退货按规则结转；
4. 只读成本展示（SKU 详情显示均价 + 最近批次成本 + 来源单据），**不提供手填**；
5. 砍价/佣金 PROFIT/毛利报表改为读 `sku_cost`，缺失即拒绝；
6. 测试：收货两次不同单价 → 均价正确；按均价出库；退货按原批次回冲；无成本时砍价被拒。

## 7. 验收标准

| 编号 | 验收 |
|---|---|
| COST-1 | 两次收货 10 件@10 元、10 件@14 元 → 均价 12 元，金额 240 元 |
| COST-2 | 出库 5 件 → 金额减少 60 元，均价仍 12 元 |
| COST-3 | 退货 5 件回到原批次（成本 10 元）→ 均价按新入库重算，而非直接 12 元 |
| COST-4 | 无入库记录的 SKU → 砍价启用被拒，提示"无入库成本记录" |
| COST-5 | 成本变动写入 `stock_cost_movement`，可按单据追溯 |
