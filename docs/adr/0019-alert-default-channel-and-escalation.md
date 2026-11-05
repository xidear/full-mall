# ADR-0019 告警默认通道与超时升级

- 状态：采纳（2026-11-05，已实现并验证）
- 决策人：产品 + 后端
- 关联：AGENTS §10.5（错误分级、告警必须可升级/可恢复）、ADR-0018（三方通道统一模型）

## 背景

§10.5 要求「告警必须去重、可升级（超时未处理升级）、可恢复通知」，但实现只做到第一步：

| 现象 | 证据 |
|---|---|
| 没有出口就什么都收不到 | `AlertDispatchService` 只按 `integration_route` 找群机器人/短信/邮件；演示环境一个路由都没绑时 `risk_alert.alerted` 恒为 false，管理端只能靠人主动翻表 |
| 没有「已确认」这个状态 | 表里有 `acknowledged_at`，但没有任何读写路径：值班人看了、正在处理，系统无从得知 |
| 没有升级 | 没有 `escalation_level`、没有巡检任务；FATAL 喊一次之后就一直静着 |
| 管理端看不到 | `GET /admin/v1/risk-alerts` 有接口，但后台没有任何页面渲染它 |

## 决策

### 1. 站内信是默认通道（不需要配置、不可关闭）

- 每条 FATAL/HIGH 告警与每条恢复通知，先写一条 `notification`（`channel=IN_APP`、`recipient=STAFF/ALL`、`event_type=ALERT_FATAL|ALERT_RECOVERY`、`link=/alerts`），再尝试群机器人/短信/邮件。
- 因此 `alerted` 现在表示「至少有一条通道送到了」，其中站内信一定送到；外部通道的失败原因仍逐条写进 `alert_error`（`alert_im/ALERT_FATAL: 尚未绑定通道` 之类），**站内信到位不掩盖外部没人被叫醒**。
- 不新增 `in_app` 能力、不在 `integration_route` 里绑「站内信供应商」：站内信是系统内置能力，把它做成可绑定的三方通道只会多一个「没配就收不到」的坑（AGENTS §1）。

### 2. 三态状态机：待处理 → 处理中（确认）→ 已恢复

| 状态 | 判定 | 升级巡检 |
|---|---|---|
| 待处理 | `acknowledged_at IS NULL AND recovered_at IS NULL` | 参与升级 |
| 处理中 | `acknowledged_at IS NOT NULL AND recovered_at IS NULL` | 不参与 |
| 已恢复 | `recovered_at IS NOT NULL` | 不参与 |

- `POST /admin/v1/risk-alerts/{id}/ack`：记 `acknowledged_at/by`；已确认或已恢复再确认返回 409（`RISK_ALERT_ALREADY_ACKNOWLEDGED`/`_RECOVERED`），不静默成功。
- 确认与恢复是两件事：确认=我看到了、在处理；恢复=问题已解决（会发恢复通知）。

### 3. 超时升级：按严重度分档、有上限、可停止

- `AlertEscalationJob` 每分钟巡检一次（`app.scheduling.enabled` 控制，测试默认关闭）。
- 间隔：FATAL 15 分钟、HIGH 1 小时；每次升级把 `escalation_level` +1（同时更新 `escalated_at` 作为下一次的计时起点），**最多 3 级**。
- 每次升级都用同一套派发链路再推一轮，文案加 `[升级 L{n}]` 前缀，让收到的人知道「这是催办，不是新问题」。
- 并发安全：`UPDATE ... WHERE id=? AND escalation_level=? AND acknowledged_at IS NULL AND recovered_at IS NULL`，影响行数不为 1 视为并发修改并放弃本次升级（多副本同时巡检不会重复升级）。
- 为什么设置上限：继续无上限升级只会训练大家忽略告警；真正的收敛手段是人确认或标记恢复。

### 4. 管理端必须能看（`/alerts`）

- 表格列：级别、状态、升级级别、规则、对象、详情、已推送通道、创建时间、确认人、操作（确认处理/标记恢复）。
- 手机尺寸（≤640px）改卡片列表：宽表格的右侧固定列在 390px 下会吃掉半屏，操作按钮虽可点但看不到上下文。
- 状态切换遵循 AGENTS §13.1 的反面要求：确认/恢复是**不可逆的状态迁移**，不是二元开关，所以用带动作名的按钮而不是开关。

## 后果

- 演示环境（无任何通道路由）也能在「通知中心」看到全部 FATAL/HIGH 告警，并且 `alerted=true` 有明确含义。
- 值班人忘记处理时，告警会自己升级到 L1–L3 并重复提醒；但**永远不会自动恢复**——恢复必须是人做的判断。
- 站内信通道不经过 `ChannelSendService`：它没有外部依赖、没有凭据、不会因为配置错误而失败，这正是它能当「默认」的原因。

## 验证

- `AlertEscalationTest`（嵌入式 PG，4 个用例）：站内信默认送达并置 `alerted`、确认后不再进入升级候选、按严重度分档的间隔、升级推送 `[升级 L1]` 且到上限后停止。
- `ChannelRoutingTest`：未绑定群机器人时 `alerted=true` 且 `alertChannel` 含 `IN_APP`、`alertError` 仍写「尚未绑定通道」。
- API 实测（41280）：`POST /admin/v1/risk-alerts/{id}/ack` 200/重复 409/未登录 401；巡检把 20 分钟前的 FATAL 升到 L1 并写入 `channel=IN_APP` 的站内信；`recover` 后收到 `ALERT_RECOVERY`。
- Playwright（`web/e2e/alerts.spec.ts`，edge）：桌面确认+恢复、通知接口契约、390×844 卡片列表可确认处理。
