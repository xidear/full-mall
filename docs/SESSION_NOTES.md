# 会话交接记录

> 规则见 `AGENTS.md` §14：每批改动结束更新本文件，只保留最近 3 批。
> 写清“已做并验证 / 已做未验证 / 未做”，以及环境事实（端口、容器、库是否重建）。

## 2026-11-06 / 公开端拓扑：GitHub 唯一源 + Gitee 自动镜像

- 已做并验证：
  - **GitHub 推送**：`publish/github` → `github.com:xidear/full-mall` `main`，快进（`58469eaf..2e3b41cd`，无强推）。
    两个公开仓库都已在 2026-10-18 创建（无需新建仓库）；GitHub 原 `main` 是本分支祖先，直接快进、无强推。
    公开端树里 `docs/GITEA_PRIVATE_CREDENTIALS.md` 与 `.env` 双双不存在（GitHub API 404；Gitee contents 返回 `[]`，需按目录列举复核）。
  - **Gitee 自动镜像**：新增 `.github/workflows/mirror-gitee.yml`，push 到 GitHub main / 打 tag 时触发。
    实测成功：Run 42230154087 全绿，Gitee `main` = `2e3b41cd` = GitHub `main`；
    Gitee 侧 `xidear/full-mall` 已存在（公开，默认分支 main）。
    注意：首次运行挂死期间，为保证 Gitee 不是空的，用本机快进直推过一次（`58469eaf..11119caf`）；镜像通道打通后不再手工推。
  - **认证改为 SSH 证书（当日晚些时候）**：GitHub 直连恢复、证书可用后，把临时私人令牌整套下线。
    - GitHub：`~/.ssh/id_ed25519_github`（`ssh -T git@github.com` 返回 `Hi xidear!`）。
    - Gitee 本机：新生成 `~/.ssh/id_ed25519_gitee`，公钥登记为 Gitee 账户公钥（id 6170661），`~/.ssh/config` 增加`Host gitee.com`；`git ls-remote git@gitee.com:xidear/full-mall.git`通过。
    - Gitee 镜像（Actions）：新生成 `~/.ssh/id_ed25519_gitee_ci`，登记为账户公钥（id 6170664），私钥写入 GitHub secret `GITEE_SSH_KEY`；`GITEE_TOKEN` secret 已删除（204）。
    - **实测 Gitee 部署公钥只读**：`POST /repos/{owner}/{repo}/keys`添加后推送被拒（`Could not read from remote repository`），因此只能用账户级密钥；已写进文档 §3.3 注释，避免后人重复踩。
    - 镜像工作流改 `git@gitee.com:` 推送，Run 42235875032 全绿，Gitee `main` = GitHub `main` = `f94a35cd`。
  - **文档**：`docs/GIT_AND_ENVIRONMENTS.md` §1/§2/§3.2–3.5/§4.1 与 `AGENTS.md` §6 改为新拓扑（GitHub 唯一源、Gitee 只读镜像、`publish/gitee` 降为备用）。
- 已做未验证：无。Gitee 同步已按 sha 核对，不是“看起来跑了”。
- 未做 / 已知缺口：
  - **临时令牌仍待你手动删除（API 删不掉）**：GitHub 经典 PAT（`ghp_` 前缀，scope 含 `repo`/`workflow` 等，2026-11-13 13:28 UTC 自动过期）无法通过 API 吊销——`DELETE /authorization`、`GET /authorizations` 对 UI 创建的 PAT 均返回 404，该端点只覆盖 OAuth App 授权。请到 https://github.com/settings/tokens 删除该条。
    Gitee 私人令牌已不被任何工作流使用，同样建议到 Gitee → 设置 → 私人令牌 删除。两者的值都不在仓库、remote URL、secret 里。
  - `Gitee /mirrors` API 全方法 405（未开放），将来若开通 Gitee 企业版可改为 Gitee 侧拉取并删除本工作流。
  - GitHub Actions 里仓库自带的 `CI` / `Secret Guard` 工作流与本批无关：`Secret Guard` 每次 `skipped`（既有行为，未查原因），`CI` 长时间 `in_progress`，本批未跟到结论。
- 环境事实：本机到 github.com **直连被封**（DNS 解析到 20.250.119.72，443 超时），可用代理是 NAS `192.168.1.3:7890`
  （文档里的 `192.168.1.2:7893` 已不可用，已改）；GitHub 推送实际走 SSH（全局 `url.git@github.com:.insteadof https://github.com/` + `~/.ssh/id_ed25519_github`）。
  远端：`gitea`（主）、`github`（公开源）；分支 `main`/`publish/github`/`publish/gitee`；里程碑 `milestone/2026-11-06-demo-ready`。
- 收尾核对（2026-11-06 22:11):`publish/github` = `c28e6a53`，GitHub `main` = `c28e6a5`，Gitee `main` = `c28e6a5`，Gitea `main` = `c91dc73`（公开端已剔除私有清单与 `.msg3.tmp`）。

## 2026-11-06 / 发布分支模型 + 里程碑标签

- 已做并验证：
  - **发布分支**：`publish/gitee`、`publish/github` 从 `main` 派生，逐个剔除 `docs/GITEA_PRIVATE_CREDENTIALS.md`；
    已用 `git ls-tree` 校验两条分支都不含该文件，`.env` 本就不入库。
  - **里程碑标签**：`milestone/2026-11-06-demo-ready`（附注标签，annotated）打在 `main` 上，已推 Gitea（`git ls-remote --tags` 可验证）。
  - **文档**：`docs/GIT_AND_ENVIRONMENTS.md` 升到 3.1——§1 仓库关系、§3.1 开发分支、§3.2 发布分支（含刷新流程）、
    §3.3 里程碑标签、§3.4 `.gitignore` 策略；`AGENTS.md` §6 同步（分支模型 + 里程碑规则）。
  - **纠错**：文档里“`.env` 已入库 / 是已跟踪文件 / 首次加入需 `git add -f .env`”三处说法与仓库事实相反
    （`git ls-files` 无 `.env`，`.gitignore` 第 28 行忽略），已全部改成“`cp .env.example .env`，不入库”。
- 已做未验证：未向 Gitee / GitHub 实际推送过——按用户要求本轮只准备分支；首次推送前仍需核对远端与凭据。
- 未做 / 已知缺口：发布分支目前与 `main` 同树，尚无“同步 main”的真实演练；`publish/gitee` 与 `publish/github` 初始内容相同，
  后续两边独立演进后需要各自维护。
- 环境事实：仓库 `/Users/xidear/Projects/full-mall`，remote 仅 `gitea`（`ssh://git@home.binnarui.cn:32223/tangpan/full-mall.git`）；
  分支 `main` / `publish/gitee` / `publish/github`；标签 `milestone/2026-11-06-demo-ready`、`archive/spike-spring-boot-4`；
  本轮不涉及数据库与容器。
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

