# 开发/联调端口分配

| 服务 | 端口 | 启动 |
|---|---|---|
| 后端 API | 41280 | `bash scripts/restart-api.sh`（`APP_PORT` 可覆盖） |
| admin | 41281 | `npm run dev --workspace @fullmall/admin` |
| store-console | 41282 | `npm run dev --workspace @fullmall/store-console` |
| supplier-portal | 41283 | `npm run dev --workspace @fullmall/supplier-portal` |
| support-console | 41284 | `npm run dev --workspace @fullmall/support-console` |
| storefront | 41285 | `npm run dev --workspace @fullmall/storefront` |
| mobile H5 | 41286 | `npm run dev:h5 --workspace @fullmall/mobile` |
| wiremock | 41288 | `docker compose up -d wiremock` |
| Ollama | 41289 | `bash scripts/start-local-ai.sh` |

规则见 `AGENTS.md` §8：只用 41280–41289，禁止 3000/5173/8080 及其变体；`localhost` 只是本机调试，不算对外服务。

## 局域网访问（客户端在另一台机器）

- 所有服务绑定 `0.0.0.0`，用服务器局域网 IP 访问：`http://192.168.1.15:<端口>`。
- **admin / store-console / supplier-portal / support-console**：HTTP 走**同源相对路径**（如 `/admin/v1/...`），由 Vite 代理转发到 41280，客户端无需关心后端地址；WebSocket 按 `location.hostname:41280` 推导。
- **storefront**：客户端直接请求 API，默认基址已设为 `http://192.168.1.15:41280`（可用 `NUXT_PUBLIC_API_BASE` 覆盖到域名）；后端 CORS 已允许 `localhost` 与 `192.168.1.15` 的 41281–41286。
- **mobile H5**：`uni -p h5 --port 41286 --host 0.0.0.0`；API 基址按 `location.hostname:41280` 自动推导（H5 在客户端浏览器运行）。
- 再次强调：`localhost` 只在本机有效，**不算对外地址**。
