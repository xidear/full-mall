# core / lite 两套 compose 的隔离与"cookie 问题"根治

## 1. 现状（已确认的事实）

| 事实 | 影响 |
|---|---|
| `.env` 里 `COMPOSE_PROJECT_NAME=fullmall` | **覆盖**了 `compose.lite.yaml` 里想用的 `name:`，两套共用同一个 project |
| 同一 project → 同一容器的服务名/网络 | core 与 lite **不能同时跑** |
| 两者都启动 rabbitmq | **core 起 rabbitmq 时 lite 必须停**，否则端口/容器冲突 |
| 卷名带前缀（`lite-postgres-data`） | 数据本身不互相覆盖，冲突在**运行态**（容器名、端口、网络） |

## 2. 推荐的根治方向（二选一）

### 方案 A：lite 用独立 project（改 Makefile/脚本，最干净）

```bash
# 运行 lite 时显式覆盖项目名
COMPOSE_PROJECT_NAME=fullmall-lite docker compose -f compose.lite.yaml up -d
```

- 优点：两套可同时运行，容器名/网络/端口互不干扰；
- 代价：lite 的数据卷前缀会变成 `fullmall-lite_*`（现有 lite 数据需迁移一次，或接受重建）。

### 方案 B：保持共用 project，但强制互斥（最小改动）

在 `Makefile` 里给 core/lite 的 up 目标加互斥检查：启动前若另一套在跑则**明确报错并退出**，
而不是让 docker compose 自己撞出一堆难懂的错误。

## 3. "cookie 问题"为什么 `make reset` 后会复发

根因是**容器以 root 写入 bind mount**（`./server`、`./data` 之类），宿主机上留下 root 属主的文件；
下一次以非 root 运行时读不到/写不了，表现成登录态（cookie/会话文件）异常。

两条治理路径：

### 路径 1：让应用以宿主 UID 运行（推荐，一次性）

```yaml
  api:
    user: "${APP_UID:-1000}:${APP_GID:-1000}"
    environment:
      HOME: /tmp          # maven 需要可写 HOME
      MAVEN_CONFIG: /tmp/.m2
    volumes:
      - ./server:/workspace
      - maven-cache:/tmp/.m2
```

注意：`maven-cache` 卷已存在时属主是 root，需要先 `docker compose down -v`（会删该缓存）或手改属主。

### 路径 2：加一个 init 容器统一修属主（不改应用运行用户）

```yaml
  fix-perms:
    image: alpine:3.20
    profiles: [app]
    command: ["sh", "-c", "chown -R ${APP_UID:-1000}:${APP_GID:-1000} /workspace /data || true"]
    volumes:
      - ./server:/workspace
      - ./data:/data
    restart: "no"
```

在 `api.depends_on` 里加 `fix-perms: {condition: service_completed_successfully}`。

- 优点：不动应用运行身份，风险小；
- 缺点：每次 up 都跑一次（幂等，开销很小）。

## 4. 我需要你确认的两件事

1. **选 A 还是 B**（lite 独立 project vs 强制互斥）；
2. **cookie 治理选路径 1 还是 2**（改运行用户 vs init 容器修属主）。

确认后我改 compose/Makefile，并在文档里补"验证步骤"（同时起两套 / `make reset` 后重新登录仍正常）。
