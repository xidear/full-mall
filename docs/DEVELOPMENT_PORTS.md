# 开发/联调端口分配

| 服务 | 端口 | 启动 |
|---|---|---|
| 后端 API | 41280 | `bash scripts/restart-api.sh`（`APP_PORT` 可覆盖） |
| admin | 41281 | 开发：`bash scripts/admin-dev.sh start`；生产构建：`scripts/admin-preview.sh start` |
| store-console | 41282 | `npm run dev --workspace @fullmall/store-console` |
| supplier-portal | 41283 | `npm run dev --workspace @fullmall/supplier-portal` |
| support-console | 41284 | `npm run dev --workspace @fullmall/support-console` |
| storefront | 41285 | `npm run dev --workspace @fullmall/storefront` |
| mobile H5 | 41286 | `npm run dev:h5 --workspace @fullmall/mobile` |
| wiremock | 41288 | `docker compose up -d wiremock` |
| Ollama | 41289 | `bash scripts/start-local-ai.sh` |
| CLIP 多模态向量服务（可选） | 41290 | `cd tools/clip-service && npm start` |
| 本机 CPU embedding（bge-small-zh-v1.5，可选） | 41291 | `cd tools/embedding-service && npm start` |
| ops/SSL 辅助容器 | 41287 | `docker compose --env-file .env.ops -f compose.ops.yaml up -d ops` |

规则见 `AGENTS.md` §8：只用 **41280–41299（20 个）**，禁止 3000/5173/8080 及其变体；`localhost` 只是本机调试，不算对外服务。

> **端口段说明（2026-10-30 扩展）**：原段落 41280–41289 十个槽位在加入 CLIP 服务后**已用满**，
> 因此整段扩到 **41280–41299（20 个）**；当前 41291 已给本机 embedding，41292–41299 空闲。再不够时按 §8 先扩到 30 个（41280–41309）、
> 再扩到 50 个（41280–41329），**每次扩展同步本表与 AGENTS.md**，禁止零散借用其它号段。
>
> 核查方式（不要用 `/dev/tcp` 粗探测，会误报）：按端口反查进程，见 `scripts/port-owners.mjs`。
> CLIP 服务（41290）与 embedding 服务（41291）只在开发机按需启动，不属于交付组件；
> 不启动 embedding 时 AI 客服可用 KEYWORD 模式（`APP_AI_ASK_RETRIEVAL=KEYWORD`），或把
> `ai_provider`（code=`local-embedding`）的 endpoint 指向任意 OpenAI 兼容向量服务。

## 公网映射（iKuai，非 80/443）

| 公网端口 | 内网目标 | 用途 |
|---|---|---|
| 15051 | 192.168.1.15:41285 | 商城直连 HTTP |
| 15052 | 192.168.1.15:41281 | admin 直连 HTTP |
| 15080 | 192.168.1.15:80 | edge HTTP（301 → 15443） |
| 15443 | 192.168.1.15:443 | edge HTTPS（Let's Encrypt DNS-01） |

公网访问：`https://company.binnarui.com:15443`；admin：`http://company.binnarui.com:15052`。

## 局域网访问（客户端在另一台机器）

- 所有服务绑定 `0.0.0.0`，用服务器局域网 IP 访问：`http://192.168.1.15:<端口>`。
- **admin / store-console / supplier-portal / support-console**：HTTP 走**同源相对路径**（如 `/admin/v1/...`），由 Vite 代理转发到 41280，客户端无需关心后端地址；WebSocket 按 `location.hostname:41280` 推导。
- **storefront**：客户端直接请求 API，默认基址已设为 `http://192.168.1.15:41280`（可用 `NUXT_PUBLIC_API_BASE` 覆盖到域名）；后端 CORS 已允许 `localhost` 与 `192.168.1.15` 的 41281–41286。
- **mobile H5**：`uni -p h5 --port 41286 --host 0.0.0.0`；API 基址按 `location.hostname:41280` 自动推导（H5 在客户端浏览器运行）。
- 再次强调：`localhost` 只在本机有效，**不算对外地址**。
