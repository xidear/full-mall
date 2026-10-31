# 本机 CPU embedding 服务（可选）

给知识库语义检索用的 OpenAI 兼容向量服务。默认端口 **41291**（AGENTS §8 号段内）。

- 模型：`Xenova/bge-small-zh-v1.5`（量化 ONNX，约 23MB，512 维，中文）
- 运行：CPU（ONNX Runtime），无需 GPU
- 协议：`POST /v1/embeddings`，与主程序 `openai-embedding` 适配器直接兼容

## 启动

```bash
cd tools/embedding-service
npm install --omit=dev
EMBEDDING_ALLOW_REMOTE=1 node server.mjs     # 首次联网下载模型到 ./models
```

离线部署：把 `./models` 一起拷贝过去，设 `EMBEDDING_ALLOW_REMOTE=0`；缺模型时启动失败并打印原因，
**不会**返回空向量。

## 配置到主程序

后台「AI 客服 → 模型供应商 → 添加供应商」：

| 字段 | 值 |
|---|---|
| 标识 | `local-embedding` |
| 协议 | `openai-embedding` |
| 服务地址 | `http://127.0.0.1:41291/v1` |
| 模型名 | `bge-small-zh-v1.5` |
| 优先级 | 5（小于 ollama 的 510，确保被选中） |

然后把检索模式切到语义：

```bash
APP_AI_ASK_RETRIEVAL=SEMANTIC
```

重启 API，重建 FAQ 索引：

```bash
curl -X POST 'http://127.0.0.1:41280/admin/v1/ai/semantic/reindex?kind=FAQ' \
  -H "Authorization: Bearer <token>"
```

## 验证

```bash
curl -s http://127.0.0.1:41291/health
# {"ok":true,"model":"Xenova/bge-small-zh-v1.5","dim":512,"loaded":true}

curl -s http://127.0.0.1:41291/v1/embeddings \
  -H 'Content-Type: application/json' \
  -d '{"input":"运费怎么算"}'
```

## 为什么不用 Ollama

Ollama 需要额外二进制与守护进程；本机没有 GPU 设备节点时，9B 模型 CPU 推理只有个位数 token/s。
这个服务只做 embedding（小模型、短文本），24 核 CPU 足够，且与现有 `openai-embedding` 协议零改造。