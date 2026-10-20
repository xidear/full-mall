# 退款与优惠回退口径

> 本文件是**唯一事实来源**。任何改动都必须同时更新这里与对应的测试。

## 1. 整单退款（refundedAmount ≥ 订单实付）

| 对象 | 处理 | 实现 | 测试 |
|---|---|---|---|
| 佣金 | 全部回冲（`REVERSED`） | `CommissionLedgerService.reverseByOrder` | `RefundCommissionLinkTest` |
| 抵扣积分 | 原额退回 | `MemberPointsService.rollbackForOrder` | `MemberPointsServiceTest` |
| 赚取积分/成长值 | 按仍持有部分回退（不产生负数） | `MemberRewardService.rollback` | `MemberRewardRollbackTest` |
| 优惠券 | 未过期 → `UNUSED`；已过期 → `EXPIRED` | `CouponService.returnForOrder` | `CouponCheckoutTest` |

## 2. 部分退款（refundedAmount < 订单实付）

| 对象 | 处理 | 理由 |
|---|---|---|
| 佣金 | 不回冲，**延长冻结期**（按既有策略） | 部分退后仍可能有售后，先冻住 |
| 抵扣积分 | **不退回** | 积分已抵进本单金额，部分退再退积分等于多给优惠 |
| 赚取积分/成长值 | **默认不回退**；可切换为按比例回退 | 见下方"可切换策略" |
| 优惠券 | **保持 `USED`** | 券已在本单使用；部分退款退券会让同一张券被用两次（资损） |

**这条口径由 `CouponCheckoutTest#partialRefundKeepsCouponUsed` 钉住**，防止被误当成 bug 改掉。

## 3. 已知限制（明确记录，不是遗漏）

- 部分退款的**按行分摊模型**尚未实现（需要 `order_line` 维度的退款记录），因此无法回答"退了哪件商品、该退多少积分/佣金"。
- 实现分摊模型前，**不建议**对"已使用优惠券/积分的订单"做部分退款，运营应改为整单退再重下。

## 4. 变更流程

改退款口径时：
1. 先改本文件的表格；
2. 再改实现；
3. 加入或更新对应测试；
4. `make verify` 全绿后才推送。


## 5. 可切换策略（第 127 轮新增）

部分退款的积分/成长值处理已做成**配置开关**，默认保持现状：

```properties
app.refund.partial.points-mode=KEEP          # 默认：部分退款不回退积分/成长值
app.refund.partial.points-mode=PROPORTIONAL  # 按退款比例回退（向下取整，不多退，不产生负数）
```

- 比例 = `本次退款金额 / 订单实付金额`；**向下取整**，宁可少退也不多退；
- 幂等键含退款金额（`order:<id>:points-partial:<amount>`），同一笔退款重复调用不会重复扣；
- 仍受"余额不足时不产生负数"保护。

**为什么默认 KEEP**：部分退款是否回退属于业务口径，不是技术选择；默认保持既有行为，
避免在口径未定前悄悄改动用户资产。

佣金在部分退款下的处理**未做开关**，仍为"延长冻结期"（等业务确认后同上加开关）。

## 6. 优惠叠加与回退的优先级（当前口径，写死在代码里）

下单金额计算顺序（`OrderService.checkout`）：

```
促销（秒杀/买赠/拼团 或 满减/满折，二者互斥）
  → 会员等级折扣（基数为"小计 − 促销优惠"，运费不参与）
  → 优惠券（EXCLUSIVE 默认：必须优于当前促销优惠才可用；STACKABLE：叠加）
  → 积分抵扣（1 积分 = 1 分，最多抵到应付为 0）
```

退款回退顺序（`PaymentService.refund`）：

| 场景 | 行为 |
|---|---|
| 整单退款 | 佣金全部回冲 → 抵扣积分退回 → 赚取积分/成长值回退 → 券退回（未过期→UNUSED，已过期→EXPIRED） |
| 部分退款 | 佣金延长冻结；积分/成长值默认不回退（`app.refund.partial.points-mode=PROPORTIONAL` 可按比例回退）；券保持 `USED` |

**为什么券在部分退款时不退**：券已在本单使用，部分退款退券会让同一张券享受两次优惠（资损）。

### 已知未实现

- 券的"部分消耗"（拆单分摊到行）——当前券是一单一用；
- 佣金在部分退款下的按比例回冲（需业务口径）。

