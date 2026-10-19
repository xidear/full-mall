# 促销与推广设计（秒杀 / 买赠 / 拼团）

## 1. 目标与边界

- 秒杀：活动商品在活动期内按活动价销售，活动库存和每人限购由服务端原子校验。
- 买赠：满足购买数量后自动追加零价赠品行，赠品同样预占/扣减真实库存。
- 拼团：按团号发起/参团，拼团订单固定 1 件商品；按已支付人数判断成团，过期未成团自动退款。
- 价格统一使用最小货币单位（分）存储，活动价不引入浮点。
- 本模块不替代会员折扣、优惠券、积分；这些在 POS/结算链路分别计算。

## 2. 数据模型

| 表 | 作用 |
|---|---|
| `promotion` | 活动主表：类型、状态、时间窗、商品、活动价、赠品规则、活动库存、已售、限购、成团人数/时长、优先级 |
| `promotion_redemption` | 每个订单命中的促销和赠品数量，用于每人限购统计 |
| `group_buy` | 团号、拼团价快照、成团人数、发起人手机号、状态、截止时间 |
| `group_buy_member` | 参团成员、订单、角色（CREATOR/MEMBER）、支付时间、退款时间 |
| `order_line.promotion_id` / `line_type` | 订单行追溯：PROMOTION / GIFT / NORMAL |

## 3. 结算定价顺序

1. 读取购物车 SKU 与数量，按 SKU 合并。
2. 命中拼团时：只允许 1 个 SKU、数量 1，使用团创建时的拼团价快照。
3. 否则命中秒杀：使用活动价；活动售罄直接返回 `PROMOTION_SOLD_OUT`，不静默回退原价。
4. 买赠：按 `购买数量 / 赠送数量` 计算赠品数，并受 `每单最多赠` 和赠品活动库存约束。
5. 订单小计保持商品原价，`discount` 记录活动优惠；应付 = 原价小计 + 运费 - 活动优惠。
6. 赠品行以 0 元写入订单行，但仍参与履约分配和库存预占。

## 4. 并发与一致性

- 秒杀/买赠名额：`UPDATE promotion SET sold_quantity = sold_quantity + ? WHERE sold_quantity + ? <= stock_limit`，失败即回滚整单。
- 每人限购：对 `promotion + 手机号` 加 PostgreSQL 事务级 advisory lock，再统计 `promotion_redemption`，避免同手机号并发超限。
- 拼团人数：对团 ID 加事务级 advisory lock 后重新读取人数并插入成员，避免并发超员。
- 拼团订单与普通订单共用库存预占/FEFO/确认逻辑，库存错误时整单回滚，包括已记录的促销名额。
- 活动库存、订单、赠品、拼团成员在同一数据库事务内提交。
- 未支付订单超时：定时任务按 `created_at` 条件取消订单，释放库存预占和促销名额；取消与支付都使用条件状态更新，只有一方成功。
- 已取消订单再次支付返回 409；拼团未支付成员在取消时移除，释放团内位置。

## 5. 拼团生命周期

```text
FORMING --(已支付人数 >= 成团人数)--> SUCCESS
FORMING --(到期未成团)--> FAILED --(逐单 Mock/真实退款)--> 订单 REFUNDED
```

- 下单时只记成员，不记已支付。
- 支付回调调用 `PromotionService.orderPaid`，标记成员已付并尝试成团。
- 已失败/已过期团拒绝继续支付，支付事务回滚。
- 定时任务每分钟扫描过期团；失败后发布 `GroupBuyFailedEvent`。
- 退款监听器逐单退款并写 `refunded_at`；失败订单每分钟重试，日志保留订单与团号。

## 6. 数据权限

- 门店员工：`StoreAccessService` 校验 `staff_store` 绑定；STORE 角色未绑定任何门店时失败关闭（不是显示全部门店）。
- 门店列表：`/store/v1/my-stores` 只返回绑定门店；ADMIN/OPERATOR 返回全部。
- POS 与门店作业的 `storeId`、班次、订单、拣货任务、自提核验均校验门店归属。
- 供应商：`/open/v1` 由 HMAC 凭证确定供应商 ID，查询只返回自身订单/商品/库存。
- 客服：会话查询和操作校验分配客服；未分配会话只能先领取。
- 生产环境 `app.store.auth-required=true`、`app.support.auth-required=true`；开发环境可通过环境变量关闭以便调试。

## 7. API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/promotions` | 当前活动 |
| POST | `/api/v1/group-buys` | 发起拼团 |
| GET | `/api/v1/group-buys/{code}` | 团详情与成员进度（手机号脱敏） |
| POST | `/api/v1/checkout` | 支持 `groupBuyId`，自动应用秒杀/买赠 |
| GET/POST | `/admin/v1/promotions` | 后台活动列表/创建 |
| POST | `/admin/v1/promotions/{id}/status?status=ACTIVE|ENDED` | 启用/结束 |
| GET | `/admin/v1/promotions/group-buys` | 拼团记录 |
| GET | `/admin/v1/stores` | 全部门店（员工绑定） |
| GET | `/store/v1/my-stores` | 当前员工可访问门店 |

## 8. 验证记录

- 秒杀：马克杯 3990 → 活动价 2990，订单 `discount=1000`，订单行 `PROMOTION`。
- 买赠：鲜奶买 2 赠 1，订单生成 2 件付费行 + 1 件 0 元 `GIFT` 行。
- 拼团：2490/人，2 人成团；首人支付后 `FORMING`，第二人支付后 `SUCCESS`。
- 拼团失败：将团截止时间改为过去，定时任务标记 `FAILED`，已支付订单自动 `REFUNDED` 并写退款时间。
- 门店数据权限：STORE 账号只能访问绑定门店；未绑定失败关闭；ADMIN/OPERATOR 不受限。
- 强制鉴权实例：store 访问上海门店 200，访问北京门店商品/任务 403 `STORE_SCOPE_DENIED`；admin 返回两店。
- 客服商品链接下单：推广链接携带 agentId + 模式，支付后佣金按实付 5% 生成（2990 → 150 分）。
- 超时未支付：订单 CANCELLED，促销 sold 4 → 2，预占 RELEASED，支付返回 409。
- Playwright：10/10 通过；后端测试 38 个，36 通过，2 个 Testcontainers 用例在无 Docker socket 环境跳过。