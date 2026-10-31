# ADR-0016：AI 客服接入真实对话模型与显式检索模式

- 状态：已采纳（2026-10-31）
- 关联：`docs/AI_SERVICE_MODULE.md`、`docs/AI_PROVIDER_PARAMS.md`、ADR-0004

## 背景

AI 客服此前只做“检索片段拼装答案”，没有调用任何对话模型；当时也没有可用的 embedding 服务，
于是 `/admin/v1/ai/ask` 在真实环境里要么 503、要么只能拒答。

宿主机复核（2026-10-31，用户纠正后）：24 核 Xeon / 62G 内存；**NVIDIA RTX 5070 Ti**
（驱动 610.43.02，`/proc/driver/nvidia/gpus/0000:03:00.0`），Docker 提供 `nvidia` runtime 与 CDI
（`nvidia.com/gpu=all`）。当前 shell 内没有 `/dev/nvidia*`、也未安装 `nvidia-smi`，但容器可通过
`--gpus all` 使用 GPU。本批按用户选择用 **CPU** 跑 embedding（小模型足够），未占用 GPU；
以后要上 9B 对话模型时可直接走 GPU 容器，不必换协议。

需求是“用真实模型回答，可以用免费小模型”。候选有三类：

| 方案 | 可行性 | 结论 |
|---|---|---|
| 本机 Ollama + qwen 类 9B | 无 GPU，CPU 推理 9B 只有个位数 token/s，客服并发不可用；模型权重下载与磁盘也要额外运维 | 不选（保留为可选供应商，不默认启动） |
| 公网免费模型（OpenRouter / OpenCode 等） | 需要账号与 API Key，配额和可用性随活动变化，外部数据出境 | 不选（可手工登记为供应商） |
| 公司自营 OpenAI 兼容端点 `https://company.binnarui.com:8500/v1`（`deepseek-v4.1-flash`） | 已在本机网络内、无需 API Key、实测 200、与 `openai-chat` 适配器直接兼容 | **采用** |

embedding 由本机 CPU 小模型承担（见决策 2）。

## 决策

1. **对话模型**：使用 `ai_provider` 表中 `protocol=openai-chat` 的供应商；首批配置为公司自营
   `deepseek-v4.1-flash`。供应商通过后台「AI 客服 → 模型供应商」维护，不写死在代码或迁移里。
2. **检索模式显式化**：新增 `app.ai.ask.retrieval`，取值 `KEYWORD` / `SEMANTIC`。
   - `KEYWORD`：基于 FAQ + 导入知识块做二元组重合率检索，不需要 embedding；
   - `SEMANTIC`：向量检索。本机新增 `tools/embedding-service`（端口 41291，`Xenova/bge-small-zh-v1.5`，
     量化 ONNX，512 维，CPU 推理，约 23MB），按 `openai-embedding` 协议输出 `/v1/embeddings`；
   - **禁止失败后自动切换**：模式会显示在 `/admin/v1/ai/status`，运营能看出当前走的是哪条路；
   - 本机当前配置 `SEMANTIC`（`.env`），服务不可用时明确 503，而不是假装答不出来。
3. **引用由服务端生成**：模型只负责组织句子；引用（sourceId + 分数 + 片段）始终由检索结果产生，
   模型输出不得改变引用集合。
4. **失败必须显式**：模型不可达/超时/空返回 → `503 AI_CHAT_FAILED` / `AI_CHAT_EMPTY`；
   检索不到 → `200 fallback=true` 并转人工。两者不能混为“没有答案”。
5. **供应商选择按协议**：`TEXT` 类型下同时存在 chat 与 embedding，按 kind+优先级盲选会把问题发给
   向量模型；`AiMediaService.chat()` 改为只挑 `openai-chat`。

## 后果

- 没有 GPU 也能跑通“语义检索 + 真实模型回答 + 引用”，本机已用 CPU embedding 验证（0.777 相关度）。
- 语义检索依赖 `tools/embedding-service`（可选组件）：未启动时 SEMANTIC 明确 503；可切 `KEYWORD`，
  或把 `local-embedding` 的 endpoint 换成任意 OpenAI 兼容向量服务，代码路径不变。
- 公司自营端点是运行期外部依赖，必须登记在 `docs/DEPENDENCY_AND_DEPLOYMENT_MATRIX.md`；
  端点不可用时 AI 客服 503，不影响下单与其它后台功能。
- 关键词检索仍保留：没有 embedding 的机器（或离线演示）可用它，但召回质量低于向量检索。