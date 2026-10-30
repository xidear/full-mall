# 会话交接记录

> 规则见 `AGENTS.md` §14：每批改动结束更新本文件，只保留最近 3 批。
> 写清“已做并验证 / 已做未验证 / 未做”，以及环境事实（端口、容器、库是否重建）。

## 2026-10-31 / AI 问答最小闭环

- 已做并验证：POST /admin/v1/ai/ask（400/503/200 三态 + 拒答 + 强制引用 + 审计）；
  7 条单测；真实 HTTP 空问题 400、正常问题因缺 embedding 返回 503
- 已做未验证：命中路径的**真实** 200（需要可用的 openai-embedding 供应商；当前环境没有）
- 未做 / 已知缺口：答案由片段拼装，尚未接真实对话模型；禁词/辱骂规则未实现
- 环境事实：语义检索默认开启（APP_AI_SEMANTIC_ENABLED=true），但知识库为空

## 2026-10-31 / 生效值可见 + locale 核对

- 已做并验证：GET /admin/v1/settings/effective（12 项，含来源 MARKET/GLOBAL）；
  系统设置新增"市场级覆盖与生效值"表，页面渲染 12 行
- 已做未验证：无
- 未做 / 已知缺口：④ 优惠券/门店（无列表页与明确状态语义）、⑥ ai 三态（问答链路未实现）
- 环境事实：admin 41281 需重新构建才能看到本区块（已构建）

## 2026-10-31 / 三态回归测试

- 已做并验证：12 条单测（storage 4 / export 3 / readiness 5）全绿，BUILD SUCCESS
- 已做未验证：无
- 未做 / 已知缺口：ai 的三态（未配供应商时 /ai/ask 应 503）未测——问答链路本身还没实现
- 环境事实：跑测命令 mvn -Dtest=ConfigurableAssetStorageTest,ExportStorageGatingTest,CapabilityReadinessServiceTest test

## 2026-10-31 / 枚举覆盖第二批

- 已做并验证：MEDIA_PURPOSE / MEDIA_ATTACH_ROLE / COUPON_TYPE / PROMOTION_TYPE 四组进后端；
  素材库筛选、AI 素材两处下拉、券类型下拉、POS 促销类型列均已改为后端候选（浏览器实测）
- 已做未验证：无
- 未做 / 已知缺口：OpsView 证书来源列仍是原始值（来源字段本身是自由文本）
- 环境事实：无变化

## 2026-10-31 / StatusToggle 推广（供应商）

- 已做并验证：供应商 POST /active（停用即时失效凭证）+ 审计 SUPPLIER_STATUS_CHANGED；
  前端内联开关；make acceptance 10/10
- 已做未验证：无
- 未做 / 已知缺口：优惠券模板/门店列表未做（无明确状态语义与列表页，不做猜测性端点）；
  促销规则是 DRAFT→ACTIVE→ENDED 生命周期，布尔开关不适用，保持原按钮
- 环境事实：API 41280；审计表时间列是 created_at（不是 occurred_at）

## 2026-10-31 / readiness + 导出存储

- 已做并验证：GET /admin/v1/readiness（7 项能力，标注配置来源/MISMATCH）；导出存储改为集成配置，
  未配置创建任务 503、配置后 202；make acceptance 9/9
- 已做未验证：支付/短信/物流仍读环境变量（readiness 已标"来源：环境变量"），统一到集成配置留待下一批
- 未做 / 已知缺口：StatusToggle 推广、枚举覆盖、三态测试、系统设置"生效值"显示、locale 复核
- 环境事实：export 已启用 local + directory=/data/full-mall/server/data/exports

## 2026-10-31 / 验收体系

- 已做并验证：scripts/acceptance-check.mjs（8/8 通过）+ docs/ACCEPTANCE.md（28 条对照 + 7 项未完成）
- 已做未验证：UI 侧 28 条需人工按文档点选（脚本只覆盖接口层）
- 未做 / 已知缺口：见 ACCEPTANCE.md 第三节
- 环境事实：沿用 41280/41281/41285；make acceptance 依赖 API 已启动

## 2026-10-31 / 存储启用语义修正

- 已做并验证：
  - 未启用存储时上传 → 503 ASSET_STORAGE_NOT_CONFIGURED（实测）
  - 未启用时访问已有素材 → 改为 404（原先是 409 Conflict，错误码与场景不符）
- 已做未验证：无
- 未做 / 已知缺口：切换服务商后旧服务商文件不迁移（需运维迁移脚本）
- 环境事实：storage 当前为 local + enabled=true + directory=/data/full-mall/server/data/assets

## 2026-10-31 / 护栏与兜底清理（进行中）

- 已做并验证：
  - `scripts/check-fallbacks.py` 接入 `make contract-check`（91 处已登记，45 处标注待复核）
  - 去掉支付/短信默认 mock、AI 示例供应商自动注入默认关闭、support-console 去掉 `localhost:3000` 兜底
  - `make contract-check` 通过；API 重启后日志出现显式 mock 告警
- 已做未验证：
  - `APP_AI_BOOTSTRAP=false` 在**干净库**下确实不注入供应商（当前库里有历史遗留 5 条）
- 未做 / 已知缺口：
  - 能力就绪端点 `/admin/v1/readiness` 与未就绪统一 503
  - 导出存储改读集成配置（删除 `./data/exports` 默认值）
  - `StatusToggle` 推广、枚举字典覆盖剩余裸码列、三态测试
- 环境事实：
  - 端口：API 41280 / admin 41281 / storefront 41285（走 `scripts/dev-stack.sh`）
  - 容器：`fullmall-api-dev`、`fullmall-worker-dev`、`fullmall-postgres-1`
  - 改动过 `V1__baseline.sql`／`application.yml`：旧库需重建或 `flyway repair` + 补 DDL
