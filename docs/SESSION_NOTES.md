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
  - 供应商：`company-chat`（deepseek-v4.1-flash）、`local-embedding`（bge-small-zh-v1.5，优先级 5）；
    `ollama-*` 已显式停用（本机未运行 Ollama）
  - 容器：`fullmall-api-dev`、`fullmall-worker-dev`、`fullmall-postgres-1`
  - 数据库：**未 drop**，Flyway 增量 `V3__store_admin`（baseline 未改）；旧版本（squash 前）库仍需重建
  - 模型端点：`https://company.binnarui.com:8500/v1` + `deepseek-v4.1-flash`（无需 API Key）；
    供应商行在 `ai_provider`，由后台维护，不写进迁移
  - AI 检索：`APP_AI_ASK_RETRIEVAL=SEMANTIC`（本机已配 `local-embedding`）；没有 embedding 的机器改 KEYWORD

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