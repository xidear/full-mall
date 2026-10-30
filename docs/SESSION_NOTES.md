# 会话交接记录

> 规则见 `AGENTS.md` §14：每批改动结束更新本文件，只保留最近 3 批。
> 写清“已做并验证 / 已做未验证 / 未做”，以及环境事实（端口、容器、库是否重建）。

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
