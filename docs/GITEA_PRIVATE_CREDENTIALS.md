# 凭据清单与放置规则（仅 Gitea 内部仓库）

> **本文件只存在于私有 Gitea 仓库，禁止推送到 GitHub/Gitee。**
> 原则：**本文不记录任何明文凭据值**。本仓库（Gitea）是公司内部完整仓库，`.env` 已入库并含可直接运行的开发凭据，clone 即可构建。
> 公开的 GitHub/Gitee 仓库永不包含本文件、`.env` 与任何真实密钥。

## 1. 三个仓库的关系

| 仓库 | 地址 | 定位 | 是否含凭据 |
|---|---|---|---|
| Gitea（内部，唯一开发源） | 局域网 SSH `ssh://git@192.168.1.3:2223/tangpan/full-mall.git`；Web `https://home.binnarui.cn:33000` | 内部完整仓库，**单分支 `main`** | 是：`.env` 入库 |
| GitHub（公开分叉） | `https://github.com/xidear/full-mall.git` | 对外源码，独立提交历史 | 否：只有 `.env.example` |
| Gitee（公开分叉） | `https://gitee.com/xidear/full-mall.git` | 对外源码，独立提交历史 | 否：只有 `.env.example` |

- Gitea **只有一个分支 `main`**，不存在 `gitea-dev`。
- GitHub 与 Gitee 是**各自独立的分叉**，不是从 Gitea 派生的分支，提交逻辑与 Gitea 不同。
- 内部凭据只在 Gitea 的 `.env` 与本机凭据库中；公开仓库使用者用自己的凭据构建。

## 2. 凭据位置（不记录值）

| 项 | 位置 | 说明 |
|---|---|---|
| Gitea 用户名/密码 | 本机凭据库 `~/.git-credentials`（`credential.helper=store`）；SSH 推送优先，见 `~/.ssh/config` 的 `gitea` 主机别名 | 不写入 `.git/config`、不写入 remote URL |
| Gitea 开发用凭据 | 本仓库 `.env` 的 `GITEA_USERNAME` / `GITEA_PASSWORD` | 仅内部仓库 |
| GitHub 推送凭据 | 本机凭据库或本机凭据管理器 | 公开仓库，禁止写入仓库文件 |
| Gitee 凭据 | 本机凭据库或 CI secret | 公开仓库，禁止写入仓库文件 |
| 应用/基础设施密钥 | 本仓库 `.env`（仅本地开发值）+ 部署环境或密钥管理系统 | 生产值不进任何仓库 |

查看本机已保存的凭据条目（只显示用户名，不显示密码）：

```bash
git credential fill <<'EOF'
protocol=https
host=home.binnarui.cn:33000
EOF
```

SSH 推送（推荐，无需每次输入密码）：

```bash
ssh -T -p 2223 git@192.168.1.3    # 期望输出 Hi there, tangpan! You've successfully authenticated...
```

## 3. 使用方式

默认推送只推 Gitea：

```bash
git push gitea main
```

只有用户明确要求时才推公开端：

```bash
git -c http.version=HTTP/1.1 push github main
```

- 推送前必须确认目标仓库与分支，并扫描密钥（见 `docs/GIT_AND_ENVIRONMENTS.md` 第 6 节）。
- 公开端**永不**推送 `.env`、本文件、证书、私钥、token。
- Gitea 的 `.env` 是已跟踪文件，`.gitignore` 对已跟踪文件无效；克隆 Gitea 后无需手工创建 `.env`。

## 4. 轮换

- 凭据泄露或人员变动时，按“Gitea → GitHub → Gitee”顺序轮换，并同步更新本仓库 `.env`。
- 轮换后本文件**不记录新值**，只确认位置与责任人。