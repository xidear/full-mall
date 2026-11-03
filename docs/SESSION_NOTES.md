# 会话交接记录

> 规则见 `AGENTS.md` §14：每批改动结束更新本文件，只保留最近 3 批。
> 写清“已做并验证 / 已做未验证 / 未做”，以及环境事实（端口、容器、库是否重建）。

## 2026-11-03 / Gitea 地址校正 + SSH 推送

- 已做并验证：
  - Gitea 实际部署：**QNAP NAS `192.168.1.3`** 的 Container Station，容器 `gitea-run`（`0.0.0.0:2223->2222`、`0.0.0.0:3000->3000`）。
    NAS 固件升级重启后容器需等 dockerd 起来（`docker ps` 首次数十秒无响应属正常，勿当成失败）。
  - **Gitea 只接受 HTTPS**：`http://192.168.1.3:3000` 返回 400 `Client sent an HTTP request to an HTTPS server`；公网 Web `https://home.binnarui.cn:33000` 实测 200。
  - SSH：新增 `~/.ssh/id_ed25519` 公钥到账号 `tangpan`（API `POST /api/v1/user/keys`，201），`ssh -T gitea` 输出 `Hi there, tangpan!`。
    `~/.ssh/config` 增加 `Host gitea` 别名（`192.168.1.3:2223`，原 config 已备份为 `config.bak.*`）。
  - 修正根因：仓库本地 `core.sshCommand` 写死 `/home/xidear/.ssh/id_ed25519_gitea_dsh`（另一台 Linux 机器路径），
    本机不存在 → 每次推送都回退，已改为 `ssh -F ~/.ssh/config`。
  - 文档校正：`AGENTS.md` §6、`docs/GIT_AND_ENVIRONMENTS.md`、`docs/GITEA_PRIVATE_CREDENTIALS.md`、`docs/PROJECT_MEMORY.md`、`README.md` 的
    Gitea 地址由 `company.binnarui.com:8148` 改为实际地址。
  - 推送验证：`git push gitea main` → `5c1a3b0..b0d0872`，`git ls-remote gitea` 头部一致。
- 已做未验证：
  - 公网 SSH：`home.binnarui.cn:32223`（iKuai 32222 → 192.168.1.3:2223）与 `:22223` 均实测认证成功；曾因错指 2222 而 refused，已修。
- 未做 / 已知缺口：
  - `spike/spring-boot-4`：**该 spike 成果早已 cherry-pick 进 main**（main 上 `f1aad567`→`7fddc24f`→`23e496a6` 合并，165 测试全绿）；
    两分支 `server/pom.xml` 除 `.env` 外无差异（Boot 4.1.1 / Modulith 2.1.1 / springdoc 3.1.1 / Testcontainers 2.0.5 一致）。
    分支只是没删：停在 10-19，落后 main 597 个提交，本地仅领先 1 个 `498fd1b0`（= main `7fddc24f` 的重复提交）。
    此前“远端已有不同提交”判断有误，已更正。**patch-id 逐条核验**：32 个提交在 main 上全部有一一对应（唯一 `9e3608cf` 与 main `62f0fa65` 仅差 `.env`）。
    2026-11-03 用户决定：**冻结该分支** —— 已打归档 tag `archive/spike-spring-boot-4`（指向 `498fd1b0`）并推到远端，
    `docs/SYNC_AND_TECH_STACK_DECISIONS.md` §6 已标注“勿再在其上开发”。分支本身保留不动。
  - 另在 Gitea 侧加了分支保护（Gitea 28.2.0，`POST /repos/tangpan/full-mall/branch_protections`，`enable_push=false`），
    读回确认规则存在；**但“能否拦住推送”未做实测**（用户叫停，避免冒写风险）。
    远端分支头为 `c9b9974b`（比本地 `498fd1b0` 新，说明有别的机器推过），本地若要推需先 pull。
    另在 Gitea 加了分支保护规则（`enable_push=false`，Gitea 28.2.0，仓库此前无任何保护规则），API 读回已确认生效；
    未做推送实测（对真分支做试探推送风险大于收益），远端分支头仍为 `c9b9974b`。
- 环境事实：
  - NAS SSH：`ssh admin@192.168.1.3`（22 端口）可登录；docker 需用全路径 `/share/CACHEDEV1_DATA/.qpkg/container-station/bin/docker`
    （登录 shell 的 PATH 里没有 docker）。
  - Gitea 密码登录 API 也走 HTTPS：`curl -k https://192.168.1.3:3000/api/v1/...`（自签证书需 `-k`）。

## 2026-11-03 / 快递100 真链路 + ARM 测试环境

- 已做并验证：
  - **快递100 真链路**：`LogisticsTrackingService` 之前只构造请求就抛 503（client 与 decoder 早就有但没接线）。
    现在按「后台配置 Customer/Key → 真发 HTTP → `Kuaidi100Response` 解析」，失败统一 `unavailable()`
    （内部 `risk_alert` + 顾客通用文案）；cookie 环境变量只在后台未填时兜底并打 WARN。
  - 测试：`mvn -Dtest=LogisticsTrackingServiceTest,Kuaidi100ClientTest,Kuaidi100QueryTest,Kuaidi100ResponseTest test`
    → **12 tests / 0 failures / 0 errors，BUILD SUCCESS**（新增用例用本地桩服务验证签名、解析、密钥不外泄）。
  - ARM 主机测试可用：`server/pom.xml` 增加 `os.arch=aarch64` 自动激活的 profile，
    补 `embedded-postgres-binaries-linux-arm64v8` / `-darwin-arm64v8`（14.22.0）。
- 已做未验证：
  - 全量后端套件（本批只跑了物流相关 12 项，全量在跑，结果见下一条交接）
  - 演示模式仍走内置轨迹、不发外网；尚未给 `deploy/wiremock` 加快递100 mapping（E2E 需要时再加）
- 未做 / 已知缺口：
  - 快递100 凭据仍在 `system_setting`（后台「物流 Key/Customer」），尚未迁到 `integration_config` 加密存储；
    与「所有三方凭据统一走集成目录」的目标还差一次迁移
  - 告警通道（企微/飞书/钉钉）多通道、短信按用途勾选、邮件 126/QQ —— 三项未开始
- 环境事实：
  - 本机 aarch64：官方 `maven:3.9-eclipse-temurin-21` 是 amd64 镜像，已用 `alpine`/`debian` 自建同名镜像；
    **镜像里必须有 uid 501 的 passwd 条目**，否则 `initdb` 报 `could not look up effective user ID`
  - docker CLI 默认 context 是 `colima` → 指向 macOS 路径不可达，必须 `docker context use default`；
    daemon 侧 pull 的 DNS 坏（`[::1]:53 refused`），拉镜像会失败，只能由容器内下载后 commit
  - `docker run -v /tmp/...` 的路径由**宿主机**解析，不是当前容器；m2 缓存要用宿主机可见路径
    （`/Volumes/256/xidear/works/.dsh-maven-repo`），否则容器内 uid 501 写不进 root 属主的目录
  - 依赖下载慢：Maven 用阿里云镜像（`-s` 指向 settings.xml），JDK/Maven 二进制用 `mirrors.huaweicloud.com`
## 2026-10-31 / 自动汇率 + 退款汇率快照 + 金额去浮点

- 已做并验证：
  - 自动汇率：`exchange_rate` 集成能力（mock / Frankfurter，无 API Key）；只有 demo 未配置才用 mock，
    生产未配置不自动拉取；失败保留上次/手工汇率；`GET /provider`、`POST /refresh`
  - 退款汇率：V7 `order_line_refund.fx_*`；退款仍原币种，允许退款时汇率与下单不同
  - 金额去浮点：POS 百分比券改 `Money.mulDiv`，砍价 1.05 改整数比例，
    积分最小单位改 BigDecimal；前端 `toMinor` 改字符串/整数解析
  - 测试：ExchangeRateFetch/Mock、Refund、PosCalculator、BargainPricing 均通过
- 已做并验证（回归）：
  - 后端 **812 tests / 0 failures / 0 errors**（clean test）
  - Playwright **62 passed / 0 failed / 3 skipped**；验收 17/17
- 未做 / 已知缺口：
  - 跨币种退款/分账明确不做；只做直接汇率（无多跳）
  - 自动汇率偏差告警未做
- 环境事实：
  - migration 到 V7；`APP_FX_AUTO_INTERVAL_MS` 默认 3600000
  - 运行环境 exchange_rate 已启用 **frankfurter**（`api.frankfurter.dev/v1`，旧 `.app` 域名 301），
    mock 置为 disabled；mock 只在 `app.mode=demo` 且未配置时兜底，dev/prod 未配置不自动拉取
  - 坑：`server/target` 曾有 278 个 root 属主 class（早前用 root 跑过 Maven），
    非 root 容器写不进去 → `error while writing ...class`。用 root 容器 `rm -rf server/target` 清掉。
    **Maven 容器一律带 `-u $(id -u):$(id -g)`**
  - 41281 Vite dev / 41280 API / embedding 41291

## 2026-10-31 / 订单汇率快照 + 积分按币种 + 测试债清零

- 已做并验证：
  - `V6__order_fx_snapshot`：订单写 `fx_rate/fx_base_currency/fx_base_total_amount/fx_rate_at`；
    无汇率不阻塞下单，写 `ORDER_FX_RATE_MISSING` 审计，后台订单详情显示“未快照”
  - 积分按币种：`PosCalculator.pointsEarned(payable,currency)` 与
    `MemberRewardService.accrue(...,currency)` 按最小单位换算（JPY 每 1 日元 1 分、KWD 每 1 第纳尔 1 分）
  - 测试债：7 个后端类（Export/Schedule/Maintenance/IntegrationConfig/AiMedia/AiProvider/MediaUpload）
    通过测试夹具启用 integration_config 后全部转绿；6 个 Playwright 历史用例（admin-pickers/audit×2/
    export×2/translations）按当前 UI 修复后通过
- 已做未验证：
  - 完整后端套件：**810 tests / 0 failures / 0 errors**（clean test）
  - Playwright 全量：**62 passed / 0 failed / 3 skipped**
- 未做 / 已知缺口：
  - 供应商结算多币种仍明确拒绝（未做自动换算）
  - 跨币种退款/分账、自动拉汇率未做
- 环境事实：
  - migration 到 V6；export 集成目录已修正为容器内 `/repo/server/data/exports`
  - 41281 Vite dev / embedding 41291 / API 41280

