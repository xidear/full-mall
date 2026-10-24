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

---

## 已实施（按确认：A + 路径 1）

| 改动 | 位置 |
|---|---|
| lite 独立 project | `Makefile`：`up-lite` 用 `COMPOSE_PROJECT_NAME=fullmall-lite`，并新增 `down-lite` |
| api/worker 以宿主 UID 运行 | `compose.yaml`：`user: "${APP_UID:-1000}:${APP_GID:-1000}"` |
| Maven 仓库改可写路径 | `HOME=/tmp`、`MAVEN_CONFIG=/tmp/.m2`，卷改为 `maven-cache:/tmp/.m2` |
| 变量入 `.env` | `APP_UID=1000`、`APP_GID=1000` |

### 首次启用注意

`maven-cache` 卷历史上由 root 创建，切换用户后**可能不可写**。首次请执行一次：

```bash
docker compose down            # 不改卷
docker volume rm fullmall_maven-cache   # 仅删缓存卷，安全
docker compose --profile app up -d
```

### 验证步骤

1. `make up-lite` 与 `docker compose --profile app up -d` **可同时运行**（`docker ps` 里两套容器并存）；
2. `ls -n server | head` 不应出现 `0 0`（root:root）属主；
3. `make reset` 后重新登录，cookie/会话仍正常。

### 实机验证结果（本机 Docker 29.7.2 / Compose v5.6.0）

| 检查 | 结果 |
|---|---|
| `docker compose -f compose.yaml config -q` | ✅ 通过 |
| `COMPOSE_PROJECT_NAME=fullmall-lite docker compose -f compose.lite.yaml config -q` | ✅ 通过，project 解析为 `fullmall-lite` |
| **两套同时 up** | ✅ 成功（core 5 容器 + lite 5 容器并存） |
| 端口隔离 | core `5432/5672/15672`；lite `15432/15674/15675/17700/11025` |
| `user:` 指令 | ✅ 在 `--profile app` 下生效（`docker compose config --profile app` 可查到 `user: 1000:1000`） |

**过程中发现的两个真问题（已修）**

1. 只改 project 名不够：`.env` 里的 `RABBITMQ_PORT/POSTGRES_PORT/...` 会被 lite 复用 → 端口仍冲突。
   已把 lite 变量全部改为 `LITE_*` 前缀（`LITE_POSTGRES_PORT` 等），彻底与 core 解耦。
2. 默认端口也踩到 core 的 rabbitmq 管理端口（15672），已把 lite AMQP/管理端口默认设为 `15674/15675`。

> 注：`--profile app` 才包含 api/worker，因此不带 profile 执行 `config` 时看不到 `user:` 是正常的。

---

## 最终口径（业务确认）

> **lite 只是"没有 Docker 时的权宜之计"，不与 core 并行运行。**

因此：

| 措施 | 说明 |
|---|---|
| `make up-lite` 前置 `check-lite-exclusive` | core 有容器在跑（`docker compose ps -q` 非空）就**明确报错退出** |
| lite 端口变量 `LITE_*`（15432/15674/15675/17700/11025） | **防御性**设置：即使误开也不会抢占 core 端口 |
| project 名 `fullmall-lite` | 容器/网络不与 core 混在一起，便于一次性 `down` |

### 清理历史遗留（一次性）

早期 lite 曾用共享 project（`fullmall`）运行，会留下这些容器，导致 `make up-lite` 的互斥检查判定"core 在跑"：

```bash
docker rm -f fullmall-postgres-1 fullmall-valkey-1 fullmall-rabbitmq-1 \
              fullmall-mailpit-1 fullmall-meilisearch-1
```

> meilisearch 只属于 lite，出现在 `fullmall-*` 前缀下即是历史遗留；core 用 opensearch。

## bind mount 属主（P6，已实机验证）

**问题**：容器默认以 root 运行，写宿主机 bind mount 后产生 `root:root` 文件，宿主用户（uid 1000）**删不掉**。
实测：`wiremock` 未设 `user:` 时写入 `deploy/wiremock/.owner-probe` 属主为 `0:0`，宿主 `rm` 报 Permission denied。

**修复**：

1. 所有带 bind mount 的服务显式指定 `user: "${APP_UID:-1000}:${APP_GID:-1000}"`（wiremock 已补）。
2. 首次部署/换机器后执行一次属主修正：
   ```bash
   make fix-mount-owner
   ```
3. 复验：容器内 `id` 应为 `uid=1000`，写入文件宿主可删除。

**另外修掉一个镜像 tag 错误**：`wiremock/wiremock:3` 在 Docker Hub 不存在（pull 直接失败），已固定为 `3.9.1`。
