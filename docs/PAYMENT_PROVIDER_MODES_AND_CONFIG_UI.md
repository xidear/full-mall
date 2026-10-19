# 支付模式与 Provider 配置规范

> 日期：2026-10-19
> 触发：用户明确「微信/支付宝都要支持**商户模式**与**服务商模式**；服务商模式下我方店铺是**服务商下属商户（子商户）**，不是服务商本身，不是 b2b2c；业务可以是 B2C 或 B2B」
> 关联需求：PAY-001/002/003/004/005、CR-021（禁止手写 JSON）

## 1. 两种模式的区别（已按用户纠正：我方是子商户，不是服务商）

| | 直连商户模式（DIRECT） | 服务商模式（SUB_MERCHANT） |
|---|---|---|
| 上游是谁 | 微信支付 / 支付宝官方 | **第三方支付服务商（SP/ISV）**，它是服务商 |
| **我方角色** | 自己就是商户 | **我方是服务商下属的子商户** |
| 关键标识 | 我方 `mchid` / `app_id` | 上游 `sp_mchid` / `sp_app_id` + **我方 `sub_mch_id` / `sub_app_id`** |
| 签约与进件 | 我方直接与官方签约 | **由服务商代我方进件/报备**，我方提供营业执照、结算账户 |
| 调用凭证 | 我方商户 API 证书 | 我方子商户凭证（服务商下发/代申请），或服务商代调用 |
| 资金流向 | 进我方商户号 | **进我方子商户号**（钱仍是我们的，服务商不代收） |
| 对账 | 按我方商户号拉账单 | 按 `sp_mchid + sub_mch_id` 拉**我方子商户**账单；服务商可能提供汇总账单 |
| 回调 | 回调我方 | 回调到服务商平台或我方子商户地址，需按 `sub_mch_id` 路由到店铺/订单 |
| 资质门槛 | 需自行申请商户号 | **我方无需服务商资质**，但也受服务商约束（费率、结算周期、可用产品） |

> 我方做的是 B2C/B2B 商城，**不做 b2b2c 平台代收**；服务商模式的价值是省去自行进件与多主体管理，
> 钱进我方子商户账户，不存在平台二次清算或分账。

## 2. 数据模型

```
payment_provider_config
  id, provider(WECHAT|ALIPAY|UNIONPAY|AGGREGATOR), mode(DIRECT|SUB_MERCHANT),
  display_name, market_code, currency,
  -- DIRECT：我方 mch_id/app_id
  -- SUB_MERCHANT：上游 sp_mchid/sp_app_id + 我方 sub_mch_id/sub_app_id + 服务商名称
  sp_name, sp_mch_id, sp_app_id, mch_id, sub_mch_id, app_id, sub_app_id, api_version,
  -- 证书与密钥：只存引用与密文，禁止明文
  private_key_ref, platform_cert_ref, api_v3_key_cipher, cert_serial,
  callback_url, notify_url,
  status(ACTIVE|DISABLED), created_by, created_at, updated_at, rotated_at

payment_sub_merchant            -- 服务商模式下：我方主体/店铺 → **我方的子商户号**
  id, config_id, owner_type(LEGAL_ENTITY|STORE), owner_id,
  sub_mch_id, sub_app_id, merchant_name,      -- 都是我方的
  onboard_state(PENDING|ONBOARDED|REJECTED),  -- 服务商代进件状态
  auth_token_cipher, authorized_at            -- 支付宝 ISV 场景：我方给服务商应用的授权令牌
```

约束：

- `(provider, mode, market_code, owner_id)` 唯一，避免同一店铺挂两个有效配置；
- 服务商模式下 `sp_mch_id`（上游）与我方 `sub_mch_id` **都必填**，缺失时下单直接失败（不猜、不回退直连）；
- 直连模式下 `sp_*` 与 `sub_*` 字段必须为空；
- 所有敏感字段加密存储（AES-GCM/KMS），API 返回**掩码**，轮换写审计。

## 3. Provider 配置必须走 Web 图形界面（禁止手写 JSON）

硬规则（与项目「后台不允许手填 JSON/逗号字符串」一致）：

- 每个 provider + mode 有**独立的表单 schema**：字段类型（文本/下拉/文件上传/开关）、必填、正则、长度；
- **证书/密钥用文件上传**，不接受粘贴 PEM/JSON；
- 枚举字段（provider、mode、币种、市场）用**下拉**，不允许自由文本；
- 保存时做**强校验 + 连通性测试**（如微信证书序列号与私钥是否匹配），失败明确报错；
- 保存/轮换/禁用全部写审计（`PAYMENT_CONFIG_CHANGED`）；
- 生产模式缺配置：**启动失败或支付接口明确报错**，禁止回退 Mock（PAY-003）。

后台接口形态（示例，均为结构化 DTO，不是 Map/JSON 字符串）：

```
GET  /admin/v1/payment-configs?provider=&mode=
POST /admin/v1/payment-configs            // 结构化字段
POST /admin/v1/payment-configs/{id}/rotate   // 轮换证书/密钥
POST /admin/v1/payment-configs/{id}/test     // 连通性/签名自检
POST /admin/v1/payment-configs/{id}/sub-merchants   // 服务商模式子商户进件/授权
```

## 4. 适配器契约（PAY-001）

`PaymentConnector` 的方法签名必须携带"模式 + 子商户"上下文，不能只传金额：

```
create(PaymentContext ctx, Money amount, String outTradeNo)
  ctx = (provider, mode, configId, subMchId?, marketCode)
query/refund/cancel 同 ctx
webhook(request) -> VerifiedNotification(provider, mode, subMchId, outTradeNo, status)
reconcile(date, ctx) -> List<ChannelStatementRow>   // 按 sub_mch_id 拉账单
```

回调处理要点：

1. 先验签（服务商证书），再**按 `sub_mch_id` 反查店铺/配置**，匹配不到直接拒绝并告警；
2. 幂等 inbox（已有设计），重复/乱序回调不改账；
3. 退款必须带同一 `sub_mch_id`，否则会打回。

## 5. 对账与结算

- 微信：服务商模式下按 `sp_mchid + sub_mchid + bill_date` 下载交易/退款/资金账单；
- 支付宝：ISV 模式下按 `app_auth_token` 代子商户查账单（需在子商户授权范围内）；
- 差异分类：我方多记/少记/渠道手续费/退款滞后/跨日；
- 账单与 `ai_usage` 类同思路：**单价快照 + 幂等键**，月度对账出差异报告。

## 6. 实施顺序

1. **C2a 配置模型与后台表单**：`payment_provider_config` + `payment_sub_merchant` + 结构化 DTO + 掩码/轮换/审计 + 连通性测试。
2. **C1 适配器按 ctx 重构**：Mock 先支持两种模式；微信/支付宝直连实现。
3. **C2b 服务商模式**：子商户进件授权流程、按 `sub_mch_id` 路由回调与账单。
4. **C3 回调验签与幂等**、**C4 对账导入**。
5. 资质量子：服务商资质、子商户进件需业务提供；代码先以 Mock/沙箱跑通。