# 业务可配置项

## 促销叠加

| 配置 | 取值 | 说明 |
|---|---|---|
| `app.promotion.stacking-mode` | `BEST_ONLY`（默认）/ `STACK` | BEST_ONLY 只取优惠最大一条；STACK 按优先级叠加，互斥组内只生效一条 |

## 秒杀闸门

| 配置 | 默认 | 说明 |
|---|---|---|
| `app.seckill.capacity` | 200 | 单渠道桶容量（突发上限） |
| `app.seckill.refill-per-second` | 100 | 每秒补充令牌数 |
| `app.seckill.max-concurrent` | 50 | 同时在处理的请求上限，超出返回 503 |

限流只对显式 `seckill=1` 的试算生效，普通浏览不受影响。

## 券与促销叠加（C2）

| 配置/字段 | 取值 | 说明 |
|---|---|---|
| `app.promotion.coupon.mode` | `EXCLUSIVE`（默认）/ `STACKABLE` | 全局默认 |
| `coupon_template.stackable` | `NULL` / `TRUE` / `FALSE` | 单券覆盖；NULL 跟随全局 |

- `EXCLUSIVE` = 券与促销**取更优者**（不是让券失效）；
- `STACKABLE` = 券在促销之后继续抵扣。

## 阶梯促销（C9）

| 配置 | 默认 | 说明 |
|---|---|---|
| `app.promotion.ladder-enabled` | `false` | 开启后结算会应用 `promotion_ladder` 表里的满赠/第二件半价 |

**保守口径**（可改）：已命中秒杀/买赠等"活动型优惠"时**跳过**阶梯，避免多层叠加失控。
数据维护：`promotion_ladder(kind=GIFT|SECOND_HALF, threshold_fen, gift_sku_id, gift_quantity, priority)`。
