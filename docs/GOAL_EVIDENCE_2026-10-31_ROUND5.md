# 第五轮证据：自动汇率 / 退款汇率快照 / 金额去浮点

## 1. 自动汇率（阶段 2）

```text
能力：integration_config.capability = exchange_rate
供应商：
  mock        演示固定汇率（CNY 基准）
  frankfurter Frankfurter / ECB，免费、无 API Key
              可配 base-url（默认 https://api.frankfurter.app）

行为：
  生产未配置         → 不自动拉取；手工汇率可用；应用正常启动
  demo 未配置        → 才用 mock 兜底（dev/prod 未配置=不自动拉取）
  配置且拉取成功     → 写入 source=frankfurter/mock 的新汇率（生效时间=现在）
  配置但拉取失败     → 不写/不删任何汇率，保留上次自动或手工汇率；记录 lastError
  定时任务           → 默认 1 小时；也可在后台「汇率」页点「立即拉取」

接口：
  GET  /admin/v1/exchange-rates/provider
  POST /admin/v1/exchange-rates/refresh
```

数据库：`exchange_rate_fetch_state`（单行）保存供应商、上次尝试/成功、失败原因。

测试：`ExchangeRateFetchServiceTest` 验证 demo mock 状态与写入 `0.1389 USD/CNY`；
`ExchangeRateServiceTest` 6 项继续通过。

## 2. 退款汇率快照（V7）

```text
order_line_refund 新增：
  fx_rate / fx_base_currency / fx_base_amount / fx_rate_at

退款金额始终是原订单币种；不强转、不跨币种分账。
允许退款时汇率与下单时不同：退款快照单独记录退款时点汇率。
无汇率：不阻塞退款，写 ORDER_REFUND_FX_RATE_MISSING 审计。
```

测试：`LineRefundServiceTest` 8 项、`LineRefundQuantityTest` 2 项通过。

## 3. 金额去浮点

```text
后端：
  PosCalculator PERCENT 券：Math.round(double) → Money.mulDiv(..., 100)
  BargainPricing 1.05：Math.round(cost * 1.05) → Money.mulDiv(cost, 105, 100)
  积分最小单位：Math.pow(10, units) → BigDecimal.scaleByPowerOfTen
前端：
  toMinor：number 乘 10^n → 字符串拆分整数 + 下一位四舍五入
```

测试：`PosCalculatorTest` 7 项、`BargainPricingTest` 4 项通过。

## 4. 明确不做

- 跨币种退款/分账（用户明确不允许）；
- 多跳三角换算（只做直接汇率 + 反向推导）；
- 自动汇率偏差告警（当前只记录状态）。


## 6. 真机验证（2026-10-31 16:23）

```text
启用 Frankfurter（无 Key）→ POST /refresh 200
  lastSuccessAt 有值，写入 29 个币种
  CNY→AUD 0.21398 / CNY→USD（见 exchange_rate source=frankfurter）
恢复 mock → POST /refresh 200，mock 汇率继续可用

坏 base-url（127.0.0.1:9）→ POST /refresh 仍 200
  lastError 有值；lastSuccessAt 保留；已有汇率不删除
```

## 5. 回归

- 全量后端：**816 tests / 0 failures / 0 errors**（第六轮补 4 项 mock 语义测试后 clean test，244 个测试类）。
- Playwright：**62 passed / 0 failed / 3 skipped**。