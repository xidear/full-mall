# 分销重设计（按需求口径）

> 本文件是分销的唯一事实来源。此前把比例写死 5%、冻结写死 7 天、把"买家会不会拿佣金"当成问题，
> 都是错误做法；这里逐条纠正。

## 0. 身份模型（客服 ≠ 客户）

| 主体 | 登录方式 | 能做什么 |
|---|---|---|
| 员工（客服/店员） | 员工账号（`/admin`、`/store`、`/support`） | 在**内置客服聊天**中向客户发送商品卡片 → 建上下级关系 |
| 客户（会员） | 手机号 OTP（`/api/v1`） | 下单；若自己也是推广员，可获得**下级的**订单佣金 |

- 两类账号**严格隔离**，不存在"既是员工又是会员"的身份歧义（同一自然人可拥有两个账号，互不影响）。
- **买家永远不获得自己订单的佣金**：佣金只发给订单归属推广员及其上级。
- 客服不能通过企业微信外发链接（那是站外行为，系统无法归属）；只能在系统内聊天中发送商品。

## 1. 佣金比例（后台可配，保存即生效）

| 配置键 | 含义 | 默认 |
|---|---|---|
| `distribution.level1-mode` / `level1-value` | 一级佣金方式与数值 | `GROSS` / `5`（5%） |
| `distribution.level2-enabled` | 是否开启二级 | `true` |
| `distribution.level2-mode` / `level2-value` | 二级佣金方式与数值 | `GROSS` / `2` |
| `distribution.bind-mode` | 绑定方式：`ONCE`（一次）/`PERMANENT`（终身） | `PERMANENT` |
| `distribution.bind-timing` | 绑定时机：`REGISTER` / `CART` / `VISIT` | `CART` |

**计算方式**（`DistributionConfig.commission`）：

- `GROSS`：`订单金额 × 比例`（百分比，四舍五入到分）
- `FIXED`：固定金额（最小货币单位）
- `PROFIT`（毛利）：**未实现**——需要订单行成本快照，当前 `order_line` 无成本字段；不做假实现。

> 每次读配置都查设置表（不缓存）→ 后台保存后**立即生效**。

## 2. 二级分佣

- 一级：订单归属推广员（`referral_binding`）
- 二级：该推广员的上级（`promoter_link`），**只有存在上级时才发**
- 同一订单同一推广员同一等级唯一（`UNIQUE(order_id, promoter_staff_id, level)`）
- 佣金记录保存 `level` / `rate_mode` / `rate_value` / `promoter_type`，事后可追溯"按什么规则算的"

## 3. 冻结期（不是固定 7 天）

配置 `distribution.freeze-policy`：

| 取值 | 行为 |
|---|---|
| `AFTER_SALE_CLOSED`（默认） | 佣金 `frozen_until` 留空；订单完成后再过 `distribution.after-sale-window-days`（默认 7，对应"7 天无理由"）才可提现 |
| `FIXED_DAYS` | 固定天数（`after-sale-window-days`） |

整单退款仍直接 `REVERSED`（回冲）。

> 待实现：`AFTER_SALE_CLOSED` 的释放任务（按"订单完成时间 + 窗口"扫描 FROZEN 佣金）。
> 当前释放任务只处理 `frozen_until <= now()`；策略为 `AFTER_SALE_CLOSED` 时需要补这条扫描。

## 4. 提现

| 项 | 口径 |
|---|---|
| 最低提现额 | `distribution.min-withdrawal`（默认 10000 分 = 100 元），后台改完即生效 |
| 打款方式 | **由申请人在申请时选择**：`ALIPAY` / `BANK` / `WECHAT`（微信走专用转账接口） |
| 可选方式集合 | `distribution.payout-methods`（逗号分隔） |
| 审核 | **一级审核**：任何具备审核权限的员工均可审（走权限矩阵 `promotion.manage`，不写死 ADMIN） |

## 5. 与 CRMEB 的对照（概念层面，非代码照搬）

- 二级分佣、绑定方式/时机可配、佣金比例按等级可配 —— 与主流商城一致。
- 差异：本项目佣金走**不可变账本**（`commission_ledger`），每次状态变化留痕；CRMEB 以字段更新为主。

## 6. 待完成清单

| 项 | 状态 |
|---|---|
| 二级佣金落库 + 配置读取 | ✅ 本轮 |
| 后台设置页填写分销配置 | ⬜（键已开放，前端表单待加） |
| `AFTER_SALE_CLOSED` 释放扫描（订单完成 + 窗口） | ⬜ |
| 提现：申请人选择打款方式 + 一级审核权限化 + 最低额读配置 | ⬜ |
| `PROFIT`（毛利分佣） | ⬜ 阻塞：缺成本快照 |
| 客服聊天内发送商品建关系 | ⬜（已有客服会话与商品卡片能力，需接绑定规则） |
