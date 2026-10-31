# 第四轮证据：订单汇率快照 / 积分按币种 / 测试债清零

## 1. 订单汇率快照（V6）

```text
V6__order_fx_snapshot：
  orders.fx_rate / fx_base_currency / fx_base_total_amount / fx_rate_at

PostgresPickupFlowTest（真实 checkout）断言：
  fxRate == 1
  fxBaseCurrency == "CNY"
  fxBaseTotalAmount == total.amount

无汇率时：
  订单照常创建；写审计 ORDER_FX_RATE_MISSING；
  后台订单详情显示「未快照（无可用汇率）」——不把缺失当成 1:1
```

配置：`app.fx.base-currency = ${APP_FX_BASE_CURRENCY:CNY}`；缺失/空启动失败。

## 2. 积分按币种

```text
PosCalculator.pointsEarned(payable, currency)
MemberRewardService.accrue(orderId, phone, paidAmount, currency)

CNY 3980 分 → 39 分（原 24 分断言已按新规则更新为 39）
JPY 3980 日元 → 3980 分
KWD 3980 第纳尔最小单位 → 3 分（3 位小数）
```

测试：`PosCalculatorTest` 7 项、`MemberRewardRollbackTest` 4 项全绿。

## 3. 供应商结算多币种换算

```text
SupplierSettlementService：
  - 多币种周期不再直接拒绝；
  - 每个币种按生效汇率折算到 app.fx.base-currency；
  - 缺汇率 → 409 SETTLEMENT_FX_RATE_MISSING（不按 1:1 兜底）
```

测试：`SupplierSettlementMultiCurrencyTest` 2 项（多币种折算 + 缺汇率失败）全绿；
`SettlementReturnDeductionTest` 同批通过。

## 4. 测试债清零

后端（7 类）：
```text
ExportJobServiceTest / ExportSchedulerTest / ScheduledTaskServiceTest /
MaintenanceServiceTest / IntegrationConfigServiceTest /
AiMediaPipelineTest / AiProviderPayloadTest / MediaUploadEndpointTest
→ 通过 integration_config 测试夹具、qiniu bucket、MP4 去重桩修正后全部通过
```

Playwright（6 个历史用例）：
```text
admin-pickers（唯一 PNG 避免 E2E 素材被过滤）
audit-log（事件类型改下拉）
audit-snapshot（实体类型下拉 + 定位到 tabpanel）
export-center（状态标签改「已完成」）
export-job（导出目录修正为容器内路径）
translations（多语言标签 + 语言列表）
→ 全部通过
```

## 5. 环境修复

```text
export 集成目录：/data/full-mall/server/data/exports
  → /repo/server/data/exports（容器内路径）
```

## 6. 完整回归

- 后端全量：**810 tests, 0 failures, 0 errors**（`mvn clean test`，243 个测试类）。
- Playwright：**62 passed / 0 failed / 3 skipped**（全量，2.8m）。