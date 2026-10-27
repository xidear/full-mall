# 分布式部署与初始化脚本

本文件说明 `init.sh` / `setup.sh` / `start.sh` / `stop.sh` 的设计、幂等规则、数据库主从与多机部署方式。

## 1. 脚本职责

| 脚本 | 职责 | 是否动数据 |
|---|---|---|
| `init.sh` | 安装 Docker、配置 docker 组、按区域配置镜像加速、准备目录、登录私有仓库、拉取基础镜像 | 不删数据 |
| `setup.sh` | 生成 `.env` / `.env.ops`、构建或拉取产物、初始化数据库、启动应用、写入 `site_domain` | 只 upsert 配置/创建缺失对象 |
| `start.sh` | 启动 core/app（可选 tools/obs/ai）与 ops/edge | 不删数据 |
| `stop.sh` | 停止容器 | 保留全部 volume，绝不执行 `down -v` |

所有脚本失败即停，并打印失败命令与行号（`[禁止静默兜底]`）。

## 2. 配置

复制模板并修改：

```bash
cp deploy/deploy.env.example .env.deploy
# 必改：CODE_DIR、DATA_DIR、RUN_USER、SITE_DOMAIN、SSL_DOMAIN、POSTGRES_PASSWORD、OPS_TOKEN
```

- `.env.deploy`：部署参数（域名、数据库模式、区域、镜像仓库）。
- `.env`：应用运行时参数（compose 使用）；`setup.sh` 会补齐域名/URL/OPS 等键，但不会覆盖已有非占位值，除非 `--force-env`。
- `.env.ops`：ops/SSL/edge 参数；`setup.sh` 自动生成。
- `deploy/hosts.conf`：多机清单（从 `deploy/hosts.conf.example` 复制）。

## 3. 单机部署

```bash
sudo bash init.sh --yes
sudo bash setup.sh --yes
bash start.sh
bash stop.sh
```

幂等状态保存在 `$DATA_DIR/.deploy-state`。每个步骤记录“执行后的结果指纹”（Docker 版本、镜像清单哈希、前端源码哈希等）；下次状态一致直接打印 `跳过 ...`。

常用选项：

```bash
bash init.sh --dry-run --data-dir /opt/fullmall --code-dir /data/full-mall
bash setup.sh --skip-init --no-build --db-mode external --domain company.binnarui.com:443
bash start.sh --with-tools
bash stop.sh --all --host-api
```

## 4. 产物模式（代码与镜像挂载）

`DEPLOY_ARTIFACT_MODE`：

- `mounted`（默认，推荐）：镜像只提供运行时，代码/构建产物用 volume 挂载：
  - API/worker/ws：挂载 `server/target/fullmall-server-0.1.0-SNAPSHOT.jar`
  - storefront：挂载 `web/apps/storefront/.output`
  - admin/console：挂载各自 `dist`，`deploy/nginx-spa.conf` 提供 SPA fallback 与同源 API 代理
  - 更新代码后重新构建产物并重启即可，无需重建镜像。
- `image`：使用 `API_IMAGE` / `ADMIN_IMAGE` / `STOREFRONT_IMAGE`；`setup.sh` 会先 `docker login`（若配置了 `DOCKER_REGISTRY`）再拉取。
- `dev`：完全沿用 `compose.yaml` 的源码挂载开发模式。

## 5. 数据库模式

`DB_MODE`：

### local（默认）

启动本机 `postgres` 容器，端口 `POSTGRES_PORT`。不删除已有 volume；密码变更前请确认 volume 是用哪个密码初始化的。

```bash
sudo bash setup.sh --yes --db-mode local
```

### external

使用已有主库，不启动本机 postgres：

```bash
DB_MODE=external DB_PRIMARY_HOST=10.0.0.10 DB_PRIMARY_PORT=5432 sudo bash setup.sh --yes
```

`setup.sh` 写入 `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` 到 `.env`，compose 通过变量注入 API。

### primary-replica（本机主从）

```bash
sudo bash setup.sh --yes --db-mode primary-replica
```

- 使用 `compose.db-ha.yaml`：`postgres-primary`（5432）+ `postgres-replica`（5433）。
- 复制账号由 `POSTGRES_REPLICATION_USER/PASSWORD` 配置；从库首次启动执行 `pg_basebackup`，之后流复制。
- 应用仍写主库；从库用于只读查询/备份/故障切换准备。切换主库需人工 promote（本脚本不自动切换，避免脑裂）。
- 项目名与主 compose 同为 `fullmall`，共享网络，API 通过 `postgres-primary:5432` 访问。

## 6. 多机（分布式）

1. 每台机器 clone 仓库到相同绝对路径（例如 `/data/full-mall`），配置 SSH key 免密。
2. 每台机器执行 `sudo bash init.sh --yes`（或至少确保 Docker 可用）。
3. 在边缘机配置 `deploy/hosts.conf`：

```text
edge|root@192.168.1.15|edge,ops|/data/full-mall|/opt/fullmall
app|root@192.168.1.16|app|/data/full-mall|/opt/fullmall
db-primary|root@192.168.1.17|db-primary,app|/data/full-mall|/opt/fullmall
db-replica|root@192.168.1.18|db-replica|/data/full-mall|/opt/fullmall
```

4. 分发：

```bash
DEPLOY_MODE=distributed bash setup.sh --yes   # setup 后自动调用 remote.sh
bash scripts/deploy/remote.sh start
bash scripts/deploy/remote.sh stop
```

5. 管理端聚合多机 agent：

```bash
OPS_AGENTS=local=http://127.0.0.1:41287,app=http://192.168.1.16:41287,db=http://192.168.1.17:41287
OPS_TOKEN=<与各 agent 一致>
```

每台机器只启动 ops（不需要 edge）：

```bash
docker compose --env-file .env.ops -f compose.ops.yaml up -d --build ops
```

只有边缘机启动 edge（容器内 80/443，公网由 iKuai 映射到 15080/15443）：`bash start.sh` 默认会启动 ops+edge；远程应用机用 `bash start.sh --no-edge`。

## 7. 端口与防火墙

- 边缘机：容器内 80/443；公网使用非典型端口（本机为 15080/15443，80/443 不占用）。
  - `15080 → 边缘 80`（HTTP 跳转）
  - `15443 → 边缘 443`（HTTPS，Let's Encrypt DNS-01 证书）
  - `15051 → storefront 41285`（直连）
  - `15052 → admin 41281`（直连）
- 应用/后端：41280-41289 段（或由 `.env` 覆盖）。
- ops-agent：41287。
- 数据库/Redis/RabbitMQ 不要直接暴露公网；跨机访问用内网 + 防火墙白名单。

## 8. 回滚

- 配置回滚：修改 `.env` / `.env.deploy` 后重新 `setup.sh`。
- 应用回滚：切回上一个镜像 tag 或上一次 jar/dist，然后 `start.sh`。
- 数据回滚：脚本从不自动删卷；使用 `pg_dump`/volume 备份恢复。`make reset` 是显式破坏性操作，不在部署脚本中调用。
- 端口映射回滚见 `docs/IKUAI_PORT_MAPPING_RECON.md`。
