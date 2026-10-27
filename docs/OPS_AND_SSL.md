# 运维中心与 SSL 自动续签

## 1. 组成

- `deploy/ops/`：一个 Node 辅助容器，同时提供：
  - 主机信息 / Docker 容器状态与资源占用
  - 安全基线检查与修复建议
  - acme.sh 证书申请、自动续签、手工续签、来源/到期查询
- `deploy/edge/`：Nginx 边缘代理（80/443），HTTP-01 webroot、TLS 终止、反代 storefront/API。
- admin 后端 `/admin/v1/ops/*`：聚合多台 agent，代理证书操作。
- admin 前端「运维中心」：总览 / 容器 / 安全 / 证书 / 部署说明。

## 2. 启动

```bash
cp .env.ops.example .env.ops
# 修改 SSL_DOMAIN / SSL_EMAIL / OPS_TOKEN
docker compose --env-file .env.ops -f compose.ops.yaml --profile edge up -d --build
```

或使用部署脚本：`bash start.sh`（默认 WITH_OPS=1、WITH_EDGE=1）。

## 3. 证书

- CA：`SSL_CA=letsencrypt`（默认）/ `zerossl` / `buypass`。
- 验证方式 `SSL_CHALLENGE`：
  - `http`：webroot，需要外网 80 可达；
  - `dns`：DNSPod DNS-01，适合 80/443 不可映射（当前生产使用）。
- 自动续签：每 6 小时检查一次；剩余天数 ≤ `SSL_DAYS_BEFORE`（默认 30）时续签。
- 手工续签：admin「运维中心 → 证书 → 手工续签」，调用 `POST /api/ssl/renew`，强制 `acme.sh --renew --force`。
- 证书来源/到期：读取 `fullchain.pem` 的 issuer/notAfter，返回 `source`、`validFrom`、`validTo`、`daysLeft`、`lastRenewAt`；不返回私钥。
- 证书落盘：`fullmall-ops_ssl-certs` volume → `/certs/<domain>/fullchain.pem` + `privkey.pem`；边缘 Nginx 每 30 秒检测到变化后自动 reload。
- DNS-01 配置：`SSL_CHALLENGE=dns` + `DP_ID` / `DP_KEY`（DNSPod API Token）。
- 首次申请前提：
  - `http`：DNS 已指向公网、路由器 80→边缘机 80；
  - `dns`：域名托管在 DNSPod，Token 有 DNS 编辑权限（无需 80/443）。
- 当前已验证：`company.binnarui.com` 通过 DNS-01 签发 Let's Encrypt 证书（89 天有效），边缘 `15443` 对外提供 HTTPS。

失败不会静默：agent 的 `state.lastError` 会显示在 admin 证书页；容器日志也会打印 acme.sh 输出。

## 4. 安全基线

agent 检查并给出证据/修复建议：

- Docker socket 权限、容器 privileged/host network/docker.sock 挂载
- 镜像 latest 标签、healthcheck、restart policy
- SSH `PermitRootLogin` / `PasswordAuthentication`（读取宿主 `/etc/ssh/sshd_config`）
- ufw/firewalld/nftables、fail2ban
- 磁盘/内存压力

评分 100 分起，fail -15、warn -5、unknown -1。修复建议是命令级说明，不在 agent 里自动改宿主配置。

## 5. 多机

每台机器运行 ops（同一镜像），admin 配置：

```bash
OPS_AGENTS=local=http://127.0.0.1:41287,app=http://192.168.1.16:41287
OPS_TOKEN=<各 agent 一致>
SSL_HELPER_URL=http://127.0.0.1:41287
```

- 只有边缘机的 `SSL_DOMAIN` 非空；其他机器 `SSL_DOMAIN=`，证书页显示“本机未启用 SSL”。
- 容器操作只允许名称前缀 `OPS_MANAGE_PREFIX`（默认 `fullmall-`）或 label `com.fullmall.ops.manage=true` 的容器，避免误停系统容器。
- agent 默认监听 41287；跨机访问需防火墙放行并设置 `OPS_TOKEN`。

## 6. API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/v1/ops/overview` | 主机/容器/安全/证书/技术栈聚合 |
| GET | `/admin/v1/ops/containers` | 各主机容器 |
| GET | `/admin/v1/ops/security` | 安全基线 |
| GET | `/admin/v1/ops/ssl` | 证书状态 |
| POST | `/admin/v1/ops/ssl/renew` | 手工续签 |
| POST | `/admin/v1/ops/containers/{host}/{id}/{action}` | start/stop/restart |

## 7. 本地验证

```bash
docker compose --env-file .env.ops -f compose.ops.yaml up -d --build ops
curl -fsS http://127.0.0.1:41287/health
curl -fsS http://127.0.0.1:41287/api/ops | head -c 500
```
