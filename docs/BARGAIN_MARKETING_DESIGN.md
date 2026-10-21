# 砍价营销（假砍价）设计草案 v0

> 定性：**名义砍价、实质拉新**。属合法营销，但**不能做成抽奖或 0 元购**。
> 状态：草案，待确认参数后实施（OPEN_ITEMS D13）。

## 1. 规则（建议默认值）

| 参数 | 默认 | 约束 |
|---|---|---|
| `floorPrice`（最低成交价） | 必填 | ≥ 成本 × 1.05；不填不允许启用 |
| 最大优惠 | 原价 − floorPrice | 由 floorPrice 推导，不允许手填超出 |
| 最少助力人数 | 5 | 3..30 |
| 单次砍价额 | 固定或区间（如 5~20 元） | 总额 ≤ 最大优惠，最后一刀自动收敛到 floorPrice |
| 有效期 | 24 小时 | 12..72 |
| 达成后 | **必须支付 floorPrice** | 不是免费领 |

## 2. 数据模型

```
bargain_activity (id, sku_id, floor_price, max_cut, min_helpers, cut_min, cut_max,
                  expires_hours, status, starts_at, ends_at)
bargain_session  (id, activity_id, member_id, current_price, helpers, status,
                  expires_at, order_id)
bargain_help     (id, session_id, helper_member_id, cut_amount, created_at,
                  UNIQUE(session_id, helper_member_id))   -- 助力人去重
```

## 3. 并发与幂等

- 砍价用 `SELECT ... FOR UPDATE` 锁 session；每人只能助力一次（唯一索引兜底）；
- 最后一刀 `min(cut, current_price - floor_price)`，**不允许低于底价**；
- 达成写 `status=READY` 并生成锁定价格；下单时校验收货地址与活动价一致；
- 过期未达成：`status=EXPIRED`，无扣款（未支付）。

## 4. 合规红线（写进代码与页面）

| 禁止 | 原因 |
|---|---|
| 显示虚假原价（先涨后降） | 《价格法》《明码标价规定》 |
| 未达成也收钱 | 构成欺诈 |
| 必须关注公众号/下载 App 才能继续砍 | 诱导分享，违反平台规则 |
| 0 元或 1 分购 | 不正当竞争风险 |
| 不公示中奖/助力规则 | 消费者知情权 |

页面必须明示：活动价、最低成交价、所需人数、截止时间、未达成处理方式。

## 5. 待确认

1. 底价按成本 +5% 自动算，还是运营手填？
2. 是否需要"新客专享"（仅未下过单的会员可发起）？
3. 达成后未支付，是否释放库存并关闭活动？
