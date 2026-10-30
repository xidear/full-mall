# 会话交接记录

> 规则见 `AGENTS.md` §14：每批改动结束更新本文件，只保留最近 3 批。
> 写清“已做并验证 / 已做未验证 / 未做”，以及环境事实（端口、容器、库是否重建）。

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
