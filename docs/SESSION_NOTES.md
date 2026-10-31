# 会话交接记录

> 规则见 `AGENTS.md` §14：每批改动结束更新本文件，只保留最近 3 批。
> 写清“已做并验证 / 已做未验证 / 未做”，以及环境事实（端口、容器、库是否重建）。

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

## 2026-10-31 / 金额精度 + 汇率阶段1 + 门店软删除 + 完整测试

- 已做并验证：
  - 金额精度：`Money.toMajor/parseMajor` 按币种小数位；`CurrencyPrecision` 用 ISO 并修正
    TWD/HUF（原误写 0）；前端统一 `@fullmall/api-client/money`（Intl/CLDR），
    admin 全部 `/100` 已清（只剩税率 bps/折扣等非金额换算）；storefront/store-console/
    supplier/support/mobile 同步
  - 汇率阶段 1：`V5__exchange_rate` + `ExchangeRateService`（BigDecimal、反向推导、找不到明确失败）
    + `/admin/v1/exchange-rates`（列表/新增/删除/换算）+ admin「汇率」页
  - 门店软删除：`V4__store_soft_delete` + `deleted_at`、删除/恢复接口、列表过滤、员工绑定拦截、
    前端「已删除」筛选与恢复
  - 模型超时：默认 180s，供应商 `extra.timeoutSeconds` 可配 5..600，AI 页可编辑
  - Maven 大陆镜像：`scripts/maven-settings-aliyun.xml`；新增 23 项测试全绿
  - 前端构建：admin / store-console / supplier / support / storefront 全部通过
- 已做并验证（续）：
  - 完整后端套件 808 项：本批引入的失败（store.code 缺失、迁移 V23 断言、asset.source 空、
    AI 错误文案）已全部修复并单测复跑通过；剩余 12 项在 7 个历史配置型测试类
    （MediaUpload/Export/Schedule/Maintenance/IntegrationConfig/AiMedia/AiProvider），与本次改动无关
  - Playwright 全量 52 passed / 10 failed / 3 skipped；其中 5 个失败是旧测试绑定已变文案，
    已修测试并复跑通过（admin/pricing×2/coupons/ai-page）；新增 money-and-exchange 3/3
  - 汇率页真机打开无控制台错误（screenshot `docs/evidence/exchange-rates.png`）
- 已做未验证：
  - 剩余 6 个 Playwright 历史用例（admin-pickers/audit×2/export×2/translations）未修，属已知测试债
- 未做 / 已知缺口：
  - 汇率未接入下单快照/结算/跨币种退款（阶段 2，见 ADR-0017）
  - 积分/佣金仍有 CNY 口径（`paidAmount / 100`），未按市场币种参数化
  - 7 个后端历史配置型测试类需补 integration_config 夹具
- 环境事实：
  - 新增迁移：`V4__store_soft_delete`、`V5__exchange_rate`（增量，不重建库）
  - 新页面：`/exchange-rates`（42 个帮助页）；权限 `settings.manage`
  - 测试命令：`mvn -s scripts/maven-settings-aliyun.xml test`
  - 41281 仍是 Vite dev；embedding 41291 仍在跑

## 2026-10-31 / 门店管理 + 促销完善 + AI 真实模型 + 15052 重建

- 已做并验证：
  - 门店管理：`GET/POST/PUT /admin/v1/stores` + `POST /{id}/active`；新增 admin「门店管理」页
    （列表内联启停、生成编码、市场/坐标/电话校验、`STORE_*` 审计）；V3 迁移给 `store` 补
    `code`（唯一）与 `updated_at`；`StoreAdminServiceTest` 覆盖编码重复/市场不存在/坐标越界/停用可见性
  - 促销完善：促销状态机只允许 DRAFT→ACTIVE/ENDED、ACTIVE→ENDED；草稿可编辑；新增「复制为草稿」；
    状态变更用条件更新（并发返回 409）；满减规则返回并回填 `scopeIds`（修复编辑清空范围）；
    规则编码一键生成；类型/渠道/范围/状态中文标签全部改为后端枚举（修正 PROMOTION_TYPE 缺 FLASH_SALE 等）
  - AI：`AiAnswerService` 真正调用 `openai-chat` 供应商（公司自营 `deepseek-v4.1-flash`），
    引用仍由服务端生成；新增 `app.ai.ask.retrieval=KEYWORD|SEMANTIC` 显式检索（默认 KEYWORD，
    无需 embedding）；`KeywordRetriever`（FAQ + 知识块，bigram 重合率）；模型失败 503 `AI_CHAT_FAILED`；
    `/admin/v1/ai/status` 返回 ready/missing/chatModel/retrieval；AI 页新增「模型供应商」管理
  - 税率规则改为列表内联开关（AGENTS §13.1）
  - 跨云迁移：仅落地规划 `docs/CROSS_CLOUD_MIGRATION_PLAN.md`（明确暂不实施）
  - 文档：ADR-0016（模型与检索选择）、ACCEPTANCE_MANUAL ⑧–⑫、permissions 说明
- 已做并验证（第二轮，按用户纠正）：
  - 本机 CPU embedding：`tools/embedding-service`（41291，bge-small-zh-v1.5，512 维），
    `/v1/embeddings` 实测 200；`local-embedding` 供应商已登记，`APP_AI_ASK_RETRIEVAL=SEMANTIC`
  - SEMANTIC 真实验证：FAQ「运费怎么算」召回 0.777，模型作答 663ms
  - 15052 按 Q1 切回 Vite dev（`scripts/admin-dev.sh`），生产构建脚本保留
- 已做未验证：
  - Playwright 全量；SEMANTIC 下的无关问题边界只测了「0 命中→转人工」
- 未做 / 已知缺口：
  - 门店软删除（`active` 已覆盖；等长期归档需求再加 `deleted_at`）
  - 跨云迁移（按用户要求不做）
  - 金额精度、汇率（审计 L10/L11，未在本批范围）
- 环境事实：
  - 端口：API 41280 / admin 41281 / storefront 41285 / **embedding 41291**；
    `15052 → 192.168.1.15:41281`
  - **41281 当前跑 Vite dev（Q1 选择，开发速度优先）**：`scripts/admin-dev.sh {start|restart|stop|status}`；
    上线前用 `scripts/admin-preview.sh` 切构建产物（两者都占 41281，不能同时）
  - 生产构建资源目录是 `/ui/**`（不是 `/assets/**`，后者代理给 API 的媒体）
  - embedding 服务：`cd tools/embedding-service && npm start`；模型缓存在 `tools/embedding-service/models`
    （已下载，约 23MB，不入库）；离线设 `EMBEDDING_ALLOW_REMOTE=0`
  - 宿主机：24 核 Xeon / 62G；**NVIDIA RTX 5070 Ti（驱动 610.43.02）**，Docker 有 nvidia runtime/CDI；
    当前 shell 无 `/dev/nvidia*`、未装 `nvidia-smi`；本批未用 GPU
  - 供应商：`company-chat`（deepseek-v4.1-flash）、`local-embedding`（bge-small-zh-v1.5，优先级 5）；
    `ollama-*` 已显式停用（本机未运行 Ollama）
  - 容器：`fullmall-api-dev`、`fullmall-worker-dev`、`fullmall-postgres-1`
  - 数据库：**未 drop**，Flyway 增量 `V3__store_admin`（baseline 未改）；旧版本（squash 前）库仍需重建
  - 模型端点：`https://company.binnarui.com:8500/v1` + `deepseek-v4.1-flash`（无需 API Key）；
    供应商行在 `ai_provider`，由后台维护，不写进迁移
  - AI 检索：`APP_AI_ASK_RETRIEVAL=SEMANTIC`（本机已配 `local-embedding`）；没有 embedding 的机器改 KEYWORD
