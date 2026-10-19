# Git 下游、密钥与开发环境

> 版本：2.0
> 日期：2026-10-19
> 适用：Full Mall 全部开发者与 AI Agent
> 注意：本文进入 Gitea；GitHub 公开仓库只保留不含密钥的版本。

## 1. 三个 Git 下游

| 下游 | 地址 | 角色 | 推送策略 |
|---|---|---|---|
| Gitea（默认） | `https://company.binnarui.com:8148/xidear/full-mall.git` | 私有主开发仓库，包含完整开发信息 | **默认推送目标**；`main` + `gitea-dev` |
| GitHub | `https://github.com/xidear/full-mall.git` | 公开源码仓库 | 仅在用户明确说“推 GitHub”或“三端都推”时推送；不得包含 `.env`/密钥 |
| Gitee | `https://gitee.com/xidear/full-mall.git` | 公开镜像 | 不手动推送；由 GitHub Actions `mirror-gitee.yml` 在 GitHub `main`/tag 更新后强制同步 |

本地 remote：

```bash
git remote -v
# gitea  https://company.binnarui.com:8148/xidear/full-mall.git
# github https://github.com/xidear/full-mall.git
```

## 2. 推送规则（强制）

1. **用户说“git 推送”且没有特别说明时，只推 Gitea。**
2. 只有用户明确说“推 GitHub”“推 Gitee”“三个端都推”时，才推对应远端。
3. Gitea 包含完整开发信息（含 `gitea-dev` 分支的 `.env`）；GitHub 只能有公开安全内容。
4. `gitea-dev` 分支**永远不要**推到 GitHub/Gitee。
5. 推送前必须：
   - `git status` 确认目标分支和工作区；
   - 扫描密钥/令牌/证书；
   - 确认没有把 `gitea-dev` 推到公开远端；
   - GitHub 推送使用 `git -c http.version=HTTP/1.1`（HTTP/2 会间歇超时）；
   - GitHub 直连失败时可使用代理 `192.168.1.2:7893`。
6. 不强制推送公共 `main`；不改写已推送历史，除非用户明确要求并确认影响。

## 3. Gitea 分支策略

### 3.1 `main`

- 公开安全内容：代码、迁移、文档、测试、`.env.example`。
- 不含 `.env`、真实密钥、证书、私钥、生产 token。
- 可随时推 GitHub/Gitee。

### 3.2 `gitea-dev`

- 从 `main` 派生，额外包含 **`.env`**（本地可直接运行）和 Gitea 专用开发配置。
- 新机器可直接：

```bash
git clone -b gitea-dev https://company.binnarui.com:8148/xidear/full-mall.git full-mall
cd full-mall
docker compose --profile core --profile app up -d
```

无需再创建 `.env`。

- 更新流程：

```bash
git checkout main
git pull gitea main
git checkout gitea-dev
git merge main
# 解决 .env 冲突（保留本地/开发值）
git push gitea gitea-dev
```

- 禁止：

```bash
git push github gitea-dev      # 禁止
git push gitee gitea-dev       # 禁止
```

### 3.3 `.gitignore` 策略

- `main` 继续忽略 `.env`、`.env.*`、`*.pem`、`*.key`、`secrets/`，防止公开泄漏。
- `gitea-dev` 上的 `.env` 是**已跟踪文件**，`.gitignore` 不影响已跟踪文件；用 `git add -f .env` 首次加入。
- 不随意忽略业务源码、迁移、文档、测试、素材清单、接口契约。
- 运行产物继续忽略：`target/`、`node_modules/`、`.nuxt/`、`.output/`、`dist/`、日志、Playwright 报告。
- `docker-data/` 属于运行数据，不入库；需要备份时走数据库/对象存储备份流程。
- `.zcode/` 为 Agent 本地配置，不入公开仓库；需要交接的内容写入 `docs/PROJECT_MEMORY.md`。

## 4. 密钥清单（Key Inventory）

> 原则：仓库不伪造真实密钥。以下列出**键名、用途、当前状态、放置位置**。真实生产密钥只放 Gitea `gitea-dev` 的 `.env` 或部署环境/密钥管理系统，绝不进 GitHub。

### 4.1 Git / CI

| Key | 用途 | 当前状态 | 放置位置 |
|---|---|---|---|
| `GITEE_TOKEN` | GitHub Actions 同步 Gitee | 只以 secret 名出现，工作区无值 | GitHub 仓库 Settings → Secrets → Actions |
| GitHub PAT / credential | 推送 GitHub | 由 Windows 凭据管理器/HTTPS 凭据提供，未写入仓库 | 本机凭据管理器 |
| Gitea token / credential | 推送 Gitea | 由本机凭据管理器提供，未写入仓库 | 本机凭据管理器 |

### 4.2 本地基础设施（demo/local only）

| Key | 本地值 | 说明 |
|---|---|---|
| `POSTGRES_DB` | `fullmall` | 本地库名 |
| `POSTGRES_USER` | `fullmall` | 本地用户 |
| `POSTGRES_PASSWORD` | `change-me-local` | 仅本地；生产必须替换 |
| `RABBITMQ_DEFAULT_USER` | `fullmall` | 本地 |
| `RABBITMQ_DEFAULT_PASS` | `change-me-local` | 仅本地 |
| `PGADMIN_DEFAULT_EMAIL` | `admin@example.com` | 本地 |
| `PGADMIN_DEFAULT_PASSWORD` | `change-me-local` | 仅本地 |

### 4.3 应用安全

| Key | 用途 | 当前状态 | 生产要求 |
|---|---|---|---|
| `SUPPLIER_CREDENTIAL_KEY` | 供应商 secret AES-GCM 加密 | 空；dev 使用派生 key 并告警 | 必须配置 Base64 32 字节；缺失启动失败 |
| `STORE_AUTH_REQUIRED` | 门店接口鉴权 | dev false / prod true | prod 必须 true |
| `SUPPORT_AUTH_REQUIRED` | 客服接口鉴权 | dev false / prod true | prod 必须 true |
| `POS_REQUIRE_MANAGER_APPROVAL` | 退款店长授权 | dev false / prod true | prod 必须 true |

生成 `SUPPLIER_CREDENTIAL_KEY`：

```bash
openssl rand -base64 32
```

### 4.4 支付

| Key | 用途 | 当前状态 |
|---|---|---|
| `PAYMENT_PROVIDER` | `mock` / `wechat` / `alipay` / `unionpay` / `aggregator` | 默认 `mock` |
| `PAYMENT_WECHAT_MCH_ID` | 微信商户号 | 未配置 |
| `PAYMENT_WECHAT_APP_ID` | 微信 AppID | 未配置 |
| `PAYMENT_WECHAT_API_V3_KEY` | 微信 API v3 key | 未配置 |
| `PAYMENT_WECHAT_PRIVATE_KEY_PATH` | 微信商户私钥 | 未配置 |
| `PAYMENT_WECHAT_CERT_SERIAL` | 微信证书序列号 | 未配置 |
| `PAYMENT_WECHAT_PLATFORM_PUBLIC_KEY_PATH` | 微信平台公钥 | 未配置 |
| `PAYMENT_ALIPAY_APP_ID` | 支付宝 AppID | 未配置 |
| `PAYMENT_ALIPAY_PRIVATE_KEY_PATH` | 支付宝应用私钥 | 未配置 |
| `PAYMENT_ALIPAY_PUBLIC_KEY_PATH` | 支付宝公钥 | 未配置 |
| `PAYMENT_UNIONPAY_MER_ID` | 银联商户号 | 未配置 |
| `PAYMENT_UNIONPAY_CERT_PATH` | 银联证书 | 未配置 |
| `PAYMENT_AGGREGATOR_MERCHANT_ID` | 聚合收单商户号 | 未配置 |
| `PAYMENT_AGGREGATOR_KEY` | 聚合收单密钥 | 未配置 |

生产模式缺失这些配置时，支付/退款接口必须明确报错，禁止回退 Mock。

### 4.5 AI / 客服

| Key | 用途 | 当前状态 |
|---|---|---|
| `AI_PROVIDER` | `mock` / `openai` / 其他 | 默认 `mock` |
| `AI_BASE_URL` | AI 服务地址 | 未配置 |
| `AI_API_KEY` | AI 密钥 | 未配置 |
| `AI_MODEL` | 模型名 | 未配置 |
| `AI_EMBEDDING_MODEL` | 向量模型 | 未配置 |
| `AI_RAG_ENABLED` | 是否启用 RAG | 默认 false |

生产 `AI_PROVIDER` 非 mock 且缺 key 时必须报错，不得返回空答案。

### 4.6 对象存储 / 素材库

| Key | 用途 | 当前状态 |
|---|---|---|
| `STORAGE_PROVIDER` | `local` / `seaweedfs` / `s3` / `qiniu` / `aliyun` | 默认 `local` |
| `S3_ENDPOINT` | S3/SeaweedFS 地址 | 本地默认 |
| `S3_ACCESS_KEY` | S3 Access Key | 本地占位 |
| `S3_SECRET_KEY` | S3 Secret Key | 本地占位 |
| `S3_BUCKET` | 桶名 | 本地默认 |
| `QINIU_ACCESS_KEY` | 七牛 AK | 未配置 |
| `QINIU_SECRET_KEY` | 七牛 SK | 未配置 |
| `QINIU_BUCKET` | 七牛空间 | 未配置 |
| `QINIU_DOMAIN` | 七牛 CDN 域名 | 未配置 |
| `ALIYUN_OSS_ACCESS_KEY_ID` | 阿里云 AK | 未配置 |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | 阿里云 SK | 未配置 |
| `ALIYUN_OSS_BUCKET` | OSS Bucket | 未配置 |
| `ALIYUN_OSS_ENDPOINT` | OSS Endpoint | 未配置 |

### 4.7 供应商 API

| Key | 用途 | 当前状态 |
|---|---|---|
| `SUPPLIER_TIMESTAMP_SKEW_SECONDS` | HMAC 时间窗 | 默认 300 |
| `SUPPLIER_TOKEN_TTL_SECONDS` | 供应商 token 有效期 | 规划 |
| `SUPPLIER_IP_ALLOWLIST_ENABLED` | 是否启用 IP 白名单 | 规划 |
| `SUPPLIER_WEBHOOK_RETRY_MAX` | Webhook 最大重试 | 规划 |
| `SUPPLIER_WEBHOOK_BACKOFF_MS` | Webhook 退避基数 | 规划 |

## 5. 新机器启动

### 5.1 Gitea 快速开发（推荐）

```bash
git clone -b gitea-dev https://company.binnarui.com:8148/xidear/full-mall.git full-mall
cd full-mall
docker compose --profile core --profile app --profile tools up -d
```

浏览器：

- 商城 http://localhost:3000
- 后台 http://localhost:5173
- 门店 http://localhost:5174
- 供应商 http://localhost:5175
- 客服 http://localhost:5176
- API http://localhost:8080

### 5.2 GitHub 公开仓库

```bash
git clone https://github.com/xidear/full-mall.git full-mall
cd full-mall
cp .env.example .env
# 按需填写；生产密钥不要提交
```

### 5.3 本机无 Docker 时的最小运行

见 `docs/PROJECT_MEMORY.md` 第 8 节：WSL Docker + Maven 容器 + `fullmall-postgres-test` + `fullmall-api-pos`。

## 6. 推送检查清单

- [ ] 目标远端是 Gitea（默认）还是用户明确指定的 GitHub/Gitee？
- [ ] 当前分支是否正确？`gitea-dev` 绝不推公开端。
- [ ] `git status` 无意外文件。
- [ ] `git diff --cached` 无真实密钥/证书/token。
- [ ] GitHub 推送使用 HTTP/1.1；失败时用代理。
- [ ] 推送后核对 `git ls-remote` 的 commit 与本地一致。
- [ ] Gitee 只等 Actions 镜像，不手动推。
