# ADR-0016：AI 客服接入真实对话模型与显式检索模式

- 状态：已采纳（2026-10-31）
- 关联：`docs/AI_SERVICE_MODULE.md`、`docs/AI_PROVIDER_PARAMS.md`、ADR-0004

## 背景

AI 客服此前只做“检索片段拼装答案”，没有调用任何对话模型；本机没有 GPU，也没有运行 Ollama，
语义检索依赖的 embedding 供应商同样缺席，于是 `/admin/v1/ai/ask` 在真实环境里要么 503、要么只能拒答。

需求是“用真实模型回答，可以用免费小模型”。候选有三类：

| 方案 | 可行性 | 结论 |
|---|---|---|
| 本机 Ollama + qwen 类 9B | 无 GPU，CPU 推理 9B 只有个位数 token/s，客服并发不可用；模型权重下载与磁盘也要额外运维 | 不选（保留为可选供应商，不默认启动） |
| 公网免费模型（OpenRouter / OpenCode 等） | 需要账号与 API Key，配额和可用性随活动变化，外部数据出境 | 不选（可手工登记为供应商） |
| 公司自营 OpenAI 兼容端点 `https://company.binnarui.com:8500/v1`（`deepseek-v4.1-flash`） | 已在本机网络内、无需 API Key、实测 200、与 `openai-chat` 适配器直接兼容 | **采用** |

embedding 仍然没有可用服务，因此检索不能绑死向量检索。

## 决策

1. **对话模型**：使用 `ai_provider` 表中 `protocol=openai-chat` 的供应商；首批配置为公司自营
   `deepseek-v4.1-flash`。供应商通过后台「AI 客服 → 模型供应商」维护，不写死在代码或迁移里。
2. **检索模式显式化**：新增 `app.ai.ask.retrieval`，取值 `KEYWORD`（默认）/ `SEMANTIC`。
   - `KEYWORD`：基于 FAQ + 导入知识块做二元组重合率检索，不需要 embedding；
   - `SEMANTIC`：沿用向量索引，缺 embedding 时明确 503，并提示改配置或装向量服务。
   - **禁止失败后自动切换**：模式会显示在 `/admin/v1/ai/status`，运营能看出当前走的是哪条路。
3. **引用由服务端生成**：模型只负责组织句子；引用（sourceId + 分数 + 片段）始终由检索结果产生，
   模型输出不得改变引用集合。
4. **失败必须显式**：模型不可达/超时/空返回 → `503 AI_CHAT_FAILED` / `AI_CHAT_EMPTY`；
   检索不到 → `200 fallback=true` 并转人工。两者不能混为“没有答案”。
5. **供应商选择按协议**：`TEXT` 类型下同时存在 chat 与 embedding，按 kind+优先级盲选会把问题发给
   向量模型；`AiMediaService.chat()` 改为只挑 `openai-chat`。

## 后果

- 没有 GPU/embedding 也能跑通“检索 + 真实模型回答 + 引用”，本机能直接验收。
- 语义检索仍是可选项；要获得更好的召回，配置 `openai-embedding` 并把 `APP_AI_ASK_RETRIEVAL` 改为
  `SEMANTIC` 即可，代码路径不变。
- 公司自营端点是运行期外部依赖，必须登记在 `docs/DEPENDENCY_AND_DEPLOYMENT_MATRIX.md`；
  端点不可用时 AI 客服 503，不影响下单与其它后台功能。
- 关键词检索的召回质量低于向量检索；知识库以 FAQ 短问答为主时可用，文档型知识建议上 embedding。