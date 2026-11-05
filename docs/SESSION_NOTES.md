# 会话交接记录

> 规则见 `AGENTS.md` §14：每批改动结束更新本文件，只保留最近 3 批。
> 写清“已做并验证 / 已做未验证 / 未做”，以及环境事实（端口、容器、库是否重建）。

## 2026-11-05 / 告警站内信默认通道 + 超时升级 + 清单合并

- 已做并验证：
  - **清单合并（ADR-0018 收尾）**：`MarketIntegrationService.CAPABILITIES`/`PROVIDERS` 改为从 `IntegrationCatalog` 派生，市场侧不再有静态清单；删掉从未实现的 `address` 能力与 `stripe`/`paypal`/`aliyun-oss`/`s3`/`dhl`/`fedex`/`moderation.mock` 候选，给 `logistics` 补 `mock`（演示分支真实存在）。`MarketScopedConsolidationTest` 8/8（含新增 `unimplementedProviderIsRejected`）；`IntegrationConfigServiceTest` 6/6。
  - **站内信默认通道（ADR-0019）**：`AlertDispatchService` 先写 `notification(channel=IN_APP, event_type=ALERT_FATAL|ALERT_RECOVERY, link=/alerts)` 再试群机器人/短信/邮件；`alerted` 表示「至少一条送到」，外部通道失败原因仍写进 `alert_error`。新增 `AlertEscalationTest`（嵌入式 PG）4/4。
  - **超时升级**：`V11__alert_escalation.sql` 加 `acknowledged_by`/`escalation_level`/`escalated_at` + 部分索引；`RiskAlertService.acknowledge/escalationCandidates/escalate`；`AlertEscalationJob`（每分钟，FATAL 15min / HIGH 1h，最多 3 级，条件更新防并发）；`POST /admin/v1/risk-alerts/{id}/ack`。
  - **管理端页面**：`/alerts`（`AlertsView.vue`），表格 10 列（升级级别紧跟状态）+ 窄屏（≤640px）卡片列表；菜单/路由/help 目录/HELP_INDEX 已同步，`check-help-coverage` 43 页通过。
  - **API 实测（41280，jar 已重建并重启容器）**：`ack` 200（acknowledgedBy=admin）→ 重复 `ack` 409 `RISK_ALERT_ALREADY_ACKNOWLEDGED` → 未登录 401 → 巡检把 20 分钟前的 FATAL 升到 L1 并写 `channel=IN_APP` 站内信（`[升级 L1]` 文案）→ 确认后 70s 不再升级 → `recover` 200 且收到 `ALERT_RECOVERY`。
  - **Playwright（edge）**：`alerts.spec.ts` 3/3（桌面确认+恢复、通知接口契约、390×844 卡片列表）；`markets.spec.ts` 3/3（新增用例断言能力下拉有「告警群机器人」「邮件」、无「地址」，物流只有快递100/演示占位、无 DHL）；`help-drawer.spec.ts` 1/1。
  - **门禁**：`check-api-alignment`（451 路由 / 230 调用 / 0 未匹配）、`check-fallbacks`（93 处）、`check-enum-inputs`（16 处）、`check-money-float`、`generate-permission-help --check`、admin `npm run build` 全通过。
- 已做未验证：手机尺寸只验 390×844（iPhone 12/13 档），未覆盖平板与横屏。
- 未做 / 已知缺口：升级只按严重度分档，没有值班表/班次（「该叫谁」仍是全体 STAFF 广播）；`risk_alert` 的 MEDIUM/LOW 不外发也不升级（按设计）。
- 环境事实：本地栈 `fullmall-pg`（postgres:17-alpine，密码 `change-me-local`，用户/库均 `fullmall`）、`fullmall-redis`、`fullmall-api`（`--network fullmall-dev`，`SPRING_PROFILES_ACTIVE=api,dev`、`APP_MODE=demo`、`SPRING_DATASOURCE_URL=jdbc:postgresql://fullmall-pg:5432/fullmall`）；admin `npx vite --port 41281`；E2E 需要 `E2E_DB_CONTAINER=fullmall-pg`（`alerts.spec.ts` 用 psql 造 FATAL 告警，演示模式不会自然产生）。Flyway 已到 **v11**。

## 2026-11-04 / 通道路由（ADR-0018）：多供应商 + 按用途绑定 + 告警派发

- 已做并验证：
  - **数据层**：`V8__channel_routing.sql`（`integration_route(capability, usage_key, provider, recipient)`；`risk_alert` 加 `alerted/alerted_at/alert_channel/alert_error/recovered_at`；severity CHECK 扩到 `FATAL/HIGH/MEDIUM/LOW`；回填仅在能力恰好只有一家启用供应商时生效）、`V9__multi_provider_capabilities.sql`（删 `integration_config_one_enabled`——V1 的部分唯一索引阻断了“短信同时有阿里云+腾讯云”，单供应商唯一性改由 `IntegrationConfigService` 保证）。
  - **通道内核**：`platform/channel/**`（`ChannelUsage` 9 个用途、`IntegrationRouteService`、`ChannelSendService`、`sender/` 下 `AliyunSmsChannel`/`TencentSmsChannel`/`SmtpEmailChannel`/`WechatTemplateChannel`/`ImWebhookChannel`）；旧 `notification/channel/**` 与 `NotificationChannelConfig` 已删除。
  - **告警派发**：`AlertDispatchService`（`@Async("alertExecutor")`，`dispatchNow` 供测试同步调用）+ `RiskAlertService.recover` + `POST /admin/v1/risk-alerts/{id}/recover`。
  - **接口**：`GET/PUT/DELETE /admin/v1/channel-routes`、`POST /admin/v1/channel-routes/test`。
  - **后台页面**：`IntegrationsView.vue` 新增“通道路由”矩阵（供应商下拉 + 收件人 + 列表内绑定开关 + 测试发送），供应商候选由后端下发，前端不写死。
  - **后端测试全绿**：`mvn test` 全量 **834 个用例**（docker `maven-temurin21-local`，每类独立 JVM）。首轮 5 处失败全是“通道改成真发送”后的断言过期，逐个修完并单类回归通过：`NotificationRetryTest`（事件类型要用真实存在的 `CHANNEL_TEST`）、`ChannelTestSendTest`、`IntegrationConfigServiceTest`（多供应商下 `resolve()` 必须拒绝猜、用例间要清表）、`EmailLoginAndCustomsTest`（邮箱验证码要先绑 `email/OTP_LOGIN`）。新增 `ChannelSenderConfigTest`(5)、`ImWebhookChannelTest`(6，本地 HTTP 桩：钉钉加签 / HTTP200+业务错误码 / 不可识别响应)、`SmtpEmailChannelTest`(3，GreenMail)、`ChannelRoutingTest`(13，嵌入式 PG + 真库，含告警派发到企微桩)。
  - **页面回归（Playwright，Edge，宿主机）**：新增 `web/e2e/admin-channel-routes.spec.ts`（桌面：配供应商 → 绑用途 → 测试发送；手机尺寸 390×844：无横向溢出 + 标题横排 + 宽表可横向滚动）。与 `admin.spec`、`markets.spec`、`admin-pickers.spec`、`channel-test.spec`、`distribution-settings.spec`、`setup-wizard.spec` 合计 **9/9 通过**（逐个单跑全绿；早期一次合并跑里 2 个失败是 `sms_provider` 死配置未清理、保存被 400 拒绝引起的连锁，清理后复跑通过）。
  - **演示模式串外网的真实缺陷**：`app.mode=demo` 下把「短信 / OTP_LOGIN」绑到阿里云后，`POST /admin/v1/channel-routes/test` 真的打到了阿里云（假凭据拿到 404），接口报 500 → `ChannelSendService` 增加演示拦截：绑定真实供应商一律 `CHANNEL_PROVIDER_DEMO_FORBIDDEN`(409)，未绑定用途仍走 mock + WARN；同时 `IntegrationCatalog` 给 `sms` 补 `mock`（演示占位）供应商，否则演示环境连「把短信绑到 mock」都会被拒（实测 409）。新增 `ChannelDemoModeTest`(4) 卡住这条路径，接口复测：绑 aliyun → 409 且无外网调用，绑 mock → 200 且 `provider=mock`。
  - **门户连不上开放接口的真实缺陷**：分端口跑 E2E 时供应商门户报 `Failed to fetch`（`web/e2e/supplier-settlements` 一直红）。根因是 `SupplierHmacFilter` 把浏览器的 CORS 预检（`OPTIONS`，按规范不带签名头）拦成 401，响应里没有 `Access-Control-Allow-Origin` → 预检失败 → 后续请求全被浏览器丢弃。修法：`shouldNotFilter` 放行 `CorsUtils.isPreFlightRequest`，交给 MVC 的 CORS 处理；补 `SupplierOpenSecurityTest.preflightIsAllowedWithoutSignature`（断言 2xx + Allow-Origin），实测预检返回 200 且带 `Access-Control-Allow-Origin: http://localhost:41283`，门户用例转绿。
  - **E2E 登录收敛到共享助手**：24 个 spec 各自手写「读验证码题目 → 填答案 → 点登录」，题目刷新时会提交旧答案（本轮 3 次被误判成产品缺陷）。改成 `web/e2e/env.ts` 的 `login(page)`：填完再点、点完看结果、失败整轮重试 3 次，仍失败抛错并给排查方向；同时把 24 个 spec 的重复块与 3 个本地包装函数、22 处失效的 `ADMIN_BASE` 导入一起收掉。
  - **手机尺寸修掉的真实缺陷**：`.admin-header` 没给 `.header-title` 写 flex，390px 宽时右侧 4 个控件把标题挤成竖排一字一行（整页无横向溢出，所以只断言“没溢出”漏掉了它）→ 补 `.header-title` flex + `@media (max-width: 640px)` 让标题与操作各占一行、隐藏副标题；供应商配置表单在小屏把 150px 标签列改成标签独占一行；`通道路由` 宽表加“可左右滑动”提示。e2e 断言同步加严：标题盒宽 ≥80px 且高 ≤60px、表格 `scrollWidth - clientWidth > 0`。
  - **验收脚本**：`node scripts/acceptance-check.mjs` **17/17 通过**（首跑“汇率换算”失败是启动瞬间 mock 汇率刷新的时间戳竞争，重跑稳定通过；另手工 `PUT /admin/v1/exchange-rates` + `convert` 实测 100 USD → 72000 分，接口本身正确）。
  - **契约与静态检查**：`make contract-check` 通过（后端 450 路由 / 前端 227 调用 / 未匹配 0；权限 15 条；兜底 93 处已登记；枚举 16 处已登记；金额浮点 7 文件）、`python3 scripts/check-help-coverage.py` 通过（42 页面）、`npm run build --workspace @fullmall/admin` 通过（含 `vue-tsc --noEmit`）。
  - **过程中修掉的真实缺陷**：① `JdbcTemplate` 传 `Instant` → PG 无法推断类型（`Can't infer the SQL type ... java.time.Instant`），改 `OffsetDateTime`；② 演示供应商（mock）没有可填字段，前端“至少填一项配置”的校验让它**永远保存不了**，改为按字段清单是否为空判断；③ 未绑定行里先选的供应商没被记住，开关打开时会绑到“第一家可用供应商”，与页面显示不一致 → 加 `pendingProviders`；④ `el-switch` 的 `role=switch` 落在 0×0 隐藏 input 上，e2e 点不到 → 改为点可见的 `.el-switch`。
  - **文档**：`docs/adr/0018-third-party-channel-routing.md`（采纳 + 实现记录）、`docs/adr/README.md`、`docs/DEPENDENCY_AND_DEPLOYMENT_MATRIX.md` §3.1c、`docs/requirements/TECHNOLOGY_DECISIONS_V2.md` §1.4b、`docs/fallback-allowlist.json`。
- 已做未验证：
  - 真实阿里云/腾讯云短信、126/QQ 真实邮箱、企微/飞书/钉钉真实机器人：无凭据。按用户口径（“演示模式只用官方 mock，生产再在 admin 配 key”）只验证了官方协议报文与失败路径（本地桩 + GreenMail）。
- 未做 / 已知缺口：
  - 短信死配置入口已回收：`SetupView`/`SettingsView` 的短信服务商与短信密钥输入框已删除（改成指向「通道路由」的说明），`SystemSettingService` 的 4 个 `sms_*` 键与 `SetupService` 的就绪项一并移除，`V10__retire_sms_settings.sql` 清掉 `system_setting`/`market_setting` 的历史行；`adminSettings()` 改为只回显仍受支持的键。
  - `MarketIntegrationService.CAPABILITIES`（市场级 7 项）未与全局 `IntegrationCatalog` 合并：它管 `market_integration`（某市场是否启用某通道），职责不同，本批明确不做。
  - 告警“升级（超时未处理 → 升级）”未实现，只做了去重 + 恢复通知。
- 环境事实：
  - 宿主机 macOS Darwin 27.0.0 arm64；docker 走 colima（`colima start --cpu 4 --memory 4`，context `colima`）；宿主机无 JDK/Maven，Java 构建与测试全部在 `maven-temurin21-local` 容器内。
  - **`docker pull` 走国内镜像可用**：`docker.m.daocloud.io/library/postgres:17-alpine`、`docker.m.daocloud.io/library/redis:7-alpine` 均拉取成功（拉完 `docker tag` 成短名）。此前“守护进程 pull 必失败”的结论只对直连 Docker Hub 成立。
  - 本地 E2E 栈（本次手工起；`scripts/dev-stack.sh` 用 `--network host`，在 macOS 上不通）：`docker network create fullmall-dev`；`fullmall-pg`（postgres:17-alpine，`-p 5432:5432`）、`fullmall-redis`（redis:7-alpine）、`fullmall-api`（`java -jar server/target/fullmall-server-0.1.0-SNAPSHOT.jar`，`-p 41280:41280`，`SPRING_PROFILES_ACTIVE=api,dev`、`APP_MODE=demo`、`APP_SECRET_KEY` 必填否则启动即失败、`SPRING_DATA_REDIS_HOST=fullmall-redis`）；admin 用宿主机 `npx vite --port 41281`。
  - e2e 收尾脚本默认找容器 `fullmall-postgres-1`（compose 名），换容器名时要传 `E2E_DB_CONTAINER=fullmall-pg`，否则只打印清理失败并以退出码 1 结束。
  - Maven 缓存 `/Volumes/256/xidear/works/.dsh-maven-repo`，settings `/Volumes/256/xidear/works/.dsh-maven-settings.xml`（国内镜像）。
  - 新增依赖 `spring-boot-starter-mail`（随镜像）与 `com.icegreen:greenmail:2.1.3`（test scope），已登记 `docs/DEPENDENCY_AND_DEPLOYMENT_MATRIX.md` §3.1c。

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
