# 支付模式与 Provider 配置规范

> 日期：2026-10-19
> 触发：用户明确「微信/支付宝都要支持**商户模式**与**服务商模式**；服务商模式下我方店铺是**服务商下属商户（子商户）**，不是服务商本身，不是 b2b2c；业务可以是 B2C 或 B2B」
> 关联需求：PAY-001/002/003/004/005、CR-021（禁止手写 JSON）

## 1. 两种模式的区别（必须分清，写错会影响资金归属）

| | 直连商户模式（MERCHANT） | 服务商模式（SERVICE_PROVIDER） |
|---|---|---|
| 谁签约 | 我方每个主体各自签约 | 我方作为**服务商/ISV** 签约，旗下商户作为**子商户**进件 |
| 下单标识 | 自己的 `mchid` / `app_id` | 服务商 `sp_mchid` / `app_id` + **子商户 `sub_mchid` / `sub_appid`** |
| 签名证书 | 商户 API 证书/密钥 | **服务商**证书/密钥（代子商户调用） |
| 资金流向 | 进我方商户号 | **直接进子商户号**（我们不做平台分账） |
| 对账 | 按自己的商户号拉账单 | **按 `sp_mchid` + `sub_mchid` 拉子商户账单** |
| 退款 | 原商户号发起 | 带 `sub_mchid` 发起 |
| 回调 | 回调到我方 | 回调到**服务商**，必须按 `sub_mchid` 路由到对应店铺/订单 |

关键澄清（按用户口径）：服务商模式里 **我方平台是服务商，我们的店铺/商户是子商户**，钱进子商户，
因而不是 b2b2c 的平台代收代付，也**不做二级清算/分账**。业务形态可以是 B2C（自营店铺）或 B2B（企业客户下单）。

## 2. 数据模型

```
payment_provider_config
  id, provider(WECHAT|ALIPAY|UNIONPAY|AGGREGATOR), mode(MERCHANT|SERVICE_PROVIDER),
  display_name, market_code, currency,
  app_id, mch_id/sp_mch_id, api_version,
  -- 证书与密钥：只存引用与密文，禁止明文
  private_key_ref, platform_cert_ref, api_v3_key_cipher, cert_serial,
  callback_url, notify_url,
  status(ACTIVE|DISABLED), created_by, created_at, updated_at, rotated_at

payment_sub_merchant            -- 服务商模式下：我方店铺/主体 → 子商户号
  id, config_id, owner_type(STORE|LEGAL_ENTITY), owner_id,
  sub_mch_id, sub_app_id, sub_merchant_name,
  auth_state(PENDING|AUTHORIZED|REVOKED), auth_token_cipher, authorized_at
```

约束：

- `(provider, mode, market_code, owner_id)` 唯一，避免同一店铺挂两个有效配置；
- 服务商模式下 `sub_mch_id` **必填**，缺失时下单直接失败（不猜、不回退直连）；
- 直连模式下 `sub_*` 字段必须为空；
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