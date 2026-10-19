# POS 收银与组合支付设计

## 1. 范围

覆盖自营门店收银、O2O 自提核销、线下扫码收款、组合支付、积分抵扣、优惠券核销、现金收付、班次现金小结、门店财务对账和未来支付方式扩展。

不在本文范围：银行卡收单资质、终端硬件认证、完整总账/税务申报、离线银行卡支付（取决于收单机构能力）。

## 2. 角色

- 收银员：开/关班、收银、扫码/碰一碰、现金收付、锁屏。
- 店长：改价/折扣授权、退款授权、班次审核、现金差异处理。
- 财务：日结、支付对账、现金汇总、差异调查、导出。
- 顾客：扫码付款、会员码/积分、优惠券、自提核销。

## 3. 核心原则

1. **销售单与支付分离**：一个销售单可有多个 tender（支付方式）。
2. **金额整数分**：所有金额用最小货币单位；现金找零单独记录。
3. **幂等**：销售单、每笔 tender、退款、券/积分核销都有 idempotency key。
4. **账本优先**：积分、储值、优惠券使用 ledger，不直接改余额字段。
5. **现金可审计**：每次现金收入/支出/找零/交接都追加流水。
6. **离线可降级**：现金和已授权扫码可本地排队；碰一碰/银行卡按厂商能力决定。
7. **厂商隔离**：业务层只依赖 `PosPaymentProvider`，不接触厂商 DTO。

## 4. 组单与组合支付模型

```text
PosSale
  subtotal
  promotionDiscount
  couponDiscount
  memberDiscount
  pointsDiscount
  payable
  tenders[]
  changeAmount
  status
```

恒等式：

```text
payable = subtotal - promotionDiscount - couponDiscount - memberDiscount - pointsDiscount
sum(tender.amount) = payable
changeAmount = sum(cashTendered) - cashApplied
```

- **优惠**（券、活动、会员价、积分抵扣）减少应收。
- **Tender** 是实际收款方式：现金、微信、支付宝、银行卡、储值卡、其他。
- 积分抵扣既可建模为 discount，也可建模为 tender；本系统采用 **discount + points ledger**，便于财务区分收入和负债。
- 组合支付示例：应收 100 元 = 积分抵扣 10 元 + 优惠券 20 元 + 现金 30 元 + 微信 40 元。
- 支付失败：已成功的 tender 进入 `PENDING_REVERSAL` 或由收银员改选；不得静默丢弃。
- 现金：记录 `cashTendered`、`cashApplied`、`changeAmount`；钱箱操作单独流水。

## 5. 数据模型（建议）

- `pos_shift`：id、store_id、cashier_id、opened_at、closed_at、opening_float、expected_cash、actual_cash、variance、status、approved_by。
- `pos_sale`：id、sale_no、store_id、shift_id、member_id、currency、subtotal、discount_total、points_discount、payable、change_amount、status、idempotency_key、created_at。
- `pos_sale_line`：sale_id、sku_id、batch_id、quantity、unit_price、discount、tax、line_total。
- `pos_tender`：id、sale_id、type（CASH/WECHAT/ALIPAY/CARD/STORED_VALUE/OTHER）、amount、provider、provider_trade_no、status、idempotency_key、paid_at、reversal_status。
- `pos_cash_movement`：id、shift_id、type（OPENING_FLOAT/CASH_IN/CASH_OUT/SALE/REFUND/HANDOVER）、amount、reason、operator_id、created_at。
- `member` / `member_card`：会员、等级、储值余额（投影）。
- `member_card_ledger`：充值/消费/退款/赠送，不可变。
- `points_ledger`：earn/redeem/refund/expire，余额投影，唯一业务键防重复。
- `coupon_instance`：模板、实例、状态（ISSUED/LOCKED/USED/EXPIRED）、锁定时间、使用时间、sale_id。
- `coupon_redemption`：券实例、销售单、核销金额、操作人、回滚记录。
- `pos_refund`：原销售单、退款 tender 分配、原因、审批人、状态。
- `pos_reconciliation`：门店/日期/支付方式/应到账/实到账/差异。
- `pos_outbox`：与中心库存/订单/财务同步的可靠事件。

## 6. 状态机

销售单：

```text
DRAFT -> PAYING -> PAID -> COMPLETED
                  \-> PAYMENT_PENDING
DRAFT/PAID -> VOIDED
PAID/COMPLETED -> PARTIALLY_REFUNDED -> REFUNDED
```

Tender：

```text
CREATED -> PENDING -> SUCCESS
                   \-> FAILED
SUCCESS -> REVERSING -> REVERSED
```

班次：

```text
OPEN -> CLOSING -> CLOSED -> APPROVED
```

券/积分：锁定 → 核销 → 回滚/过期；所有操作带幂等键。

## 7. 收银流程

1. 收银员登录、开班、输入备用金。
2. 扫码/搜索商品；支持称重、改价（需权限）、退货、挂单/取单。
3. 识别会员：会员码/手机号；加载等级、积分、券、储值。
4. 应用优惠：券核销、积分抵扣、活动/会员价；显示优惠明细。
5. 选择收款：现金、扫码、碰一碰、银行卡、储值；支持组合支付。
6. 现金输入实收，系统计算找零；钱箱流水。
7. 支付成功：打印小票、更新门店库存/批次、写销售/积分/券/现金/事件记录。
8. 失败/取消：按 tender 逆序回滚；不可回滚的进入人工处理。
9. 关班：系统计算应有现金，收银员输入实盘，差异需说明/店长审批。
10. 财务日结：按 tender/门店/收银员汇总，和支付渠道对账，导出差异。

## 8. 积分与优惠券

### 积分

- 获得规则：实付金额/商品类目/活动；退款按比例扣回。
- 抵扣规则：固定兑换比例、最低/最高抵扣、不可与某些活动叠加。
- Ledger：`EARN/REDEEM/REFUND/EXPIRE`；余额是投影，不可直接 UPDATE 余额。
- 并发：唯一业务键 + 行锁/版本号，防重复抵扣。

### 优惠券

- 模板：满减、折扣、无门槛、商品/类目限定、门店限定、线上/线下渠道。
- 实例：领取、锁定（收银中）、核销、回滚、过期。
- 锁定有 TTL；支付失败/超时释放；核销与销售单强绑定。
- 退款：整单退/部分退按规则恢复券或按比例退；写回滚记录。
- 券成本进入财务口径，不能只从销售额里减掉不留痕。

## 9. 现金与财务

- 开班备用金、现金收入、现金支出、退款、交接、关班实盘。
- 应有现金 = 备用金 + 现金销售 - 现金退款 + 现金收入 - 现金支出。
- 差异 = 实盘 - 应有；差异需原因、店长审批、审计。
- 日结：销售额、优惠、退款、积分/券负债、tender mix、现金差异、支付渠道应到账/实到账。
- 对账：支付渠道账单 vs `pos_tender` vs 销售/退款；差异单独立项。
- 财务导出：CSV/Excel；后续对接总账/ERP。

## 10. 扫码、碰一碰与离线

- **B 扫 C（顾客出示付款码）**：收银端扫码 → 调 provider 付款接口 → 轮询/回调 → 成功。
- **C 扫 B（动态码）**：收银端创建支付意图 → 展示二维码 → webhook/轮询 → 成功。
- **支付宝碰一碰**：需要认证终端、NFC、支付宝 ISV/商户资质；后端只负责创建/查询/回调/对账，不假设纯软件可实现。
- **离线**：
  - 现金可离线；
  - 扫码/银行卡是否离线取决于收单机构，默认不允许“假装成功”；
  - 本地排队必须有金额上限、设备绑定、签名、补传、人工复核；
  - 任何离线成功都要有唯一凭证，联网后幂等上传。

## 11. Provider SPI

```java
public interface PosPaymentProvider {
    String provider();
    PosPaymentIntent create(PosPaymentRequest request);
    PosPaymentResult query(String providerTradeNo);
    PosPaymentResult cancel(String providerTradeNo, String reason);
    PosRefundResult refund(PosRefundRequest request);
}
```

适配器：微信、支付宝、银联、聚合收银厂商、现金（内部）、储值（内部）、Mock。业务层只依赖接口；厂商状态/错误码统一映射。

## 12. API（建议）

- `POST /store/v1/pos/shifts` 开班
- `POST /store/v1/pos/shifts/{id}/close` 关班/现金小结
- `POST /store/v1/pos/sales` 创建销售单（draft）
- `POST /store/v1/pos/sales/{id}/tenders` 添加支付方式
- `POST /store/v1/pos/sales/{id}/complete` 完成销售
- `POST /store/v1/pos/sales/{id}/refund` 退款
- `POST /store/v1/pos/sales/{id}/void` 作废
- `GET /store/v1/pos/sales/{id}` 详情
- `GET /store/v1/pos/providers` 当前门店可用支付方式
- `POST /store/v1/pos/members/lookup` 会员识别
- `POST /store/v1/pos/points/redeem` 积分抵扣
- `POST /store/v1/pos/coupons/verify` 券核销
- `GET /admin/v1/pos/reports/daily` 日结
- `GET /admin/v1/pos/reports/cash` 现金小结
- `GET /admin/v1/pos/reconciliation` 支付对账

所有写接口带 `Idempotency-Key`，收银员/门店/班次权限校验，操作审计。

## 13. 收银端 UI

- 收银员登录、锁屏/解锁、开/关班；
- 商品扫码/搜索、称重、改价授权、挂单/取单；
- 会员识别、积分/券/储值展示；
- 组合支付面板：应收、已收、待收、找零；现金/扫码/碰一碰/银行卡/储值；
- 支付结果明确，不允许“pending 就打印小票”；
- 现金钱箱/交接班；
- 离线队列和补传状态；
- 退款/作废需权限和原因；
- 小票打印/电子小票。

## 14. 分阶段落地

### P0：Mock 组合支付闭环
- 班次、销售单、tender、现金流水、积分/券 ledger 表；
- Mock 扫码/碰一碰 provider；
- 现金+扫码+积分+券组合支付；
- 关班现金小结、日结、退款；
- Playwright：开单→组合支付→完成→关班→退款。

### P1：真实支付适配器
- 微信/支付宝/银联/聚合收银 adapter；
- webhook 验签、查询、退款、对账；
- 终端扫码/碰一碰硬件联调；
- 支付失败回滚与人工处理。

### P2：会员与财务增强
- 会员卡储值、充值赠送、余额退款；
- 券模板/领取/核销/回滚；
- 财务对账差异工作流、导出、审批；
- 多门店/多收银员/多班次报表。

## 15. 验收标准

- 组合支付金额恒等式成立，任何一步失败可回滚或明确进入人工处理。
- 积分/券不能重复核销；并发下余额不为负。
- 现金应有/实盘/差异可追溯，关班需审批。
- 支付成功、退款、对账三方能对上。
- 碰一碰/扫码适配器可替换，业务代码零修改。
- 离线队列不产生“假成功”，补传幂等。
- 操作有审计，收银员/店长权限分离。

## 16. P0 实现对照（2026-10-18）

### 已落地
| 设计项 | 实现 |
|---|---|
| 班次 | `pos_shift`；开班、按收银员查未关班次、关班、备用金、现金收/付、差异原因 |
| 销售单 | `pos_sale` + `pos_sale_line` + `pos_sale_allocation`；草稿、完成、作废、整单退款 |
| 组合支付 | `pos_tender`；CASH/WECHAT/ALIPAY/CARD/STORED_VALUE/OTHER；现金实收/找零 |
| 现金 | `pos_cash_movement`；OPENING_FLOAT/SALE/REFUND/CASH_IN/CASH_OUT |
| 积分 | `member.points_balance` 投影 + `points_ledger`；REDEEM/EARN/REFUND，业务键幂等 |
| 储值 | `member.stored_value_balance` + `member_card_ledger`；消费/退款 |
| 优惠券 | `coupon` + `coupon_redemption`；ISSUED→LOCKED→USED，退款回滚，行锁防并发 |
| 退款 | `pos_refund` + `pos_refund_line` + `pos_refund_allocation` + `pos_refund_tender`；整单/按行部分退款、原路分摊、积分比例扣回、券回滚、批次库存回补 |
| 对账 | `pos_reconciliation` 表；当前报表按 tender + 退款计算，真实渠道接入后写入实收 |
| Provider SPI | `PosPaymentProvider` + `MockPosPaymentProvider`；业务层不依赖厂商 DTO |
| API | `/store/v1/pos/**`、`/admin/v1/pos/**`、`/mock/pos/**` |
| UI | 门店工作台「收银台」；运营后台「门店收银」 |

### 关键实现规则
- 销售单创建即按门店 FEFO 预占批次；完成销售 `CONFIRMED → SHIPPED`；作废/退款释放或回补。
- 退款只在原班次仍为 OPEN 时允许，避免已关班现金被事后修改；跨班次退款需门店财务流程。
- 现金找零只允许在最后一笔收款；退款/作废均写负向现金流水。
- 退款回补批次仅允许 `AVAILABLE/NEAR_EXPIRY`；过期/隔离/冻结批次明确报错，转人工报损流程。
- `Idempotency-Key` 使用 PostgreSQL advisory lock；tender 还有独立幂等键。
- 金额单位全部为分；积分按 1 积分 = 1 分钱；每 1 元实付发 1 积分。

### 仍待实现
- 真实微信/支付宝/银联/聚合收单适配器、webhook 验签、查询/撤销/对账文件。
- 支付宝碰一碰终端认证与设备绑定。
- 退货入库质检、退款审批工作流。
- 储值赠送、会员等级折扣、优惠券批量发放与核销统计。
- 收银员锁屏、店长授权改价/退款、门店-员工绑定、班次审核。
- 离线队列与设备签名。


## 17. 部分退款实现补充

- `POST /store/v1/pos/sales/{id}/refund-lines`，请求体包含 `items[{saleLineId, quantity}]`、`reason`、`operatorName`。
- 退款金额按整单 `payable/subtotal` 比例分摊到商品行，使用最大余数法保证合计等于应收；再按累计退款数量比例计算本次金额，最后一笔精确收口。
- 原 tender 按金额比例分摊退款；现金写负向 `pos_cash_movement`，储值写退款 ledger，外部渠道调用原 provider 的 `refund`。
- 批次回补按 `pos_sale_allocation` 逐批记录到 `pos_refund_allocation`；过期/隔离/冻结批次明确失败，转人工报损。
- 销售单状态 `COMPLETED → PARTIALLY_REFUNDED → REFUNDED`；积分按退款比例扣回，全部退完时退回抵扣积分并恢复优惠券。
