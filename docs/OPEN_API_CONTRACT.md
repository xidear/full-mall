# 供应商开放 API 契约（`/open/v1`）

> 面向供应商系统对接。所有接口**强制 HMAC 签名**（`SupplierHmacFilter` 对 `/open/**` 全路径生效，无白名单例外）。

## 1. 鉴权

请求头：

| 头 | 必填 | 说明 |
|---|---|---|
| `X-Supplier-Key` | 是 | 平台下发的 access key |
| `X-Supplier-Timestamp` | 是 | Unix 秒；与服务端偏差超窗即拒绝（防重放） |
| `X-Supplier-Nonce` | 是 | 一次性随机串，服务端去重 |
| `X-Supplier-Signature` | 是 | 见下 |

签名串（换行分隔，**顺序固定**）：

```
HTTP方法
请求路径(含 query，不含 host)
timestamp
nonce
sha256Hex(请求体)
```

签名 = `HMAC-SHA256(secret, 签名串)` 的十六进制小写。

失败返回：`401` + `{code:"SUPPLIER_SIGNATURE_REQUIRED" | "SUPPLIER_SIGNATURE_INVALID" | "SUPPLIER_SIGNATURE_EXPIRED"}`。

## 2. 接口清单

| 方法 | 路径 | 用途 | 幂等 |
|---|---|---|---|
| GET | `/open/v1/ping` | 连通性与凭据自检 | 是 |
| GET | `/open/v1/todos` | 待办（待接单 + 待处理退货） | 是 |
| GET | `/open/v1/orders` | 待处理订单列表 | 是 |
| GET | `/open/v1/orders/{fulfillmentId}` | 订单详情 | 是 |
| POST | `/open/v1/orders/{id}/accept` | 接单 | 是（重复接单返回当前状态） |
| POST | `/open/v1/orders/{id}/ship` | 发货（需承运商 + 运单号） | 是 |
| POST | `/open/v1/orders/{id}/reject` | 拒单（需原因） | 是 |
| GET | `/open/v1/returns` | 退货列表 | 是 |
| POST | `/open/v1/returns/{id}/decision` | 同意/拒绝退货 | 是 |
| GET | `/open/v1/settlements` | 结算单列表 | 是 |
| GET | `/open/v1/settlements.csv` | 结算单 CSV | 是 |

## 3. 状态机（订单）

```
ALLOCATED --accept--> ACCEPTED --ship--> SHIPPED
     \--reject--> REJECTED
```

非法状态转移返回 `409` + 明确当前状态，**不做静默忽略**。

## 4. 错误码约定

| HTTP | code | 含义 |
|---|---|---|
| 400 | `SUPPLIER_*_INVALID` | 参数不合法 |
| 401 | `SUPPLIER_SIGNATURE_*` | 签名缺失/错误/过期 |
| 403 | `SUPPLIER_FORBIDDEN` | 越权访问他人订单 |
| 404 | `*_NOT_FOUND` | 资源不存在 |
| 409 | `*_STATE_*` | 状态不允许该操作 |
| 429 | `SUPPLIER_RATE_LIMITED` | 超过限流窗口 |

## 5. 限流

按供应商 + 路径窗口计数，超限 `429`。窗口与阈值由 `app.supplier.rate-limit.*` 配置。

## 6. Webhook（平台推送）

见 `docs/OPEN_ITEMS.md` G5 与 `WebhookDispatcher`：

- 请求头 `X-Webhook-Timestamp`、`X-Webhook-Signature`、`X-Webhook-Event`
- 签名 = `HMAC-SHA256(secret, timestamp + "." + body)`
- 2xx 视为成功；失败按 1m/5m/30m/2h/6h 重试，5 次后置 FAILED
- 对端**必须按 timestamp 校验时间窗**，否则会被重放

## 7. 对接自检清单

- [ ] 能正确构造签名串（含 query、body 的 sha256）
- [ ] 时钟偏差 < 5 分钟
- [ ] nonce 不复用
- [ ] 重复调用 accept/ship 不报错（幂等）
- [ ] 收到 429 会退避
