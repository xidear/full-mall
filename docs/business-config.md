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
