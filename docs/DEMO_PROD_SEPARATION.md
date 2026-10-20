# 演示模式与生产模式的隔离（硬约束）

> 原则：**演示/开发可以用 mock 伪造报文；生产环境绝不允许**。
> 伪造的"成功"比失败更危险——它让运维以为一切正常。

## 1. 模式定义

| 模式 | 用途 | mock 适配器 |
|---|---|---|
| `dev` | 本地开发 | 允许 |
| `demo` | 公开演示站（只读） | 允许，且**强制** mock（即使后台配了真实 AK 也不发外网） |
| `prod` | 生产 | **禁止**，启动即失败 |

## 2. 生产就绪检查（**只报告，不阻断启动**）

> 之前的实现是"缺配置就拒绝启动"，这是错的：**服务商与 AK 由管理员在后台配置**，
> 启动即失败会导致根本进不了后台。策略已改为：

| 时机 | 行为 |
|---|---|
| 启动 | **永远成功**；缺失项以 WARN 列出（见下） |
| 后台 | `GET /admin/v1/system/readiness` 展示 payment/logistics/sms 是否已配真实服务商 |
| 运行时 | 真正调用该能力时才拒绝（**不伪造成功**），并写风控告警 + 日志 |

实测启动日志（prod + mock 服务商）：

```
Started FullMallApplication in 11.034 seconds
WARN 生产环境存在未配置的服务商（不影响启动，请在后台「系统设置」中补齐）：
  - payment = mock（未配置或仍为 mock，真实调用会被拒绝并记录告警…）
```

### 运行时拒绝示例

生产环境未配置真实支付时，`POST /api/v1/payments/mock/{id}/pay` 返回 **503**「支付暂不可用，请稍后再试」，
同时写入风控告警 `PAYMENT_PROVIDER_NOT_CONFIGURED`（客户看不到内部原因，维护人员能看到）。

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

## 7. 上线检查清单（不阻断启动，但必须补齐）

```bash
# 部署后进后台「系统设置」确认（或直接查就绪接口）
GET /admin/v1/system/readiness

payment   = wechat|alipay|stripe
logistics = kuaidi100|kuaidiniao|aftership
sms       = aliyun|tencent|yunpian
```

未补齐时：系统可正常登录后台、可浏览与配置，但**相关真实调用会被拒绝并告警**，不会伪造成功。

### 实测输出（本地带 PG 跑 prod 模式）

```
Caused by: java.lang.IllegalStateException: 生产模式禁止使用 mock/未配置适配器，启动中止：
  - app.payment.provider=mock（生产禁止 mock，会伪造成功报文）
  - app.logistics.provider=mock（生产禁止 mock，会伪造成功报文）
  - app.sms.provider=mock（生产禁止 mock，会伪造成功报文）
```

> 注意：验证时必须用**新打出的 jar**。我第一次用旧 jar 跑，应用居然起来了——
> 因为 `mvn test` 只编译不打包。这与第 134 轮踩的是同一个坑。
