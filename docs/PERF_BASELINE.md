# 本地性能基线（非生产）

> 环境：本机容器/devenv，API 单实例（默认 JVM 参数），PostgreSQL 本机。
> 用途：作为**回归对比基线**；生产容量需独立压测环境（H13 的生产部分仍待环境）。

## 结果（并发 20，每接口 100 次）

| 接口 | 错误 | P50 | P95 | 吞吐 |
|---|---|---|---|---|
| 商品列表 `/api/v1/products?size=20` | 0 | 24.1 ms | 45.3 ms | 678.7 req/s |
| 商品详情 `/api/v1/products/{id}` | 0 | 11.4 ms | 22.2 ms | 1433.5 req/s |
| 促销试算 `/api/v1/promotions/preview` | 0 | 20.8 ms | 89.9 ms | 558.6 req/s |

复跑命令：

```bash
python3 scripts/bench-local.py http://localhost:8080 20 100
```

## 已知未覆盖

- 未做写链路压测（下单/支付）——需要独立库与可接受的副作用；
- 未做长稳（30 分钟以上）与内存增长观测；
- 未在容器内 API（而非宿主机 jar）下测量。
