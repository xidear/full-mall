# 开发环境与缺失软件

日期：2026-10-18

## 当前机器已验证可用

| 软件 | 状态 |
|---|---|
| Node.js | 22.22.3 |
| npm | 12.0.2 |
| JDK | 17.0.20（本机）；JDK 21 通过缓存的 Maven 容器验证 |
| Maven | 临时 Maven 3.9.9（`C:\Users\admin\AppData\Local\Temp\fullmall-tools`） |
| WSL | Ubuntu 2，Running |
| Docker（WSL） | 29.1.3 |
| Docker Compose（WSL） | v2.29.7 |
| 浏览器 | Microsoft Edge（Playwright 使用 `msedge` channel） |
| PostgreSQL 测试 | WSL 缓存 `postgres:16-alpine`，端口 55432 |

## 当前缺失/受限

### 1. Docker Hub 出网
WSL Docker 无法直接拉 Docker Hub 镜像，报 `dial tcp ... i/o timeout`。

原因：Windows 代理为 `127.0.0.1:7897`，WSL NAT 不能直接使用宿主 localhost 代理；Docker daemon 需要单独配置代理，而修改 systemd 需要 sudo 密码。

缺失镜像：
- `postgres:17-alpine`
- `valkey/valkey:8-alpine`
- `rabbitmq:4-management-alpine`
- `axllent/mailpit`
- `node:22-alpine`
- `opensearchproject/opensearch:2`
- `chrislusf/seaweedfs:3.80`

本地验证已使用缓存的 `postgres:16-alpine` 替代 PostgreSQL 17；生产 compose 仍保持 17。

### 2. JDK 21
本机只有 JDK 17。已用缓存的 `maven:3.9.9-eclipse-temurin-21` 容器完成 Java 21 编译和打包。若要在宿主机直接运行测试，安装 Temurin 21 即可。

### 3. Playwright ffmpeg
Playwright 视频录制需要 ffmpeg；当前 `video: 'off'`，不影响截图/trace/E2E。需要视频时执行：

```bash
cd web && npx playwright install ffmpeg
```

### 4. Nuxt 依赖告警
`npm audit --omit=dev` 剩余 12 个 high，均在 Nuxt 构建/开发链（`@nuxt/cli`、`nitro-server`、`vite-builder`、`listhen`、`node-forge`、`glob` 等）。生产部署不应暴露 Nuxt dev server；等待 Nuxt 官方修复后升级。

## 配置 Docker 代理（需要 WSL sudo 密码）

在 WSL 内执行：

```bash
HOST_IP=$(ip route | awk '/default/ {print $3}')
echo "host proxy: http://$HOST_IP:7897"

sudo mkdir -p /etc/systemd/system/docker.service.d
sudo tee /etc/systemd/system/docker.service.d/http-proxy.conf >/dev/null <<EOF
[Service]
Environment="HTTP_PROXY=http://$HOST_IP:7897"
Environment="HTTPS_PROXY=http://$HOST_IP:7897"
Environment="NO_PROXY=localhost,127.0.0.1,172.17.0.0/16,172.18.0.0/16,172.19.0.0/16"
EOF

sudo systemctl daemon-reload
sudo systemctl restart docker
```

验证：

```bash
wsl -- bash -lc "curl -x http://$(ip route | awk '/default/ {print $3}'):7897 -I https://registry-1.docker.io/v2/"
cd /mnt/d/works/full-mall
docker compose --profile core --profile app up -d
```

回滚：删除 `/etc/systemd/system/docker.service.d/http-proxy.conf`，然后 `sudo systemctl daemon-reload && sudo systemctl restart docker`。

## 备选：导入镜像

如果无法改 Docker daemon 代理，可在有网络的机器拉取缺失镜像并 `docker save` / `docker load` 到 WSL。不要使用来路不明的镜像源。

## 安装命令参考

```bash
# Ubuntu/WSL：JDK 21 + Maven
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk maven

# 确认
java -version
mvn -version
```

Node 22 与 Edge 已可用；Playwright 使用 Edge 无需下载 Chromium。

## 安全提醒

- 不要把 `.env`、真实商户号、证书、私钥提交到 Git。
- 公开演示站必须使用 `demo` profile，保持只读。
- 生产环境必须配置真实认证、支付/物流适配器和 HTTPS/HSTS。
- `npm audit` 使用官方 registry；当前 npmmirror 不实现 audit API：

```bash
cd web
npm audit --omit=dev --registry=https://registry.npmjs.org
```