# 第三轮证据：金额精度 / 汇率 / 软删除 / 超时 / 完整测试

采集时间：2026-10-31（Asia/Shanghai）

## 1. 金额精度

```text
GET /api/v1/currencies/precision
→ {"defaultMinorUnits":2,"overrides":{"JPY":0,...,"KWD":3,...}}

Money.toMajor(1234,"JPY")  = "1234"
Money.toMajor(1234,"USD")  = "12.34"
Money.toMajor(1234,"KWD")  = "1.234"
Money.parseMajor("1.234","USD") → ArithmeticException（不静默截断）

CurrencyPrecision: TWD=2, HUF=2（旧实现误写 0，已修正）
```

前端：`web/packages/api-client/src/money.ts` 统一 `currencyDigits/toMajor/toMinor/formatMoney/formatAmount`；
admin 源码中金额 `/100` 已清零（仅剩税率 bps、折扣百分比等非金额换算）；storefront/store-console/
supplier-portal/support-console/mobile 同步。

## 2. 汇率（阶段 1）

```text
PUT /admin/v1/exchange-rates
{"baseCurrency":"USD","quoteCurrency":"CNY","rate":7.2,"source":"TEST","effectiveAt":"..."}
→ 201

GET /admin/v1/exchange-rates/convert?amount=10000&from=USD&to=CNY
→ 200 {"amount":10000,"fromCurrency":"USD","convertedAmount":72000,
       "toCurrency":"CNY","rate":7.2,"inverse":false}

USD→JPY 150：10000 分（100 USD）→ 15000 日元（JPY 0 位）
CNY→USD 反向：72000 分 → 10000 分，inverse=true
无汇率：409 EXCHANGE_RATE_NOT_FOUND（不按 1:1 兜底）
```

测试：`ExchangeRateServiceTest` 6 项全绿。

## 3. 门店软删除

```text
V4__store_soft_delete：store.deleted_at + 部分索引
DELETE /admin/v1/stores/{id} → 200 deletedAt 非空、active=false
默认/自提列表不再出现；includeDeleted=true 可见
POST /admin/v1/stores/{id}/restore → 200 deletedAt=null，active 仍为 false
仍绑定员工 → 409 STORE_HAS_STAFF
已删除门店切换状态/编辑 → 409 STORE_DELETED
```

测试：`StoreAdminServiceTest` 7 项全绿（含软删除/恢复/删除后禁改）。

## 4. 模型超时

`OpenAiChatAdapter`：默认 180s，读取供应商 `extra.timeoutSeconds`，范围 5..600；
超范围直接失败；AI 页「模型供应商」可编辑。

## 5. 测试与构建

```text
新增/相关测试：
  MoneyPrecisionTest        5/5
  StoreAdminServiceTest     7/7
  StoreAccessServiceTest    5/5
  ExchangeRateServiceTest   6/6

前端构建：
  admin / store-console / supplier-portal / support-console / storefront 全部 exit 0
```

## 6. 完整后端测试

```text
命令：mvn -B -q -s scripts/maven-settings-aliyun.xml test
总计：Tests run: 808, Failures: 5, Errors: 22（首次跑）

其中由本批改动引入的失败已全部修复并单独复跑通过：
  PostgresPickupFlowTest / PostgresPosFlowTest / PickLineMappingTest
  MigrationSmokeTest / SupportProductReferralTest
  AuditRecoveryRealRunTest / AssetScopeAndAuditTest
（原因：V3 store.code NOT NULL 后，demo seed 与测试 INSERT 没带 code；迁移断言仍写死 V23；
  asset.source NOT NULL 但测试实体未设置）

剩余 12 项失败集中在 7 个历史配置型测试类（与金额/汇率/门店软删除无关）：
  MediaUploadEndpointTest（视频上传 400）
  ExportJobServiceTest / ExportSchedulerTest（测试未启用导出存储）
  ScheduledTaskServiceTest / MaintenanceServiceTest（同上）
  IntegrationConfigServiceTest（qiniu 测试未带 bucket）
  AiMediaPipelineTest / AiProviderPayloadTest（测试未启用素材存储）
这些类需要单独补测试夹具（配置 integration_config），留待下一批，不在本轮伪装成全绿。
```

## 7. Playwright

```text
全量：npx playwright test --project=chromium
首次：52 passed / 10 failed / 3 skipped（8.7m）

其中 4 个失败是测试绑定已变化/已删除的文案，已修测试并单独复跑通过：
  admin.spec（结束促销新增二次确认；主题工作室旧按钮已移除）
  pricing-and-tax.spec ×2（下拉选项与占位文案变化；运费档位选择 SG）
  coupons.spec（券模板实际落 DISABLED，列表显示「已停用」）
  ai-page.spec（内部版有模型状态，不再只显示社区版提示）
新增：e2e/money-and-exchange.spec.ts 3/3 通过（币种精度/汇率/门店软删除列表）

仍失败的 6 个为历史用例（与金额/汇率/软删除无关）：
  admin-pickers / audit-log / audit-snapshot / export-center / export-job / translations
  （导出类依赖未启用的导出存储；审计/译文页面的交互文案与旧测试不一致）
```

截图：`docs/evidence/exchange-rates.png`（汇率页，无控制台错误）。