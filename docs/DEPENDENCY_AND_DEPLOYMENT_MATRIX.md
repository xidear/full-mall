# 依赖与部署矩阵（含 HEIC 转码）

> 目的：回答一个具体问题——**客户在一台干净机器上部署，到底要不要额外装东西？**
> 结论按"依赖装在哪一层"给，每一行都注明：谁来装、离线怎么办、缺了会怎样（明确报错 / 静默降级 / 功能不可用）。
>
> 本文只记录**已经在本仓库里落地**的事实；计划中但没实现的会明确标"未实施"。

## 0. 先回答 HEIC（以及为什么不需要装东西）

`heic2any`（MIT，内含 libheif 的 wasm 构建）是**前端 npm 依赖**，在 `npm install` + `npm run build` 时打进
`apps/admin/dist` 与 `apps/storefront/.output`（admin 为单独懒加载 chunk，约 1.35MB，只有真的上传 HEIC 时才下载）。

| 问题 | 答案 |
|---|---|
| 服务器要装 libheif / ffmpeg / ImageMagick 吗？ | **不要**。后端没有 HEIC 解码代码，收到未转码的 HEIC 直接返回 `ASSET_HEIC_NOT_CONVERTED` |
| Docker 镜像里要加什么吗？ | **不用**。`server/Dockerfile` 只装 JRE；`web/Dockerfile.vite` 是前端构建阶段，产物是静态文件 |
| 客户新机器部署要做额外步骤吗？ | **不需要**，前提是部署的是已构建产物（发布镜像 / 已构建 dist）。npm 依赖只在**构建机**上需要 |
| 客户从源码构建呢？ | 构建机需要能访问 npm registry（或用内网镜像 / 离线缓存）；`heic2any` 就在这一步获取 |
| 浏览器不支持 WebAssembly 怎么办？ | 上传前就提示"导出为 JPEG 或换新版 Chrome/Edge/Safari"，不落库、不静默失败（见 `web/apps/admin/src/upload.ts`、`web/apps/storefront/utils/upload.ts`） |
| 会不会静默降级（偷偷存成打不开的文件）？ | 不会。服务端白名单不含 HEIC，前端转码失败会抛出可读错误 |

一句话：**HEIC 是"浏览器能力 + 构建期依赖"，不是运行期运维依赖。**

## 1. 四层模型

| 层 | 谁负责装 | 客户新机器要不要动 | 典型内容 |
|---|---|---|---|
| A. 随镜像（应用自身） | 镜像构建时 | 不要 | JDK 21 运行时、Spring/Tika 等 jar、前端静态产物（含 heic2any）、storefront 的 Node 运行时 |
| B. 运行期中间件 | `init.sh` 拉基础镜像 + `compose.yaml` | 不要手工装，但要给足磁盘/端口/密码 | Postgres、Valkey、RabbitMQ、SeaweedFS、OpenSearch（可选）、Mailpit（dev）、WireMock（dev）、Ollama（可选 AI） |
| C. 构建期依赖 | 构建机 | 走发布镜像时**不要**；从源码构建时**要外网或离线包** | Maven Central、npm registry、Docker Hub、GitHub（acme.sh） |
| D. 外部服务 / 浏览器能力 | 客户自己配/客户用户的浏览器 | 不装软件，但要配置或告知 | 支付、短信、物流、AI 供应商、OSS/CDN；浏览器的 WebAssembly、H.264 播放 |

## 2. 运行期（A + B）逐项

| 依赖 | 层 | 落地位置 | 少了会怎样 | 离线/内网注意 |
|---|---|---|---|---|
| Java 21（JRE） | A | `server/Dockerfile`（`eclipse-temurin:21-jre-alpine`） | 镜像起不来 | 镜像里已含，无需宿主 JDK |
| Apache Tika（含上传类型嗅探） | A | `server/pom.xml` → fat jar | 上传校验无法判型 | 已在 jar 内，无外部服务 |
| 前端静态产物（含 heic2any） | A | `web/Dockerfile.vite` → nginx 静态目录 | 后台打不开 / 无法传图 | 构建期依赖 npm，运行期零依赖 |
| storefront（Nuxt SSR） | A | `web/apps/storefront/Dockerfile`（`node:22-alpine`） | 商城前台 502 | Node 在镜像内；需 `NUXT_API_INTERNAL_BASE` 指向 api |
| PostgreSQL 17 | B | `compose.yaml` service `postgres` | 全站不可用（启动即失败，不静默） | 需预拉 `postgres:17-alpine` |
| Valkey 8（Redis 协议） | B | service `valkey` | 依赖缓存的接口报错 | 预拉 `valkey/valkey:8-alpine` |
| RabbitMQ 4 | B | service `rabbitmq` | 异步任务/事件不工作 | 预拉 `rabbitmq:4-management-alpine` |
| SeaweedFS（S3 兼容） | B | service `seaweedfs` | `STORAGE_PROVIDER=local` 时上传失败（明确报错） | 预拉 `chrislusf/seaweedfs:3.80`；也可换七牛/COS/OSS |
| OpenSearch 2 | B（可选） | service `opensearch` | 当前代码未使用，只作容量预案（`SemanticIndexService` 注释：>10 万条再切） | 可整组不启动 |
| Mailpit | B（dev） | service `mailpit` | 本地收不到邮件，不影响业务 | 生产不启用 |
| WireMock | B（dev） | service `wiremock` | AI/官方接口 mock 不可用，AI 生成链路失败 | 生产配真实供应商 |
| Ollama | B（可选 AI） | service `ollama`（profile `ai`，镜像固定 `0.40.13`） | 本地模型能力不可用；文本/语义检索功能报"供应商不可用" | **模型权重不在镜像里**：`bash scripts/start-local-ai.sh` 联网拉取；离线用 `--offline` 校验卷里已有模型，缺模型直接失败并给出命令 |
| acme.sh | B（ops） | `deploy/ops/Dockerfile` 构建期 `git clone`（地址可被 `--build-arg ACME_SH_SOURCE` 覆盖） | 证书自动续签不可用 | 内网构建：传入内部 git 镜像地址，或在有网机器上预构建 ops 镜像 |

## 3. 构建期（C）

| 依赖 | 谁用 | 离线策略 | 风险 |
|---|---|---|---|
| Maven Central | `server/Dockerfile`（`dependency:go-offline`） | 内网 Nexus 镜像，或 `maven-cache` 卷预热 | 首次构建需外网 |
| npm registry | `web/Dockerfile.vite`、`apps/storefront/Dockerfile` | 内网 npm 镜像 / 预构建镜像 | 已改 `npm ci` + COPY 根 `package-lock.json`，版本锁定；构建仍需 registry 可达（或本地缓存） |
| Docker Hub 基础镜像 | `init.sh` | 区域镜像加速 / 私有仓库（`init.sh` 已支持） | 首次拉取需外网 |
| GitHub（acme.sh） | `deploy/ops/Dockerfile` | 见 §5 | 内网直接失败 |
| Playwright + ffmpeg | 仅本地 E2E（`npx playwright install ffmpeg`） | 不装也行（当前 `video: 'off'`） | 与生产无关 |

## 3.1 本轮新增依赖（2026-10-28/29：富文本详情 + 视觉模型 + 素材检索）

| 依赖 | 层 | 谁装 | 缺了会怎样 | 离线/降级 | 验证方式 |
|---|---|---|---|---|---|
| `org.jsoup:jsoup:1.18.3`（MIT） | C 构建期 → 打进 server jar | Maven 构建时拉取 | 编译失败（不是运行期缺失） | 内网 Nexus 镜像；jar 已在本地 `~/.m2` | `mvn -o -DskipTests compile` |
| `@tiptap/vue-3`、`starter-kit`、`extension-image` 2.11.5（MIT） | C 构建期 → 打进 admin dist | `npm install` | 构建失败 | 内网 npm 镜像；`package-lock.json` 已锁版本 | `npm run build -w @fullmall/admin` |
| 视觉模型（默认 `moondream`，1.7GB） | D→B 可选外部服务 | **不随镜像**，由运营在「AI 供应商」表单登记 endpoint/model | **不影响主流程**：未配置时「AI 识别」按钮禁用，接口返回 409 `AI_VISION_UNAVAILABLE` | 可换成远端公共模型（DashScope/OpenAI 兼容）或客户自建 CPU 推理服务；本地 ollama 不默认启动 | `GET /admin/v1/ai/providers/vision` 看 `configured` |
| Ollama 容器（`--profile ai`，端口 41289） | B 运行期（可选） | 运维按需 `docker compose --profile ai up -d ollama` | 素材「AI 识别」不可用；素材库/生成/检索其余功能正常 | 不默认启动；无 GPU 也能跑（CPU 推理慢） | `docker exec fullmall-ollama-1 ollama list` |
| 模型下载源 | C（一次性，人工） | 运维执行 `ollama pull <model>` | 无模型时上述 409 | 国内网络建议走 ModelScope / hf-mirror 的 GGUF，再 `ollama create`；docker 镜像可走 dockerproxy | `ollama list` 里有该模型 |

## 4. 客户端 / 浏览器能力（D）

| 能力 | 用在哪 | 不满足时 |
|---|---|---|
| WebAssembly | HEIC→JPEG 自动转码 | 上传前给中文提示，请用户导出 JPEG；服务端也会拒绝裸 HEIC |
| H.264 视频播放 | 素材库/商品页/评价里的 mp4 播放 | Chrome/Edge/Firefox/Safari 均支持；极旧设备上视频不播，图片仍正常 |
| localStorage / cookie | 登录态、语言 | 无痕/禁用存储时需重新登录（不静默假装登录） |

## 5. 缺口清单与处理状态

| # | 缺口 | 状态 | 证据 / 说明 |
|---|---|---|---|
| 1 | ops 镜像构建依赖 GitHub（`git clone acme.sh`） | **已提供离线钩子** | `deploy/ops/Dockerfile` 新增 `ACME_SH_SOURCE` 构建参数，失败时打印"改用内部镜像或预构建镜像"的明确提示；air-gapped 构建未实测（本机 apk 源不可达，构建不出来） |
| 2 | 前端构建不锁依赖版本（`npm install`、无 lockfile） | **已修并验证** | 两个 Dockerfile 改 `npm ci` + COPY 根 `package-lock.json`；实测 `docker build` 通过，产物里含 `heic2any-*.js`（1.3MB） |
| 3 | 镜像 tag 用 `latest`（重建即漂移） | **已修** | `mailpit:v1.32.0`、`otel-lgtm:0.38.1`、`pgadmin4:9.18.0`、`ollama:0.40.13`；四个 tag 均经镜像站 `manifest inspect` 确认存在，`docker compose config -q` 通过 |
| 4 | ClamAV 文档写"采用"但代码/compose 都没有 | **已改文档** | 选型表标为"未实施"，并写明接入前不得对外宣称有病毒扫描 |
| 5 | 全链路上传体积不一致（应用 64MB / 边缘 nginx 32m / SPA 反代默认 1m） | **已修** | `deploy/edge/nginx.conf.template`、`deploy/nginx-spa.conf` 统一 72m；应用侧由 `UploadMultipartGuard` 启动自检 |
| 6 | Ollama 模型权重不在镜像 | **已提供脚本** | `scripts/start-local-ai.sh` 支持 `--offline`：只启动并校验模型在卷内，缺失即失败；联网模式照旧拉取 |
| 7 | `web/Dockerfile.vite` 的 `ARG APP_DIR` 不跨 `FROM` 继承 | **已修** | 原写法最终阶段 `${APP_DIR}` 展开为空 → `COPY /workspace//dist` 必然失败（此前未被发现）；已在最终阶段重新声明 `ARG APP_DIR`，实测镜像构建通过 |
| 8 | 构建上下文包含 269MB `node_modules` | **已修** | 新增 `web/.dockerignore` |
| 9 | 前端容器镜像以 `latest` 之外的 pin 策略管理基础镜像（node/nginx/maven） | **未做** | 目前只固定了大版本（`node:22-alpine`、`nginx:alpine`、`maven:3.9-eclipse-temurin-21`）；如需强复现可改 digest |
| 10 | 内网 apk 源不可达时 ops/前端镜像无法构建 | **已知环境限制** | 本次验证仅在本机（npm registry 可达、Alpine CDN 不可达）完成；内网交付请用预构建镜像 |

## 6. 新机器部署 checklist

```bash
# 1. 系统与 Docker（脚本会做，失败即停）
sudo bash init.sh --yes
# 2. 生成 .env / .env.ops、构建或拉取产物、初始化库、启动、写 site_domain
sudo bash setup.sh --yes
# 3. 起服务（core + app；需要 AI/观测再加 --with-tools/--all）
bash start.sh
```

需要人工确认的只有：磁盘（SeaweedFS/Postgres/Ollama 数据卷）、端口（41280–41289 及 80/443）、
数据库密码、域名与证书、以及**构建机是否能出网**（不能就预先准备好镜像与前端产物）。

## 7. 自检命令（部署后跑一遍）

```bash
curl -s localhost:41280/actuator/health            # 应用与库
docker compose ps                                  # 中间件状态
curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:41280/admin/v1/media   # 应为 401/400，不是 404
```

前端 HEIC 能力无法用 curl 验证，只能真机：用 iPhone 原图在 admin 素材库上传，
应成功并得到 `.jpg` 地址；若浏览器禁用 WebAssembly，应看到"请导出为 JPEG"的中文提示。
