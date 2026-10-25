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
