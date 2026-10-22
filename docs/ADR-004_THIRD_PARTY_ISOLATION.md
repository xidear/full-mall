# ADR-004：第三方交互必须可剥离（端口-适配器）

> 业务方要求：即使自己写，也必须**非侵入式、模块化、可完全剥离**。适用于所有第三方通讯。

## 规则（强制）

1. **领域层只认识自有端口**（`NotificationChannelSender`、`PaymentConnector`、`ExportStorage`…），
   **不得出现任何第三方类型**（不得 import 阿里云/腾讯/微信的类）。
2. **适配器单独成包**：如 `notification.channel.tencent`、`payment.app.wechat`。
   删除该包 + pom 依赖 + 配置项 = 通道彻底消失，其它代码照常编译。
3. **配置驱动装配**：`@Value`/`@ConditionalOnProperty` 决定是否启用；**缺配置一律启动失败**（fail-fast），
   不允许"静默降级成 mock 或空实现"。
4. **失败语义在适配器边界明确**：第三方返回码非成功 → 抛错并带原始错误码/requestId；
   绝不返回"成功"。
5. **依赖版本固定**，并在 pom 注释里写明"该依赖仅被某适配器使用"。

## 已落地的适配器清单

| 第三方 | 适配器包 | 依赖 | 状态 |
|---|---|---|---|
| 阿里云 OSS | `platform.export` | `aliyun-sdk-oss` | 配置 fail-fast；AK 未联调 |
| 阿里云短信 | `notification.channel` | `dysmsapi20170525` | 同上 |
| 腾讯云短信 | `notification.channel.tencent` | `tencentcloud-sdk-java-sms:3.2.18` | 本轮新增；缺配置失败、channel=SMS（有测试）；未联调 |
| 微信支付 v3 | `payment.app` | `wechatpay-java` | 配置 fail-fast；API 未接 |
| 支付宝 | `payment.app` | `alipay-sdk-java` | 配置 fail-fast；API 未接 |
| 微信公众号 | `notification.channel` | `weixin-java-mp` | 配置契约已测 |
| 快递100 | `logistics.app` | 无（HTTP+签名） | 全链路 + stub 验证 |

## 反例（不允许）

```java
// 领域服务里直接 new 腾讯云客户端 —— 违反规则 1/2
class OrderService {
    private final SmsClient tencent = new SmsClient(...);
}
```

## 自检方式

```bash
# 领域包不得出现第三方 SDK 类型
grep -rn "com.tencentcloudapi\|com.aliyun\|com.github.binarywang" \
     server/src/main/java/com/fullmall/{order,payment,fulfillment,inventory} || echo "OK: 领域层干净"
```

## 结论

端口-适配器不是"多写一层"，而是**让第三方可替换、可删除、可离线测试**。
腾讯短信这次就是反例修复：先按官方 SDK 引入（3.2.18），适配器独立成包，业务完全不知情。
