# ADR-0018 三方通道统一模型：能力 / 供应商 / 用途绑定

- 状态：采纳（2026-11-04，已实现并验证）
- 决策人：产品 + 后端
- 关联：ADR-0005（第三方服务封装）、AGENTS §1（禁止静默兜底）、§13（枚举必须可选）、§10.5（告警分级）

## 背景

当前三方对接的做法不统一，同一类问题反复出现：

| 现象 | 证据 |
|---|---|
| 能力清单有两份且不一致 | `IntegrationCatalog` 8 项（含 export/exchange_rate，无 address）；`MarketIntegrationService.CAPABILITIES` 7 项（含 address，无 export/exchange_rate），注释还写着“与 integration_config 保持一致”。**2026-11-05 已合并**：市场侧改为从目录派生，见文末「清单合并」 |
| 通道列了但没实现 | `logistics` 候选含 `dhl`/`fedex`，`LogisticsTrackingService` 只有 `mock`/`kuaidi100`；`kuaidi100` 之前连 HTTP 都没发 |
| 市场能力配了没人读 | `market_integration` 的 `sms`/`storage`/`moderation`/`ai`/`address` 全仓无消费者 |
| 凭据双源 | 支付：`payment_provider_config` + `market_integration`；短信/物流：`app.sms.provider` 环境变量 + 后台系统设置 |
| 一个能力只能挑一个供应商 | 短信只有一个 `app.sms.provider`，验证码和告警只能共用同一个通道 |
| 告警没有出口 | `RiskAlertService` 只写 `risk_alert`，全仓无 IM/短信发送 |

## 决策

三方接入统一为三层，所有新能力必须落在这一套里：

### 1. 能力（capability）

唯一清单放在后端 `IntegrationCatalog`，包含：`capability`、`scope`（`global` / `market`）、供应商候选、字段 schema（key/label/secret/required/hint）。
`MarketIntegrationService` 的白名单**由目录派生**，不再手写第二份（删掉现有两份清单）。

新增能力：`email`（供应商 `126` / `qq` / `smtp`）、`alert_im`（供应商 `wecom` / `feishu` / `dingtalk`）。

### 2. 凭据（provider config）

凭据只存一处：`integration_config(capability, provider, config_enc, enabled)`，加密列已有实现。
环境变量一律降级为“迁移期兜底”，使用时打 WARN；不得再作为唯一来源。
`market_integration` 继续表示“**该市场是否启用该通道**”，不存凭据。

### 3. 用途绑定（usage routing）

新增表：

```sql
CREATE TABLE integration_route (
    capability  varchar(32) NOT NULL,
    usage_key   varchar(64) NOT NULL,
    provider    varchar(32) NOT NULL,
    updated_at  timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (capability, usage_key)
);
```

- `usage_key` 说明“哪个业务动作用哪个通道”，候选由后端下发：
  - `sms`：`OTP_LOGIN`、`ORDER_EVENT`、`ALERT_FATAL`
  - `email`：`OTP_LOGIN`、`ORDER_EVENT`
  - `alert_im`：`ALERT_FATAL`、`ALERT_RECOVERY`
- 语义：**同一能力可以有多个供应商并存**（阿里云 + 腾讯云 + 华为云都在 `integration_config` 里启用），
  由 `usage_key` 决定某个动作实际走谁。未绑定 = 该动作没有通道，必须**明确失败**并提示去配置，
  不允许回退到“第一个启用的供应商”。
- 发送端通过 `ChannelResolver.resolve(capability, usageKey)` 拿 `(provider, config)`，再构造 sender。
  这样“多短信通道 + 按用途勾选”不需要改业务代码。

### 4. 演示 / 生产

- `app.mode=demo`：所有能力强制走 `mock`（内置假数据或 wiremock），不发外网；
- 生产：`provider=mock` 视为不合法（启动自检失败），必须配真实供应商；
- 未配置：调用点返回稳定错误码（`SMS_NOT_CONFIGURED` / `EMAIL_NOT_CONFIGURED` / `ALERT_IM_NOT_CONFIGURED`），
  顾客侧文案模糊，管理端显示“去哪个页面配”。

## 供应商候选（首批）

| 能力 | 供应商 | 协议 | 备注 |
|---|---|---|---|
| `sms` | `aliyun` / `tencent` / `huawei` / `mock` | 官方 SDK | 已实现 aliyun/tencent，补 huawei 时按同一接口 |
| `email` | `126` / `qq` / `smtp` | SMTP + 授权码（JavaMail） | 126/qq 为预设 host/port/SSL，`smtp` 留给自建（后置） |
| `alert_im` | `wecom` / `feishu` / `dingtalk` | 机器人 webhook（HTTPS POST JSON） | 不引重 SDK；签名与加签按各家文档，单测用桩服务 |
| `logistics` | `kuaidi100` | 官方表单 POST | `dhl`/`fedex` 在实现落地前**不得出现在候选里** |

## 后果

- 收益：换供应商/加通道不改业务代码；admin 页只渲染目录；枚举不会再出现“填了不生效”。
- 成本：一次迁移 + `IntegrationCatalog` 重构 + 三个新 sender 家族 + admin 两个区块（能力配置、用途绑定）。
- 风险：`sms` 从“单通道”变“多通道 + 用途绑定”，需要一次数据回填：现有 `app.sms.provider` → `integration_config` + `integration_route(OTP_LOGIN)`。

## 验收

1. `make contract-check` 增加断言：catalog 里每个 `provider` 必须有对应实现分支（防 dhl/fedex 类问题）；
2. 每个 sender 家族至少一条桩服务测试（签名正确、报文解析、失败不假装成功）；
3. admin：未配置时页面显示“未配置 + 去配置入口”，不允许空下拉；
4. `risk_alert` 增加 `alerted/alerted_at`，FATAL 立即发、恢复发一条，未发出必须留痕。

## 实现记录（2026-11-04）

实现与上文决策的差异与补充（以代码为准）：

| 项 | 实际实现 | 原因 |
|---|---|---|
| 迁移 | `V8__channel_routing.sql` 建 `integration_route`、扩 `risk_alert`；`V9__multi_provider_capabilities.sql` 删 `integration_config_one_enabled` | V1 的部分唯一索引与“同一能力可存多家”直接冲突，而能力清单不应在 SQL 里再写一份 |
| 解析入口 | `IntegrationRouteService.find/require/missing`（非 `ChannelResolver`） | 命名与现有 `Integration*Service` 一致 |
| 用途清单 | 9 个：`sms` OTP_LOGIN/ORDER_EVENT/ALERT_FATAL（告警类必填收件人）、`email` 同三个、`wechat` ORDER_EVENT、`alert_im` ALERT_FATAL/ALERT_RECOVERY | 实现需要邮件告警与公众号事件通知 |
| 供应商候选 | `sms` 仅 `aliyun`/`tencent`（无 `huawei` 与 `mock`）；`email` 为 `126`/`qq`/`mock`；`wechat` 为 `wechat`/`mock` | 未实现的供应商不进候选（避免 dhl/fedex 类死配置）；演示占位只给已实现 mock 分支的能力 |
| 未配置错码 | `CHANNEL_ROUTE_NOT_CONFIGURED` / `INTEGRATION_PROVIDER_NOT_CONFIGURED` / `INTEGRATION_PROVIDER_DISABLED` / `CHANNEL_RECIPIENT_REQUIRED` / `CHANNEL_USAGE_UNSUPPORTED` / `CHANNEL_MOCK_FORBIDDEN` / `CHANNEL_EVENT_UNMAPPED` | 统一前缀便于前端按 code 映射文案（§9.5） |
| 生产与 mock | 非演示模式下显式绑定 `mock` 会报 `CHANNEL_MOCK_FORBIDDEN`（只有 `app.mode=demo` 才走 mock） | “mock 静默生效”是本仓库明令禁止的兜底形态 |
| 告警派发 | `AlertDispatchService`（`@Async` 线程池 `alertExecutor`）先 `alert_im` 再补 `sms`/`email`，失败汇总写回 `alert_error` | 告警不能阻塞主业务事务 |
| admin 接口 | `GET/PUT/DELETE /admin/v1/channel-routes`、`POST /admin/v1/channel-routes/test`、`POST /admin/v1/risk-alerts/{id}/recover` | 运营改完配置必须能当场发一条验证，告警处理完必须能收掉 |
| 测试 | `ChannelSenderConfigTest`、`ImWebhookChannelTest`（本地 HTTP 桩）、`SmtpEmailChannelTest`（GreenMail）、`ChannelRoutingTest`（13 个用例，真库） | 不靠外网也不靠真实邮箱凭据 |
| 演示模式拦截真实供应商 | 演示环境（`app.mode=demo`）里某用途若绑到真实供应商，发送时抛 `CHANNEL_PROVIDER_DEMO_FORBIDDEN`（409）；未绑定的用途仍按 mock 记账并发 WARN。实测发现：放行时 `POST /admin/v1/channel-routes/test` 会真的打到阿里云（假凭据拿到 404），接口变成 500 | 演示口径是「只走官方 mock、不联网」，既不能真发也不能假装成功；`sms` 能力同时补了 `mock`（演示占位）供应商，否则演示环境连「把短信绑到 mock」都做不到（实测绑定时 409） |
| 遗留（本批已收尾） | 后台设置里的 `sms_provider`/`sms_access_key`/`sms_secret_key`/`sms_sign_name` 已从 `SetupView`/`SettingsView`/`SystemSettingService`/`SetupService` 全部删除，并用 `V10__retire_sms_settings.sql` 清掉 `system_setting`/`market_setting` 的历史行；页面改为指向「通道路由」的说明 | 只删代码不删数据时，页面会把历史行原样回传，保存就被 `INVALID_SETTING_KEY` 拒绝（e2e 实测 400）；`adminSettings()` 同时改为只回显仍受支持的键 |
| 页面验证 | Playwright（Edge）：`web/e2e/admin-channel-routes.spec.ts` 桌面走完“配供应商 → 绑用途 → 测试发送”，手机尺寸 390×844 校验页面无横向溢出；与 `admin`/`markets`/`admin-pickers` 一起 5/5 通过 | 运营的真实路径必须在真浏览器里走通，不能只靠接口测试 |
| 验收脚本 | `node scripts/acceptance-check.mjs` 17/17；未新增“通道”专项断言 | 该脚本面向演示环境的能力就绪/资金/枚举口径，通道行为已由 `ChannelRoutingTest` 覆盖 |
| 遗留2（2026-11-05 已收尾） | 告警“超时未处理 → 升级”曾只有去重与恢复通知；现已实现站内信默认通道 + 三态状态机 + 分档升级 | 见 ADR-0019；升级策略不需要值班表——按严重度分档（FATAL 15 分钟 / HIGH 1 小时、最多 3 级）即可，值班表是“通知谁”的问题，不是“多久催一次”的问题 |

## 清单合并（2026-11-05 收尾）

本 ADR 背景里记的「能力清单有两份」已合并为一份，唯一来源是 `IntegrationCatalog`：

- `MarketIntegrationService.CAPABILITIES` = `IntegrationCatalog.capabilityCodes()`；`PROVIDERS` 由 `IntegrationCatalog.capabilities()` 派生（能力 → 服务商标识）。市场侧不再有任何静态清单。
- 合并前市场侧那两个「谁也没实现」的候选按 AGENTS §1 删掉：`address` 能力（`amap`/`tencent-map`/`google` 全仓无消费者）整条移除；`payment` 的 `stripe`/`paypal`、`storage` 的 `aliyun-oss`/`s3`、`moderation` 的 `mock`、`logistics` 的 `dhl`/`fedex` 不再可选——运营选了也永远不会生效。
- 反向补齐：`logistics` 目录里补了 `mock`（`LogisticsTrackingService` 确有 mock 分支，演示模式强制走它），否则市场级下拉没有可选项。
- 特例保留：`ai` 的服务商仍来自 `ai_provider` 表（后台 AI 供应商页维护），目录里只登记能力本身；`catalog()` 在派生结果上覆盖 `ai` 一项。
- 新增不变量测试：`MarketScopedConsolidationTest.unimplementedProviderIsRejected` 断言 `dhl`/`stripe` 这类未实现标识在启用时被拒绝（`不支持服务商`），避免清单再悄悄长出死配置。
