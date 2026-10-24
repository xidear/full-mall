# 多参考图 / 主体参考：各家官方传参方式

> 结论先说：**商品图需要多角度参考、视频需要角色脸部定型**，所以我们的任务参数必须支持
> "**多张参考图 + 每张的角色**"，而不是单张 `ref_img`。

## 我们的统一参数（内部规范）

```jsonc
{
  "prompt": "...",
  "negative": "...",
  "images": [
    { "assetId": "uuid", "url": "https://...", "role": "REFERENCE" },
    { "assetId": "uuid", "url": "https://...", "role": "SUBJECT" },      // 角色脸部定型
    { "assetId": "uuid", "url": "https://...", "role": "FIRST_FRAME" },
    { "assetId": "uuid", "url": "https://...", "role": "LAST_FRAME" }
  ]
}
```

- `role` 语义：`REFERENCE` 通用参考（多角度商品图）、`SUBJECT` 主体/角色（脸、服装）、
  `FIRST_FRAME` 视频首帧、`LAST_FRAME` 尾帧。
- 老参数 `sourceAssetIds` 保留，等价于全部 `REFERENCE`。

## 各家映射

| 供应商/协议 | 传参方式 | 多图 | 角色/首尾帧 |
|---|---|---|---|
| **火山方舟 Seedance 2.5（视频）** `ark-video` | `content: [{type:"text",text},{type:"image_url",image_url:{url},role:"reference_image"}...]` | ✅ | `role` 字段区分；官方说明**首帧 / 首尾帧 / 全模态参考**是三种互斥场景 |
| **火山方舟 Seedream（图片）** `ark-image` | 同上 `content[]`，每张图 `role:"reference_image"` | ✅ | 无 |
| **阿里百炼 qwen-image-edit（图片编辑）** `dashscope-multimodal` | `messages:[{role:"user",content:[{image:url}...,{text:prompt}]}]` | ✅ 多张 `{image}` | 无 |
| **阿里百炼 文生图（旧）** `dashscope-image` | `input.ref_img` 单张 | ❌ 单张 | 无 |
| **MiniMax Hailuo H3** `minimax-video` | 参考生视频（Ref2VA）支持多图/多视频混合参考；接口用 `subject_reference`（角色参考）与首帧图 | ✅（官方称最多 10 张参考） | `subject_reference.type=character` |
| **Google Veo / Gemini** `gemini-image` / `veo-video` | 请求里 `referenceImages[]`（最多 3 张"ingredients"） | ✅ | 角色一致性靠多张同一主体参考图 |
| **OpenAI gpt-image-1** `openai-images` | `POST /v1/images/edits`，**multipart** 字段 `image[]`（可多文件） | ✅ | 无 |

参考来源：
- 火山方舟《创建视频生成任务》：`content` 每张参考图 `role` 为 `reference_image`；首帧/首尾帧/全模态参考互斥 — <https://ark.volcengine.com/region:cn-beijing/docs/create-video-generation-task-api>
- 千问 image edit 示例：`messages[].content` 里多张 `{image}` — <http://developer.aliyun.com/article/1765536>
- MiniMax H3（Ref2VA 多图/多视频参考） — <https://design.minimaxi.com/h3>
- Veo "Ingredients to Video"（多参考图保证一致性） — <https://www.php.cn/faq/2699559.html>

## 我们的实现约定

1. 任务创建时允许传 `images[{assetId, role}]`；服务端解析成 URL 列表再交给适配器。
2. 适配器**只做协议映射**，不做业务判断；不支持多图的协议遇到多图要**明确报错**（不静默丢图）。
3. 视频场景若同时给首帧和多张参考图，按供应商限制处理：Seedance 三种场景互斥 → 我们**优先**首帧模式并在提示词中引用参考图；若显式要求全模态，则走 `omni` 标记。

## 已验证的请求体（官方形态，stub 断言）

| 协议 | 断言点 | 用例 |
|---|---|---|
| `ark-image` | `content[]` 含全部参考图 URL 且 `role=reference_image` | `AiMultiReferenceTest` |
| `ark-video` | `first_frame` 与 `reference_image` 同时出现 | `AiMultiReferenceTest` |
| `openai-images`（多图） | `POST /images/edits`，`multipart/form-data`，`image[]` 出现 `ref1.png`/`ref2.png` | `AiProviderPayloadTest` |
| `dashscope-multimodal` | `messages[0].content` 含多张 `{image}` + `{text}` | `AiProviderPayloadTest` |
| `minimax-video` | `first_frame_image` + `subject_reference[].type=character` | `AiProviderPayloadTest` |

> 这些都是"官方文档形态 + 本地 stub"，不需要真实 Key 即可回归；拿到 Key 后只换 `ai_provider` 的 endpoint/model 即可。
