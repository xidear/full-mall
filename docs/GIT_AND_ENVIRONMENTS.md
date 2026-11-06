# Git 仓库、凭据与开发环境

> 版本：3.1
> 日期：2026-11-06
> 适用：Full Mall 全部开发者与 AI Agent
> 变更：新增两个**发布分支** `publish/gitee`、`publish/github`（公开端的安全基线，不含私有凭据清单）；
> 修正 3.0 的错误记载——`.env` **未入库**（被 `.gitignore` 忽略，仓库只提交 `.env.example`）。

## 1. 三个仓库（两个模型，不是分支关系）

| 仓库 | 地址 | 定位 | 是否含凭据 | 推送策略 |
|---|---|---|---|---|
| Gitea（默认） | 公网 SSH `ssh://git@home.binnarui.cn:32223/tangpan/full-mall.git`；公网 Web `https://home.binnarui.cn:33000` | 公司内部完整仓库，**唯一开发源**（分支 `main` + 两个发布分支） | 含 `docs/GITEA_PRIVATE_CREDENTIALS.md`；`.env` 未入库 | **默认唯一推送目标** |
| GitHub | `https://github.com/xidear/full-mall.git` | 对外公开源码分叉 | 不含 | 仅用户明确说“推 GitHub”时，且只能从 `publish/github` 分支推送 |
| Gitee | `https://gitee.com/xidear/full-mall.git` | 对外公开源码分叉 | 不含 | 仅用户明确说“推 Gitee”时，且只能从 `publish/gitee` 分支推送 |

核心区别：

1. **Gitea 的开发分支只有一个 `main`**；另有两个**发布分支** `publish/gitee`、`publish/github`（见 §3.2），它们是“可安全推公开端”的快照，不参与日常开发。
2. **`.env` 未入库**（`.gitignore` 第 28 行忽略），仓库只提交 `.env.example`；内部同事 `cp .env.example .env` 后填本地值即可启动。
3. **GitHub 与 Gitee 是独立仓库**，各自维护自己的提交历史与发布节奏。发布分支只是“可安全外发的内容基线”，**不是** Gitea `main` 的镜像，推送后两边可独立演进。
4. 任何情况下，公开端都不得出现 `.env`、真实密钥、证书、私钥、token 与 `docs/GITEA_PRIVATE_CREDENTIALS.md`。

本地 remote（克隆 Gitea 后建议命名为 `gitea`）：

```bash
git remote -v
# gitea  ssh://git@home.binnarui.cn:32223/tangpan/full-mall.git
```

> remote URL **不得**包含用户名密码。SSH 推送使用 `~/.ssh/id_ed25519`，公钥登记在 Gitea 账号 `tangpan`（Settings → SSH / GPG Keys）；
> HTTPS 推送则交给本机凭据库：`git config credential.helper store`（值存 `~/.git-credentials`，权限 0600）。
>
> Gitea 部署事实（2026-11-03 核对）：QNAP NAS `192.168.1.3` 上的 Container Station 容器 `gitea-run`，
> 容器内 SSH 端口 `2222` 映射为 NAS `2223`，Web 端口 `3000`；公网 Web 入口 `https://home.binnarui.cn:33000`（TLS）。
> Gitea 只接受 HTTPS：`http://192.168.1.3:3000` 会返回 400。公网 SSH 入口有 **`32223`**（iKuai 32222 → 192.168.1.3:2223）与 **`22223`**（→ 192.168.1.3:2223）两条，2026-11-03 均实测认证成功。
> 历史坑：该条映射曾错指 `192.168.1.3:2222`，NAS 上没监听 2222，表现为 connection refused。

## 2. 推送规则（强制）

1. **用户说“git 推送”且没有特别说明时，只推 Gitea。**
2. 只有用户明确说“推 GitHub”“推 Gitee”时，才推对应公开端。
3. 推送前必须：
   - `git status` 确认目标分支和工作区；
   - `git diff --cached` 扫描密钥/令牌/证书/私钥；
   - 确认目标远端正确（Gitea 之外的任何远端都算公开端）；
   - GitHub 推送使用 `git -c http.version=HTTP/1.1`（HTTP/2 会间歇超时）；
   - GitHub 直连失败时可使用代理 `192.168.1.2:7893`。
4. 不强制推送 `main`；不改写已推送历史，除非用户明确要求并确认影响。
5. 公开端**永不**包含 `.env`、真实密钥、证书、私钥、token、`docs/GITEA_PRIVATE_CREDENTIALS.md`。

## 3. 分支策略

### 3.1 Gitea：开发分支 `main`

- 内容：全部代码、迁移、文档、测试、`.env.example`，以及 `docs/GITEA_PRIVATE_CREDENTIALS.md`（仅内部）。
- 目的是“内部共享方案，clone 就能构建”。
- 新机器：

```bash
git clone ssh://git@home.binnarui.cn:32223/tangpan/full-mall.git full-mall
cd full-mall
cp .env.example .env   # 按需改本机值；.env 不入库
docker compose --profile core --profile app up -d
```

- **`.env` 未入库**（`.gitignore` 忽略 `.env` 与 `.env.*`，只放行 `.env.example` 这类样例）。不要再写“入库的 `.env`”，也不要 `git add -f .env`。
- 日常改动直接提交到 `main`。

### 3.2 发布分支：`publish/gitee` 与 `publish/github`

这两个分支是“**可安全外发的内容基线**”，用来在需要时推给 Gitee / GitHub，替代“临时手挑文件”这种容易漏掉私有文件的做法。

| 分支 | 用途 | 内容差异（相对 `main`） |
|---|---|---|
| `publish/gitee` | 推送到 `https://gitee.com/xidear/full-mall.git` 的基线 | 删除 `docs/GITEA_PRIVATE_CREDENTIALS.md` |
| `publish/github` | 推送到 `https://github.com/xidear/full-mall.git` 的基线 | 同上（初始与 gitee 分支同树） |

规则：

1. **只有这两个分支允许推公开端**（`git push <public-remote> publish/gitee:main`）；`main` 只推 Gitea。
2. 推送前必须做内容扫描（见 §2 第 3 条），确认没有 `.env`、密钥、证书、私钥、token、私有凭据清单。
3. 发布分支**不参与日常开发**：`main` 有新内容时按下面流程刷新，不要在发布分支上直接写业务代码：

```bash
git checkout main && git pull --ff-only gitea main
git checkout publish/gitee
git merge --no-ff main            # 冲突通常只在 docs/GITEA_PRIVATE_CREDENTIALS.md（应保持已删除）
git rm -q --cached docs/GITEA_PRIVATE_CREDENTIALS.md 2>/dev/null || true
git commit -m "chore(publish): 同步 main 并保持不含私有凭据清单"
git push gitea publish/gitee
```

4. 两个公开仓库是**独立仓库**，各自有提交历史与发布节奏；发布分支只是安全基线，**不是**镜像。推过去之后两边可独立演进。
5. `publish/github` 首次推送用 `git -c http.version=HTTP/1.1 push github publish/github:main`（HTTP/2 间歇超时），失败可加代理 `192.168.1.2:7893`。

### 3.3 里程碑标签（tag）

- 每完成一个可交付批次，在 `main` 上打一个附注标签，形如 `milestone/<yyyy-mm-dd>-<主题>`（例：`milestone/2026-11-06-demo-ready`）。
- 标签是“这一批做完了、可回溯”的锚点；发布分支从标签所在提交派生，便于回答“公开端这份代码对应哪一批”。
- 已有标签：`archive/spike-spring-boot-4`（早期技术验证归档，不再维护）。

### 3.4 `.gitignore` 策略

- `.gitignore` 继续忽略 `.env` 与 `.env.*`：这防止在公开侧或误操作中新增环境文件。
- 不随意忽略业务源码、迁移、文档、测试、素材清单、接口契约。
- 运行产物继续忽略：`target/`、`node_modules/`、`.nuxt/`、`.output/`、`dist/`、日志、Playwright 报告。
- `docker-data/` 属于运行数据，不入库；需要备份时走数据库/对象存储备份流程。
- `.zcode/` 为 Agent 本地配置，不入仓库；需要交接的内容写入 `docs/PROJECT_MEMORY.md`。

## 4. 凭据清单（Key Inventory）

> 原则：**文档不记录明文凭据值**。位置与键名记录在此；开发值只存在于各人本地 `cp .env.example .env` 后的 `.env`（不入库）；生产值只存在于部署环境或密钥管理系统。
> 公开仓库只有 `.env.example`，其中的值必须是无害占位。

### 4.1 Git / CI

| Key | 用途 | 放置位置 |
|---|---|---|
| Gitea 凭据（用户名/密码） | clone/push Gitea | 本机凭据库 `~/.git-credentials`；**不写 remote URL、不写 `.git/config`** |
| `GITEA_URL` / `GITEA_USERNAME` / `GITEA_PASSWORD` | Gitea API 与脚本 | Gitea `.env`（仅内部仓库） |
| `GITHUB_USERNAME` / `GITHUB_TOKEN` | 推送 GitHub | 本机凭据库或凭据管理器；公开侧 CI 用 secret |
| `GITEE_USERNAME` / `GITEE_TOKEN` | 推送 Gitee | 本机凭据库或凭据管理器；公开侧 CI 用 secret |

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
| `SUPPLIER_CREDENTIAL_KEY` | 供应商 secret AES-GCM 加密 | dev 使用派生 key 并告警 | 必须配置 Base64 32 字节；缺失启动失败 |
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
| `UPLOAD_MULTIPART_MAX_FILE_SIZE` | 单文件解析层天花板（Spring multipart），必须等于 `ValidationRules.UPLOAD_MULTIPART_CEILING_BYTES`，不一致启动失败 | 默认 `64MB` |
| `UPLOAD_MULTIPART_MAX_REQUEST_SIZE` | 单请求总上限（文件 + multipart 头） | 默认 `72MB` |

> 图片/视频的**业务上限**不在环境变量里：它们存在 `system_setting`（键 `upload.admin-image-max-bytes` 等），
> 由后台「系统设置 → 素材上传」修改，保存后下一次上传生效。环境变量只负责解析层天花板。

### 4.7 供应商 API

| Key | 用途 | 当前状态 |
|---|---|---|
| `SUPPLIER_TIMESTAMP_SKEW_SECONDS` | HMAC 时间窗 | 默认 300 |
| `SUPPLIER_TOKEN_TTL_SECONDS` | 供应商 token 有效期 | 规划 |
| `SUPPLIER_IP_ALLOWLIST_ENABLED` | 是否启用 IP 白名单 | 规划 |
| `SUPPLIER_WEBHOOK_RETRY_MAX` | Webhook 最大重试 | 规划 |
| `SUPPLIER_WEBHOOK_BACKOFF_MS` | Webhook 退避基数 | 规划 |

## 5. 新机器启动

### 5.1 Gitea 内部开发（推荐）

```bash
git clone ssh://git@home.binnarui.cn:32223/tangpan/full-mall.git full-mall
cd full-mall
docker compose --profile core --profile app --profile tools up -d
```

`.env` 已随仓库提供，无需创建。浏览器：

- 商城 http://localhost:41285
- 后台 http://localhost:41281
- 门店 http://localhost:5174
- 供应商 http://localhost:5175
- 客服 http://localhost:5176
- API http://localhost:41280

### 5.2 公开分叉（GitHub / Gitee）

```bash
git clone https://github.com/xidear/full-mall.git full-mall
cd full-mall
cp .env.example .env
# 填写你自己的凭据与应用密钥；不要使用他人密钥，不要把 .env 提交
```

### 5.3 本机无 Docker 时的最小运行

见 `docs/PROJECT_MEMORY.md` 第 8 节。

## 6. 推送检查清单

- [ ] 目标远端是 Gitea（默认）还是用户明确指定的公开端？
- [ ] 当前分支是 `main`，且 `git status` 无意外文件？
- [ ] `git diff --cached` 无真实密钥/证书/token（Gitea 的 `.env` 除外，它只允许进 Gitea）？
- [ ] 若目标是公开端：确认 `.env`、`docs/GITEA_PRIVATE_CREDENTIALS.md` 不在本次提交中？
- [ ] GitHub 推送使用 HTTP/1.1；失败时用代理？
- [ ] 推送后核对 `git ls-remote` 的 commit 与本地一致？