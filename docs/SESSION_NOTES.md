# 会话交接记录

> 规则见 `AGENTS.md` §14：每批改动结束更新本文件，只保留最近 3 批。
> 写清“已做并验证 / 已做未验证 / 未做”，以及环境事实（端口、容器、库是否重建）。

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
- 已做未验证：
  - 15052 生产构建替换（本次会执行并记录结果；若失败按原 Vite dev 回滚）
  - Playwright 全量（本轮以 HTTP + 构建 + 单测为主）
- 未做 / 已知缺口：
  - embedding 供应商仍缺（SEMANTIC 模式不可用；KEYWORD 已可跑）
  - 跨云迁移（按用户要求不做）
  - 金额精度、汇率（审计 L10/L11，未在本批范围）
- 环境事实：
  - 端口：API 41280 / admin 41281 / storefront 41285；`15052 → 192.168.1.15:41281`
  - **41281 现在跑 `vite preview`（构建产物）**：`scripts/admin-preview.sh {start|restart|stop|status}`；
    不要再直接跑 `npm run dev`，否则 15052 会退回源码服务
  - 构建资源目录是 `/ui/**`（不是 `/assets/**`，后者代理给 API 的媒体）
  - 容器：`fullmall-api-dev`、`fullmall-worker-dev`、`fullmall-postgres-1`
  - 数据库：**未 drop**，Flyway 增量 `V3__store_admin`（baseline 未改）；旧版本（squash 前）库仍需重建
  - 模型端点：`https://company.binnarui.com:8500/v1` + `deepseek-v4.1-flash`（无需 API Key）；
    供应商行在 `ai_provider`，由后台维护，不写进迁移
  - AI 检索：`APP_AI_ASK_RETRIEVAL=KEYWORD`（默认）；有 embedding 后改 SEMANTIC

## 2026-10-31 / 补齐 8 项（状态机/link/未命中页/编码生成/迁移脚本/AI 接模型）

- 已做并验证：通知状态机（OPEN-ACK-RESOLVED 实测）、后端下发 link（/inventory）、
  AI 未命中清单页（截图）、编码生成（接口 + 页面按钮）、迁移脚本（--verify 抓到 1 个丢失文件）、
  AI 接 chat 模型（单测 8 条）
- 已做未验证：真实模型调用（本机无可用 chat 供应商，ollama 未运行）
- 未做 / 已知缺口：跨云迁移未实现（脚本明确报错）；促销/门店不适用布尔开关
- 环境事实：**存储目录必须是容器内路径（/repo/server/data/assets）**，
  配成宿主路径会写进容器文件系统、重启即丢——脚本 --verify 可发现

## 2026-10-31 / AI 禁词与未命中清单

- 已做并验证：禁词输入/输出双向拦截（固定话术 + 审计）；/admin/v1/ai/unanswered 可查；
  单测 8 条；acceptance 11/11
- 已做未验证：真实命中回答（仍需 openai-embedding 供应商）
- 未做 / 已知缺口：答案仍由片段拼装，未接真实对话模型；未命中清单尚无前端页面
- 环境事实：禁词默认内置，可用 APP_AI_ASK_BANNED_WORDS 覆盖