# 演示模式与生产模式的隔离（硬约束）

> 原则：**演示/开发可以用 mock 伪造报文；生产环境绝不允许**。
> 伪造的"成功"比失败更危险——它让运维以为一切正常。

## 1. 模式定义

| 模式 | 用途 | mock 适配器 |
|---|---|---|
| `dev` | 本地开发 | 允许 |
| `demo` | 公开演示站（只读） | 允许，且**强制** mock（即使后台配了真实 AK 也不发外网） |
| `prod` | 生产 | **禁止**，启动即失败 |

## 2. 生产启动校验（`ProductionSafetyGuard`）

`app.mode=prod` 时启动即校验，任一项不合规则**中止启动**并列出所有问题：

| 配置 | 生产要求 |
|---|---|
| `app.payment.provider` | 真实支付（wechat/alipay/stripe…），不得为 `mock` |
| `app.logistics.provider` | 真实物流（kuaidi100/kuaidiniao/aftership），不得为 `mock` |
| `app.sms.provider` | 真实短信（aliyun/tencent/yunpian），不得为 `mock` |
| `app.storage.provider` | 必须显式配置，不得留空 |

错误信息示例：

```
生产模式禁止使用 mock/未配置适配器，启动中止：
  - app.payment.provider=mock（生产禁止 mock，会伪造成功报文）
  - app.sms.provider=aliyun 不是已知的真实适配器（白名单：[...]）
```

## 3. 通知渠道隔离

| 环境 | 外部渠道（SMS/EMAIL/WECHAT/APP） | 结果 |
|---|---|---|
| dev/demo | `MockNotificationChannelSender` | 记录报文，状态 `SENT` |
| prod + 已配真实服务商 | 真实 sender（待接 SDK） | 真实投递 |
| prod + mock 或未配 | `UnconfiguredChannelSender` | **抛错 → 通知落 `FAILED` 并带原因** |

`IN_APP`（站内消息）任何模式都可用：它落库即送达，不涉及外部服务。

**为什么不用 mock 顶在 prod**：mock 会把状态写成 `SENT`，运营看到"已发送"却没人收到。
现在一定会失败并留下原因，运维一眼可见。

## 4. 接口级隔离

| 接口 | 隔离方式 |
|---|---|
| `/mock/**`（DemoController） | `@Profile({"dev","demo"})`，prod 下不注册 |
| `POST /api/v1/payments/mock/{id}/pay` | `@ConditionalOnExpression`，prod 下不注册；且服务层再校验 `provider=mock` |

## 5. 演示数据

`DemoDataSeeder` 在 `app.mode=prod` 时**直接抛错**（不是静默跳过），避免"prod 配置了 dev profile"这种
危险组合被忽略。

## 6. 验证（自动化）

| 测试 | 断言 |
|---|---|
| `ProductionSafetyGuardTest` | prod + mock → 启动失败；prod + 未知服务商 → 失败；demo/dev + mock → 通过 |
| `ChannelSenderModeTest` | 生产下 SMS sender 被调用 → 抛"拒绝伪造发送成功"；`IN_APP` 始终可用 |

## 7. 部署检查清单

```bash
# 生产部署前必须确认
app.mode=prod
app.payment.provider=<真实渠道>
app.logistics.provider=<真实物流>
app.sms.provider=<真实短信>
app.storage.provider=<真实存储>
```

任一项不满足，服务**起不来**——这是有意设计。
