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
| Maven Central | `server/Dockerfile`（`dependency:go-offline`） | 内网 Nexus 镜像，或 `maven-cache` 卷预热；大陆直连慢时用 `mvn -s scripts/maven-settings-aliyun.xml`（阿里云公共仓库） | 首次构建需外网 |
| npm registry | `web/Dockerfile.vite`、`apps/storefront/Dockerfile` | 内网 npm 镜像 / 预构建镜像 | 已改 `npm ci` + COPY 根 `package-lock.json`，版本锁定；构建仍需 registry 可达（或本地缓存） |
| Docker Hub 基础镜像 | `init.sh` | 区域镜像加速 / 私有仓库（`init.sh` 已支持） | 首次拉取需外网 |
| GitHub（acme.sh） | `deploy/ops/Dockerfile` | 见 §5 | 内网直接失败 |
| Playwright + ffmpeg | 仅本地 E2E（`npx playwright install ffmpeg`） | 不装也行（当前 `video: 'off'`） | 与生产无关 |

## 3.1 本轮新增依赖（2026-10-28/29：富文本详情 + 视觉模型 + 素材检索）

| 依赖 | 层 | 谁装 | 缺了会怎样 | 离线/降级 | 验证方式 |
|---|---|---|---|---|---|
| `org.jsoup:jsoup:1.18.3`（MIT） | C 构建期 → 打进 server jar | Maven 构建时拉取 | 编译失败（不是运行期缺失） | 内网 Nexus 镜像；jar 已在本地 `~/.m2` | `mvn -o -DskipTests compile` |
| `scripts/generate-permission-help.mjs`（**零第三方依赖**，仅 Node 内置模块） | C 构建期/契约检查期 → 产出 api-client TS 与 `docs/PERMISSIONS.md` | `make contract-check` / `npm run generate:permissions` | 权限说明不生成：页面显示"暂无说明"，`contract-check` 失败（**不会静默通过**） | 无网络需求；仓库自带脚本 | `node scripts/generate-permission-help.mjs --check` |
| `@tiptap/vue-3`、`starter-kit`、`extension-image`、`extension-table`、`extension-placeholder`、`extension-character-count`、`extension-text-align` **3.32.1**（MIT；2026-10-30 由 2.11.5 升级并补齐扩展） | C 构建期 → 打进 admin dist | `npm install` | 构建失败 | 内网 npm 镜像；`package-lock.json` 已锁版本 | `npm run build -w @fullmall/admin` |

> tiptap **v2 → v3 是破坏性升级**：`editor.commands.setContent(content, emitUpdate: boolean)` 在 v3 变成
> `setContent(content, { emitUpdate })`。升级时 `RichTextEditor.vue` 用的还是 v2 签名，admin 构建直接 TS 报错
> （`Type 'false' has no properties in common with type 'SetContentOptions'`）。后续再升 tiptap 大版本时，
> 先跑 `npm run build -w @fullmall/admin` 再合入。
>
> 另一个 v3 陷阱：**StarterKit 已内置 `Link` 与 `Underline`**，再把 `@tiptap/extension-link` / `-underline`
> 单独注册会抛 `Duplicate extension names found`；这两个包因此不作为直依赖（只在 StarterKit 内部存在）。
| 自营对话模型端点 `https://company.binnarui.com:8500/v1`（`deepseek-v4.1-flash`，OpenAI 兼容、无 Key） | D 外部服务 | 运维在「AI 客服 → 模型供应商」登记 endpoint/model，无需 API Key | AI 问答返回 503 `AI_CHAT_NOT_CONFIGURED`；下单/后台其它功能不受影响 | 可换任意 `openai-chat` 兼容服务（自建 vLLM/Ollama、其它免费端点）；无 GPU 时优先远端 | `GET /admin/v1/ai/status` 看 `ready/chatModel`；再发一条 `/admin/v1/ai/ask` |
| 关键词检索（`APP_AI_ASK_RETRIEVAL=KEYWORD`） | A 应用内 | 无外部依赖 | 无 | 召回质量低于向量检索；没有 embedding 服务时用它 | `/admin/v1/ai/status` 的 `retrieval` |
| 本机 CPU embedding（`tools/embedding-service`，端口 41291，bge-small-zh-v1.5） | D→A 可选工具 | `cd tools/embedding-service && npm install --omit=dev && npm start`；模型首次从 hf-mirror 下到 `./models`（约 23MB） | 未启动/缺模型：SEMANTIC 模式 503 `AI_EMBEDDING_*`；不影响下单，可切回 KEYWORD | 离线把 `./models` 一起拷贝并设 `EMBEDDING_ALLOW_REMOTE=0`；也可换任意 `openai-embedding` 端点 | `GET :41291/health` 的 `loaded`；`/admin/v1/ai/semantic/reindex?kind=FAQ`；`/ai/ask` 返回 `retrieval=SEMANTIC` |
| 自动汇率 Frankfurter（`https://api.frankfurter.dev/v1`，ECB 数据，免费无 Key） | D 外部服务（可选） | 运营在「第三方服务 → 自动汇率」选择；不需要 API Key，可填内网代理地址 | 未配置：不自动拉取，手工汇率可用，应用照常启动；拉取失败：保留上次自动/手工汇率 | 可换内网代理；只有 `app.mode=demo` 未配置时才用 mock 兜底 | `GET /admin/v1/exchange-rates/provider` 的 `configured/lastSuccessAt/lastError`；`POST /refresh` |
| 视觉模型（默认 `moondream`，1.7GB） | D→B 可选外部服务 | **不随镜像**，由运营在「AI 供应商」表单登记 endpoint/model | **不影响主流程**：未配置时「AI 识别」按钮禁用，接口返回 409 `AI_VISION_UNAVAILABLE` | 可换成远端公共模型（DashScope/OpenAI 兼容）或客户自建 CPU 推理服务；本地 ollama 不默认启动 | `GET /admin/v1/ai/providers/vision` 看 `configured` |
| Ollama 容器（`--profile ai`，端口 41289） | B 运行期（可选） | 运维按需 `docker compose --profile ai up -d ollama` | 素材「AI 识别」不可用；素材库/生成/检索其余功能正常 | 不默认启动；无 GPU 也能跑（CPU 推理慢） | `docker exec fullmall-ollama-1 ollama list` |
| 模型下载源 | C（一次性，人工） | 运维执行 `ollama pull <model>` | 无模型时上述 409 | 国内网络建议走 ModelScope / hf-mirror 的 GGUF，再 `ollama create`；docker 镜像可走 dockerproxy | `ollama list` 里有该模型 |
| `@xenova/transformers` 2.17.2 + `sharp` 0.33.5（Apache-2.0 / MIT，ONNX 版 CLIP） | D（可选工具，`tools/clip-service`） | 开发者本地 `npm install --ignore-scripts` | **不影响主程序**：未配置时「图片内容」搜索按钮禁用，接口 409 `CLIP_UNAVAILABLE` | 模型文件从 hf-mirror 下到 `./models`（`node scripts/download-model.mjs`），运行时不联网；sharp 用 npm 预编译包，绕开 GitHub 下载与 node-gyp | `GET /admin/v1/assets/image-search/status` → `{available}` |
| CLIP 模型（clip-vit-base-patch32 量化版，约 150MB） | D | `tools/clip-service/scripts/download-model.mjs` | 同上（能力不可用，功能不受影响） | 仓库不入库模型；客户可改为远端多模态服务 | `curl /health`（41290） |

## 3.2 已决策待实施：行政区划数据集（ADR-0013，2026-10-29）

| 依赖 | 层 | 谁负责安装 | 离线怎么办 | 缺失时的表现 | 如何验证 |
|---|---|---|---|---|---|
| `province-city-china`（MIT，GB/T 2260 省市区） | C 构建期（**仅生成快照时需要**） | 维护者执行 `node scripts/build-region-snapshot.mjs`（待建），产物是仓库内 JSON/CSV | 快照已提交进仓库并随 Flyway 迁移灌库，交付机器**不需要 npm registry** | 未灌库时 `GET /api/v1/regions` 明确报错（不是空列表假装正常），结算页回落为手填并提示 | `psql -c "select count(*) from region"`；接口冒烟 `curl localhost:41280/api/v1/regions?country=CN&level=PROVINCE` |
| `world-countries`（ODbL-1.0）/ `i18n-iso-countries`（MIT） | C 构建期（同上，一次性） | 同上，国家层快照 | 同上 | 国家下拉为空时页面明确提示「国家数据未初始化」，禁止静默只留中国大陆 | `select count(*) from region where level='COUNTRY'` |
| 三方地址校验/联想（高德 / Google Address Validation / 菜鸟国际） | D 外部服务 | 客户在「系统设置 → 第三方服务」配置（`integration_config` capability=`address`，ADR-0005） | **不是必需**：未配置时静态字典 + 手填完全可用 | 未配置=功能可用；已配置但调用失败=前端回落手填，真实原因只进日志/管理端（对用户不报错） | 后台渠道自检；日志中 `provider=address` 事件；**不进 ProductionSafetyGuard**（有意差异，见 ADR-0013 §5） |

注：区划数据是**数据运维**，不是 Java 运行期依赖（Maven Central 检索中国行政区划 artifact 为 0 命中）。年度刷新 = 重跑生成脚本 + 一条新迁移；旧节点置 `active=FALSE`，禁止删除（历史订单必须能解释已存 code）。

| 依赖 | 层 | 谁负责安装 | 离线怎么办 | 缺失时的表现 | 如何验证 |
|---|---|---|---|---|---|
| libaddressinput **地址元数据**（`chromium-i18n.appspot.com/ssl-address/data/<CC>`） | C 构建/运维期（一次性抓取） | 维护者抓取到 `address_format` 快照；**运行期不得访问该域名**（实测本会话网络不可达，客户内网更不可达） | 元数据落库后即为自有数据；未抓取的国家**不允许开通** | `GET /api/v1/regions/formats?country=XX` 返回 404 `ADDRESS_FORMAT_MISSING`，后台开通市场时明确报错「该国无地址格式规则」，**禁止回退中国模板**（静默套模板 = 静默兜底） | 开通前自检脚本逐国打印 require/fmt/zip 并人工抽检 |
| `member_address.country_code`（新增，NOT NULL） | A 随迁移 | Flyway 迁移 + 结算/地址簿写入路径必填 | 无外部依赖 | 缺失则无法判断国家 ⇒ 运费/税/表单全部无从生效 | 迁移后 `select country_code, count(*) from member_address group by 1` |
| `market.active` 取代 `MARKET_VALUES` 常量 | A 随代码 | 改 `SystemSettingService`：默认市场必须 ∈ active 市场 | 无外部依赖 | 现状：加一个国家要改代码发版（`:92`、`:311-314`）；改后新增国家 = 配置 + 快照迁移 | 后台把 default_market 设为未开通国家应返回稳定错误码 |
| `scripts/fetch-address-metadata.py`（**已实施**） | C 构建/运维期（一次性，仅刷新区划时） | 维护者手工执行；**需要 HTTP 代理**才能访问 ADS 域名 | 快照已提交进仓库（V144，9 国/741 节点），交付机器不需要 python 与网络 | 无影响：运行期只读 `region`/`address_format` 表 | `python3 -m py_compile` + 迁移重放通过 |
| `python3`（快照脚本运行时） | C 手工步骤 | 维护者本机 | 不需要（产物已入库） | 只影响"年度刷新区划"这一动作 | 同上 |

注：批次 A/C **没有新增 Java/npm 运行时依赖**——国家码/时区校验用 JDK 自带 `Locale.getISOCountries()`/`ZoneId.of()`，区划数据走 Flyway 快照。

### 3.3 批次 D/E/F 依赖（2026-10-29）

| 依赖 | 层 | 谁负责安装 | 离线怎么办 | 缺失时的表现 | 如何验证 |
|---|---|---|---|---|---|
| `slugify`/区划抓取脚本依赖 | — | — | 无（脚本只用 `python3` 标准库） | 只影响"刷新区划"这一步 | `python3 -m py_compile scripts/fetch-address-metadata.py` |
| 税率/运费/价格表数据 | A 随迁移 | `V147`/`V148` + 演示种子 | 数据在迁移里，交付机器无需外部服务 | 无税率 → 不收税但**打 WARN + 报关单列出"没有税行"**；无运费规则 → 跨国下单报 `SHIPPING_RULE_MISSING` | `MultiMarketPricingTest`、`TaxAndCustomsTest` |
| 邮箱登录 | A 随代码 | `V149` + `MemberAuthService` | 无外部依赖（验证码在 dev 走 `debugCode`，生产需接邮件通道） | 未绑定邮箱的账号用邮箱登录会明确报 `EMAIL_NOT_REGISTERED` | `EmailLoginAndCustomsTest` |
| 邮件通道（生产） | D 外部服务 | 客户在「系统设置 → 第三方服务」配置 | 不是必需；未配置时邮箱 OTP 无法送达（**这是已知缺口，见下**） | 用户收不到验证码 | 后台渠道自检 |

> ✅ **已接入发信**（2026-10-29 收口）：`MemberAuthService.requestEmailOtp` 现在通过
> `NotificationService.sendToAddress(email, …, "EMAIL", …)` **真实投递**，并按投递记录判断结果：
> 渠道没配或发送失败 → **409/503 `EMAIL_SEND_FAILED`**，不假装已发送。
> dev 下 `app.sms.provider` 默认 mock（mock sender 落库为 SENT）；生产必须配真实 EMAIL 供应商，
> 否则会在每次请求邮箱验证码时报错——这是有意的"明确失败"。
>
> 仍未做：真实的邮件服务商 SDK（当前 EMAIL 渠道只有 mock 与"总是失败"两种实现）。

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
