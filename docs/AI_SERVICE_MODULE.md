# AI 客服模块（设计规格 v1）

## 0. 要不要专门训练？（结论先行）

**这一期不做训练。** 理由不是"训练很难"，而是训练解决不了这里的核心问题：

| 需求特征 | RAG（检索增强） | 微调（LoRA/SFT） |
|---|---|---|
| 商品价格/库存/参数每天变 | 改库即生效 | **训练完就过期**，必须重训 |
| 售后政策按客户改 | 换文档即可 | 每条政策都要重训 |
| 需要"你说的这句来自哪份文档" | 天然带引用 | 无法追溯来源 |
| 不知道就要说不知道 | 检索无命中→转人工 | 模型会**编**一个像样的答案 |

微调真正解决的是**表达风格、话术、行业黑话、输出格式**——不是"记住事实"。
所以顺序是：**先 RAG + 约束（本期）→ 收集真实会话 → 只有出现"事实对但话不像客服"时才上 LoRA**。

**什么时候才值得训练**（明确触发条件，避免拍脑袋）：
1. 检索命中率高（Top-3 ≥ 80%）但答案风格/格式不合格；
2. 行业术语密集，通用模型理解偏差大；
3. 有 ≥ 2000 条人工确认过的优质问答（没有这个量，训练只会学坏）。

**训练机器（192.168.1.15 / 5070Ti 16G）何时用**：上面三条同时满足时，跑 7B 的 QLoRA；
`.8` 的 2×V100 用于更大模型或全参微调。**在此之前两台机器只做推理服务**。

## 0.1 推理框架不构成决策点

对外只需 **OpenAI 兼容接口**（`/v1/chat/completions`、`/v1/embeddings`）。
vLLM、Ollama、LM Studio、云 API 都能满足，部署时选哪个由运维决定，代码不动。


> 定位：**可插拔模块**。开源版（GitHub）**不包含**；私有/生产版包含并可按许可证启用。
> 演示先用 mock 数据，客户真实需求时按客户文档训练。

## 1. 构建形态（两套产物）

| 形态 | Maven profile | 包含模块 | 用途 |
|---|---|---|---|
| 社区版 | `community`（默认） | 不含 `ai`、不含许可强制 | GitHub 开源发布 |
| 企业版 | `enterprise` | 含 `ai`、`licensing` | 客户交付/演示 |

- `server/pom.xml` 用 profile 控制 `<modules>`/依赖；
- 源码目录 `server/src-enterprise/java` 不进入社区版源码包；
- **运行时开关**：`app.ai.enabled`（默认 `false`）；未启用时不注册任何 AI 路由与任务；
- 未配置模型时**明确 503**，不返回空答案（禁止静默兜底）。

## 2. 模块边界（Spring Modulith）

```
com.fullmall.ai
├── api/        AdminAiController、SupportAiController（供客服台调用）
├── app/        KnowledgeSyncService、RetrievalService、AnswerService、FaqLearningService
├── infra/      EmbeddingClient、LlmClient、VectorStore（pgvector）、知识源读取
└── config/     AiProperties、条件装配（enabled && license.feature=ai）
```

依赖方向：`ai → catalog/order/support`（只读 API），**任何模块不得反向依赖 ai**。

## 3. RAG 管线

1. **知识源**（自学习输入）
   - 商品参数：`product`/`sku`/属性、价格与库存快照；
   - 商品关联：类目、品牌、可替代/配件关系（V72 起有 `brand`）；
   - 商品文档：说明书/FAQ/售后政策（对象存储里的 PDF/Word，用 Apache Tika 抽文本）；
   - 历史会话与工单结论（客服最终确认的答复）。
2. **切分与嵌入**：按语义段切分（商品一个 chunk = 标题+参数+卖点，≤512 token），
   `EmbeddingProvider` 接口：`HttpEmbeddingProvider`（OpenAI 兼容）/`UnconfiguredEmbeddingProvider`（明确拒绝）。
3. **存储/检索**：见 `docs/AI_VECTOR_STORAGE_DECISION.md`（ADR-001）。
   当前实现：`knowledge_chunk` 存 float32 字节 + 关键词候选 + 应用内余弦重排；
   超过 20 万块或语义召回不达标时切 Lucene HNSW（纯 Java，测试环境可跑）。
   历史写法（已废弃）：PostgreSQL + **pgvector**（`knowledge_chunk(embedding vector(1024), content, source_type, source_id, version, updated_at)`），
   HNSW 索引；同源更新**先删旧 chunk 再写新**，保证"最新参数"不被旧知识污染。
4. **检索**：向量 top-k + 关键词（`tsvector` 中文用 pg_bigm 或 trigram 兜底）+ 元数据过滤（类目/品牌/在售状态）。
5. **生成**：LLM 仅允许基于检索片段作答；无命中→"我没查到，转人工"，**不得编造**；
   引用来源（商品名/文档标题）随答复返回，便于客服核对。

## 4. 自学习（必须有人审）

| 触发 | 动作 |
|---|---|
| 商品/参数/关联更新 | 发事件 → `knowledge_sync` 入队 → 重建该商品 chunk |
| 文档上传/替换 | 抽文本 → 切分 → 重建该文档 chunk |
| 客服修改 AI 答复后采纳 | 生成 `faq_candidate` 草稿 |
| 客服确认 FAQ | 写入知识库并向量化（**人审通过才生效**） |

**不做全自动自训练**：自动把模型输出写回知识库会累积错误（模型幻觉污染知识库），
且无法追责。红线与 `docs/AI_EXTENSION*.md` 一致：AI 不改价、不退款、不封号。

## 5. 模型与算力

| 机器 | 显卡 | 建议 |
|---|---|---|
| 192.168.1.15（在线） | RTX 5070 Ti 16G | vLLM（或 Ollama）跑 7B~14B 量化（Q4/Q8），embedding 用 bge-m3 / bge-large-zh |
| 192.168.1.8（需 BMC 开机） | 2×V100 32G | 32B 量化或更大模型；注意 **V100 不支持 bf16/flash-attn**，用 FP16 + paged attention |

- 接口统一走 **OpenAI 兼容**（`/v1/chat/completions`、`/v1/embeddings`），换供应商不改代码；
- 部署脚本与模型下载清单单独放 `deploy/ai/`，不进入应用镜像；
- 本机（应用服务器）**不需要 GPU**。

## 6. 演示数据

- 公开中文客服对话数据 + 自造商品问答，落成 `knowledge_chunk` 与 FAQ 样例；
- 演示数据与生产数据**物理隔离**（独立 schema 或独立库），演示导入脚本必须显式指定目标库；
- 演示口令/凭据不入库。

## 7. 验收标准（可执行）

| 编号 | 验收 |
|---|---|
| AI-1 | 社区版构建产物**不含** ai 包与路由（`/admin/v1/ai/**` 返回 404） |
| AI-2 | 企业版未配置模型时，AI 问答返回 **503 且带原因**，不返回空答案 |
| AI-3 | 商品改价后 10 秒内，问"这个多少钱"能答出新价格（同步任务实测） |
| AI-4 | 知识库无命中时回答"未查到，建议转人工"，且返回 `fallback=true` |
| AI-5 | 每条答复返回引用来源（商品/文档 id），客服可点开核对 |
| AI-6 | 客服修改并采纳的答复进入 `faq_candidate`，未审核前不参与检索 |
| AI-7 | 检索评测脚本：50 条标注问题，Top-3 命中率 ≥ 80% 才算通过 |

## 8. 已知限制（不美化）

- 7B 级模型在专业售后问题上会答错，必须保留"转人工"与引用核对；
- 中文长文档切分会丢上下文，说明书类建议人工整理 FAQ 优先；
- pgvector 需在生产库启用扩展；嵌入式测试库不含该扩展，故 AI 相关测试用独立 PG 容器。
