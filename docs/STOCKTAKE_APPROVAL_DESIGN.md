# 盘点差异审批设计（F3）

## 现状（已实现）

- 建盘点单：把该地点所有批次快照成盘点行（系统数量）；
- 确认盘点：逐行填实盘数 → 立即写 `variance` 并按实盘数改批次数量 + 写 `STOCKTAKE_ADJUST` 流水；
- **缺口**：确认即调账，没有审批；差异大时无法拦。

## 目标流程

```
建单(DRAFT) → 录实盘(CONFIRMED) → 【差异超过阈值 → PENDING_APPROVAL】→ 审批通过(APPROVED，执行调账) / 驳回(REJECTED，不调账)
                              └─ 差异在阈值内 → 直接 APPROVED（沿用现有行为）
```

## 关键规则

| 规则 | 说明 |
|---|---|
| 阈值 | `app.stocktake.approval-threshold`（默认 5 件；按行绝对值判断） |
| 仅大差异需审批 | 小额差异直接调账，避免运营每单都点审批 |
| 驳回不调账 | 批次数量保持系统数量，差异记录保留供复盘 |
| 审批人 | 需 ADMIN（`stocktake.approve` 权限） |
| 幂等 | 审批只能一次；重复审批拒绝 |
| 流水 | 审批通过才写 `STOCKTAKE_ADJUST`，驳回写事件说明 |

## 数据改动（待执行）

```sql
ALTER TABLE stocktake ADD COLUMN approval_status VARCHAR(16) NOT NULL DEFAULT 'NONE';
ALTER TABLE stocktake ADD COLUMN approved_at TIMESTAMPTZ;
ALTER TABLE stocktake ADD COLUMN approved_by VARCHAR(64);
-- 盘点行保留 counted_quantity / variance（已有）
```

## 实施步骤

1. 迁移加列 + 状态约束；
2. `confirmStocktake`：计算最大差异，超阈值 → 只落盘点行、不改批次、状态 `PENDING_APPROVAL`；否则原逻辑；
3. `approveStocktake` / `rejectStocktake`：通过时按盘点行调账并写流水，驳回只写事件；
4. 单测：小差异直接调账；大差异挂起不改库存；审批通过后库存与流水一致；驳回后库存不变；重复审批被拒；
5. 前端：盘点列表显示 `待审批` 标记与差异合计，提供通过/驳回按钮。

## 为什么拆两步

确认即调账在生产上等于"盘点人自己改库存"，出现内盗或误点时无法追溯。加审批不是流程官僚，而是**把"发现差异"和"改动库存"两个动作分开**。
