# 8 项补齐 · 证据与验收

> 采集时间：2026-10-31 · API `http://127.0.0.1:41280` · admin `http://127.0.0.1:41281`
> 每条都给出「请求 → 实际返回 → 期望」，截图在 `docs/evidence/`。

## 一、接口证据（脚本采集，原样输出）

| # | 项 | 请求 | 实际 | 期望 | 结论 |
|---|---|---|---|---|---|
| ③ | 通知状态机-造数据 | `POST /admin/v1/notifications/test-send` `{"channel":"IN_APP","eventType":"STOCK_LOW"}` | 200 | 200 | ✅ |
| ③④ | 通知字段 | `GET /admin/v1/notifications?limit=1` | `{"state":"OPEN","link":"/inventory","eventType":"STOCK_LOW"}` | state=OPEN、link=/inventory | ✅ |
| ③ | 认领 | `POST /admin/v1/notifications/{id}/ack` | 200 changed=true | 200 | ✅ |
| ③ | 标记修复 | `POST /admin/v1/notifications/{id}/resolve` | 200 changed=true | 200 | ✅ |
| ③ | 修复后状态 | `GET /admin/v1/notifications?limit=1` | `{"state":"RESOLVED"}` | RESOLVED | ✅ |
| ⑤ | AI 空问题 | `POST /admin/v1/ai/ask` `{"question":"   "}` | 400 `AI_QUESTION_REQUIRED` | 400 | ✅ |
| ⑤ | AI 禁词 | `POST /admin/v1/ai/ask` `{"question":"你这个傻逼机器人"}` | 200 `fallback=true`（"问题包含不当内容，已转人工处理"） | 200 拒答 | ✅ |
| ⑤ | 未命中清单 | `GET /admin/v1/ai/unanswered?limit=5` | 200 数组，含 `question` 原文 | 200 | ✅ |
| ⑥ | 编码建议 | `GET /admin/v1/codes/suggest?type=BRAND` | 200 `{"code":"BRAND-4A8U9-1655"}` | 200 且未被占用 | ✅ |
| ⑥ | 编码建议 | `GET /admin/v1/codes/suggest?type=COUPON` | 200 `{"code":"CPN-4A8UN-1623"}` | 200 | ✅ |
| ② | 能力就绪 | `GET /admin/v1/readiness` | 200 含 storage/export/payment/sms/logistics/moderation/ai | 200 | ✅ |
| ⑦ | 迁移校验 | `python3 scripts/migrate-storage.py --verify --dir …` | `库内 local 记录 2 条；缺失 1` | 能发现缺失 | ✅（并真的发现了 1 个丢失文件） |

## 二、页面证据（Playwright 截图）

| 截图 | 覆盖项 | 页面可见内容 |
|---|---|---|
| `docs/evidence/notifications-state.png` | ③ 状态机 | 表头含「处理状态」；行内显示"处理中"；操作列有「查看 / 标记已修复」 |
| `docs/evidence/ai-unanswered.png` | ⑤ 未命中清单 | AI 客服页出现「未命中问题（补知识库用）」，1 行"被拦截（不当内容）· 你这个傻逼机器人" |
| `docs/evidence/code-generate.png` | ⑥ 编码生成 | 新建品牌弹窗内点「生成」，编码框从空变为 `BRAND-4KDVH-1983` |

## 三、代码与规则证据

| # | 项 | 落地内容 | 提交 |
|---|---|---|---|
| ① | AGENTS §13.1 | 状态必须内联切换、禁止重复启停按钮、失败保持原状态、生命周期状态不用布尔开关 | `dd8c673` |
| ② | 枚举/裸码 CI | `scripts/check-enum-inputs.py` 接入 `make contract-check`；11 处业务编码登记理由；税则辖区改国家下拉 | `dd8c673` |
| ③④ | 通知状态机 + link | `state/ack_by/ack_at/resolved_by/resolved_at/link` 列；ack/resolve 端点；前端状态列与按钮；`linkFor(eventType)` | `2735956` |
| ⑤ | 未命中清单页 | AiView 内新增表格（不新增路由） | `416e1ca` |
| ⑥ | 编码生成 | `GET /admin/v1/codes/suggest`（后端实测占用，20 次仍冲突则 503）+ 三个页面「生成」按钮 | `f159d83` |
| ⑦ | 迁移脚本 | `scripts/migrate-storage.py --verify/--copy`，逐文件 sha256；跨云明确报错 | `2053bff` |
| ⑧ | AI 接模型 | `AiMediaService.chat` + 只喂片段 + 失败 503 `AI_ANSWER_FAILED`；引用仍来自检索 | `d71ecc8` |

## 四、未验证/不适用（如实列出）

| 项 | 状态 | 原因 |
|---|---|---|
| ⑧ 真实模型答复 | 未验证 | 本机没有可用的 chat 供应商（ollama 未运行）；代码路径由 8 条单测覆盖 |
| ⑦ 跨云迁移 | 未实现 | local 与 COS/七牛之间迁移需各自 SDK 与凭据，脚本明确报错退出，不静默跳过 |
| 促销/门店启停 | 不适用 | 促销是 DRAFT/ACTIVE/ENDED 生命周期；admin 无门店列表页 |
