# 会话交接归档

> `docs/SESSION_NOTES.md` 只保留最近 3 批（AGENTS §14.3）；更早的批次按时间倒序留在这里。

## 2026-10-31 / 自动汇率 + 退款汇率快照 + 金额去浮点

- 已做并验证：
  - 自动汇率：`exchange_rate` 集成能力（mock / Frankfurter，无 API Key）；只有 demo 未配置才用 mock，
    生产未配置不自动拉取；失败保留上次/手工汇率；`GET /provider`、`POST /refresh`
  - 退款汇率：V7 `order_line_refund.fx_*`；退款仍原币种，允许退款时汇率与下单不同
  - 金额去浮点：POS 百分比券改 `Money.mulDiv`，砍价 1.05 改整数比例，
    积分最小单位改 BigDecimal；前端 `toMinor` 改字符串/整数解析
  - 测试：ExchangeRateFetch/Mock、Refund、PosCalculator、BargainPricing 均通过
- 已做并验证（回归）：
  - 后端 **812 tests / 0 failures / 0 errors**（clean test）
  - Playwright **62 passed / 0 failed / 3 skipped**；验收 17/17
- 未做 / 已知缺口：
  - 跨币种退款/分账明确不做；只做直接汇率（无多跳）
  - 自动汇率偏差告警未做
- 环境事实：
  - migration 到 V7；`APP_FX_AUTO_INTERVAL_MS` 默认 3600000
  - 运行环境 exchange_rate 已启用 **frankfurter**（`api.frankfurter.dev/v1`，旧 `.app` 域名 301），
    mock 置为 disabled；mock 只在 `app.mode=demo` 且未配置时兜底，dev/prod 未配置不自动拉取
  - 坑：`server/target` 曾有 278 个 root 属主 class（早前用 root 跑过 Maven），
    非 root 容器写不进去 → `error while writing ...class`。用 root 容器 `rm -rf server/target` 清掉。
    **Maven 容器一律带 `-u $(id -u):$(id -g)`**
  - 41281 Vite dev / 41280 API / embedding 41291

## 2026-10-31 / 订单汇率快照 + 积分按币种 + 测试债清零

- 已做并验证：
  - `V6__order_fx_snapshot`：订单写 `fx_rate/fx_base_currency/fx_base_total_amount/fx_rate_at`；
    无汇率不阻塞下单，写 `ORDER_FX_RATE_MISSING` 审计，后台订单详情显示“未快照”
  - 积分按币种：`PosCalculator.pointsEarned(payable,currency)` 与
    `MemberRewardService.accrue(...,currency)` 按最小单位换算（JPY 每 1 日元 1 分、KWD 每 1 第纳尔 1 分）
  - 测试债：7 个后端类（Export/Schedule/Maintenance/IntegrationConfig/AiMedia/AiProvider/MediaUpload）
    通过测试夹具启用 integration_config 后全部转绿；6 个 Playwright 历史用例（admin-pickers/audit×2/
    export×2/translations）按当前 UI 修复后通过
- 已做未验证：
  - 完整后端套件：**810 tests / 0 failures / 0 errors**（clean test）
  - Playwright 全量：**62 passed / 0 failed / 3 skipped**
- 未做 / 已知缺口：
  - 供应商结算多币种仍明确拒绝（未做自动换算）
  - 跨币种退款/分账、自动拉汇率未做
- 环境事实：
  - migration 到 V6；export 集成目录已修正为容器内 `/repo/server/data/exports`
  - 41281 Vite dev / embedding 41291 / API 41280
