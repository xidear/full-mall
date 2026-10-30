# clip-service（开发机专用多模态向量服务）

给"用一句话搜图片内容"提供图像向量。**可选依赖**：不启动它，素材库只是没有"图片内容"搜索，
其它功能（关键词、描述语义、AI 生成、AI 识别）全部照常。

## 为什么单独一个进程

图像向量模型（CLIP）体积大、算力要求高，客户服务器不一定装；主程序只按
「AI 供应商」表单里 `protocol=clip-http` 的配置调用，模型和主程序解耦。

## 启动

```bash
cd tools/clip-service
npm install --ignore-scripts   # sharp 用 npm 预编译包，避免 node-gyp 编译
npm start                      # 默认 41290 端口（CLIP_PORT 可覆盖，须在 41280–41299 段内）
```

首次需要模型文件（约 150MB）。国内直连 huggingface.co 会超时，用 hf-mirror 先下到 `./models`：

```bash
node scripts/download-model.mjs     # 内部固定 https://hf-mirror.com
```

## 在后台配置

「第三方服务 → AI 供应商」新增一行：

| 字段 | 值 |
|---|---|
| code | `clip-local` |
| protocol | `clip-http` |
| endpoint | `http://127.0.0.1:41290` |
| model | `clip-vit-base-patch32` |

配好后素材库会出现「图片内容」搜索；上传/AI 生成图片会自动建图像索引，
历史素材可在素材库接口 `POST /admin/v1/assets/image-index/reindex` 补建。

## 接口

| 方法 | 路径 | 请求 | 响应 |
|---|---|---|---|
| GET | `/health` | - | `{ok, model, dim}` |
| POST | `/embed/text` | `{text}` | `{vector: number[512], dim, ms}` |
| POST | `/embed/image` | `{url}` 或 `{base64}` | `{vector: number[512], dim, ms}` |

文本与图像共用同一向量空间（CLIP），因此"文字搜图"直接比余弦相似度即可。
