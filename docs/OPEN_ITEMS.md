# 未完成事项总清单（唯一事实来源）

> 本清单**不设上限**，也不做"凑三条"的归类。每条都有出处（文件/代码）与状态。
> 状态口径：✅ 已实现并验证 / ⚠️ 已实现未验证 / ⬜ 未实现 / 🚫 有意不做（附原因）。
> 维护规则：完成一条就改状态并注明验证方式；新增缺口必须当天登记，不允许只留在对话里。

生成时间：第 144 轮（基于当前 `main` 代码与全部 docs）。

---

## A. 支付与资金

| # | 事项 | 状态 | 依据 / 说明 |
|---|---|---|---|
| A1 | 真实支付适配器 | ⚠️ | 微信：下单/退款/回调验签+解密+幂等入账全部实现并有测试；支付宝：precreate 下单、**退款（响应验签）**、**回调验签（含 app_id 校验）** 均已实现；**两渠道的渠道对账单下载未做** |
| A2 | 支付回调/对账 | ✅ | 回调（微信验签+解密+幂等、支付宝验签+app_id）、账单下载（tradebill，签名覆盖 query）、**对账匹配（MATCHED/BILL_ONLY/LOCAL_ONLY/AMOUNT_MISMATCH，幂等）** 与差异查询接口均已实现并有测试 |
| A3 | 退款到真实渠道 | ⚠️ | Mock 支付按支付记录 provider 原路退回（有测试）；真实渠道缺适配器时明确拒绝，不回落 |
| A4 | 退款累计校验 | ✅ | V65 `payment.refunded_amount` + DB CHECK；退两次超额被拒（有测试）。**外部幂等键仍未做** |
| A10 | 时间存储与生成时区 | ✅ | 库里时间列均为 `TIMESTAMPTZ`；订单号改用显式时区（`app.timezone`，默认服务器时区），不再隐式依赖 TZ |
| A9 | 按行退款数量累计 | ✅ | V67 数量可空；金额分摊写 NULL，整件才写数量（有两条测试）；不再撞"数量超限" |
| A5 | 退款原路退回 | ✅ | 按支付记录的 `provider` 退款；渠道无适配器时明确拒绝、不回落 Mock（有测试）；台账行本轮补正 |
| A6 | 部分退款佣金回冲 | ✅ | DEFER 口径：开单冻结（按退款占比）→ 售后关闭；退货完成回冲 `reversed_amount`，驳回/换货解冻不扣（单测 + **E2E 端到端**） |
| A7 | 退款按行分摊落库 | ✅ | `PaymentService.refund` 默认落行分摊（售后走 `recordForAfterSale`，传 false 防重复）；`allocate` 是纯计算器、`allocateAndRecord` 才写库（有测试） |
| A8 | 成本核算（批次成本 + 移动加权平均） | ✅ | 采购收货→批次+加权成本→拣货回写批次→售后退货按原批次回冲；无批次/无成本时明确拒绝（有测试）|

## B. 分销与提现

| # | 事项 | 状态 | 依据 |
|---|---|---|---|
| B1 | 分销配置后台表单（UI） | ✅ | 设置页「分销设置」13 项；**E2E 已验证**：改值→保存→刷新仍在→接口复核（level1=7、min=20000） |
| B12 | 员工佣金走工资（PAYROLL）与会员钱包分离 | ✅ | V60 `payee_channel`；员工不计入可提现余额（有测试） |
| B2 | 二级分佣的上级关系维护 | ✅ | `POST /admin/v1/distribution/promoters/{id}/upline`；二级佣金有测试（500/200） |
| B3 | 绑定方式与时机实际执行 | ✅ | ONCE 不抢客、PERMANENT 可接管、REGISTER 时结算不建关系（三条测试） |
| B4 | `AFTER_SALE_CLOSED` 冻结释放扫描 | ✅ | 订单 `fulfilled_at` + 售后窗口；任务**并集**释放两种来源（有测试） |
| B5 | 提现：申请人选打款方式 | ✅ | V58 `payout_method`/`payout_account`；须在 `distribution.payout-methods` 白名单内（有测试） |
| B6 | 提现：审核走权限矩阵 | ✅ | 新增 `withdrawal.review`（默认仅 ADMIN，可在矩阵授予）；去掉写死角色（有测试） |
| B7 | 提现：最低金额读配置 | ✅ | 低于 `distribution.min-withdrawal` 直接拒绝（有测试） |
| B8 | 提现真实打款 | ⚠️ | 微信商家转账（v3 `/v3/transfer/batches`）请求+签名+失败可见已实现，stub 验证签名；经 `PayoutGateway` 端口接入提现（未配置则明确报 PAYOUT_NOT_CONFIGURED）；**真实开通与 openid 获取未做** |
| B9 | 佣金可拆分消耗（提现额大于单条佣金） | ✅ | `consumed_amount` 部分消耗 + 余额扣减；未扣完保持 AVAILABLE（有测试） |
| B10 | 客服发商品 = 走同一套绑定策略 | ✅ | 已按决策（客服佣金走 PAYROLL）统一：客服发商品**不再生成 ref 链接**、`referralMode=NONE`；请求里带 mode 明确拒绝（此前 `ref=<客服ID>&mode=<自填>` 可绕过分销绑定），含 2 条测试 |
| B11 | 分销报表 | ✅ | `GET /admin/v1/distribution/report?from&to`：冻结/可提（扣消耗）/已提/工资/钱包/单量 + 合计（有测试） |

## C. 促销与优惠券

| # | 事项 | 状态 | 依据 |
|---|---|---|---|
| C1 | 叠加/互斥可配置（PRO-003） | ✅ | `app.promotion.stacking-mode`（BEST_ONLY 默认 / STACK）；STACK 下按优先级叠加，同互斥组只生效一条；试算与结算共用同一入口；含测试 |
| C2 | 券叠加策略按券属性 | ✅ | 统一判定 `CouponStackingPolicy`：全局 `app.promotion.coupon.mode` + **单券 `coupon_template.stackable` 覆盖**（NULL 跟随全局）；互斥语义为"取更优者"；单元测试覆盖覆盖/NULL/非法配置 |
| C3 | 券与促销优先级文档化 | ✅ | `REFUND_POLICY.md` §6 |
| C4 | 券的部分消耗（拆单分摊） | ✅ | 券额**按行金额比例**摊到订单行（余数给最大行，总和严格等于券额），下单即写 `order_coupon_allocation`；查询接口 `/admin/v1/orders/{id}/coupon-allocations`；3 条测试 |
| C5 | 券模板编辑/停用 | ✅ | `PUT /admin/v1/coupons/templates/{id}`（仅草稿）+ `POST .../disable`；停用后停止发放、已领券不受影响（有测试）|
| C6 | 券效果统计 | ✅ | `GET /admin/v1/coupons/templates/{id}/stats`：已领/已用/过期/未用 + 领取率/核销率；后台「统计」弹窗（有测试）|
| C7 | 品牌范围选择器 | ✅ | V72 补 `brand` 表（含演示数据）；`scope-options?scope=BRAND` 实测返回品牌（有测试）——**此前 brand 表不存在，选品牌必然 500** |
| C8 | 促销规则编辑 | ✅ | 草稿行「编辑」→ 弹窗复用创建表单 → `PUT /rules/{id}` → 列表刷新；**E2E 验证改名生效** |
| C9 | 砍价 / 满赠阶梯 / 第二件半价 | ✅ | 规则 + **结算接入**（`promotion_ladder` 表 + `PromotionLadderService`，默认关闭 `app.promotion.ladder-enabled`）：开启后满赠给赠品、第二件半价按降序两两分组；已有活动型优惠时跳过（保守口径）；含 2 条集成测试 |
| C10 | POS 专属促销配置界面 | ✅ | 后台「POS 专属促销」页：channel 固定 POS 创建 + 列表（按 channel 过滤）；线上/门店两套互不影响 |
| C11 | 秒杀高并发增强 | ⚠️ | **令牌桶限流 + 并发上限已实现并测**（429/503 语义），试算接口 `seckill=1` 走闸门；**未做：库存预扣队列、真实压测数据** |

## D. 订单、履约、售后

| # | 事项 | 状态 | 依据 |
|---|---|---|---|
| D1 | 游客订单访问令牌 | ✅ | V62 `access_token_hash`；下单响应头返回一次；履约接口接受 `X-Order-Token`（有测试）；**手机号仍可作为兜底** |
| D2 | 订单详情后台接口 | ✅ | `GET /admin/v1/orders/{id}`（含订单行）；后台订单详情已切到该接口（有测试） |
| D3 | 换货退回旧货的库存回补 | ✅（澄清） | **前提有误**：`shipExchange` 要求状态 `RECEIVED`，而 `receive` 对 RETURN/EXCHANGE 都写入 `QUARANTINE` 批次与库存流水（代码核实） |
| D4 | 退货入库质检 | ✅ | `POST /admin/v1/after-sales/{id}/quality`：仅 RECEIVED 后、只能记一次、PASS/FAIL 校验；不合格保持隔离转人工（有测试） |
| D5 | 售后操作按钮 loading | ✅ | 已核实：`AfterSalesView` 有 4 处 `:loading`（同意/驳回/收货/退款），详情弹窗无异步请求故不需要 |
| D6 | 履约单取消 | ✅ | `POST /admin/v1/orders/{orderId}/fulfillments/{id}/cancel`：未拣货可取消并**释放库存预占**；PICKING/PACKED 需人工盘点、SHIPPED 后走售后；退款不在此处（归售后），2 条测试 |
| D10 | 自营履约「接受/打包」接口 | ✅ | 新增 `/store/v1/fulfillments/{id}/accept` 与 `/pack`；此前自营订单**永远无法发货** |
| D11 | 会员订单页硬刷新 404 | ✅ | 令牌落 cookie（`fullmall_member_token`），订单页用 `useCookie` 读取；**E2E 已验证硬刷新可渲染履约** |
| D13 | 砍价营销 | ✅ | 后端（活动/发起/帮砍/下单/底价推荐）已有；**新增活动详情接口** 与前台 `/bargain/{id}` 页面（当前价、已砍刀数、每刀区间、底部价、发起与帮砍）；底价低于成本需显式确认 |
| D12 | 换货到期自动完成 | ✅ | 发货后 N 天（默认 7，可配 1..60）无异议自动完成；顾客 `POST .../not-received` 阻断并转人工（有测试） |
| D7 | 后台退款 | ✅ | 接口 + 订单详情「发起退款」（金额按元、原因必填）+ E2E 验证；按行分摊自动完成 |
| D8 | 物流轨迹真实调用 | ✅ | 请求构造/签名/HTTP 发送/响应解析全部实现；**mock 服务器验证**请求含 customer/sign/param 且不含密钥；未配置时明确拒绝 |
| D9 | 物流轨迹展示 | ✅ | 轨迹区 + 「查看物流轨迹」按钮；E2E 已验证点击后展示「已揽收」（根因：把 `useAsyncData` 的 data 当对象调 `refresh()`） |

## E. 会员与权益

| # | 事项 | 状态 | 依据 |
|---|---|---|---|
| E1 | 短信真实通道 | ⚠️ | 阿里云（dysmsapi）+ **腾讯云（tencentcloud-sdk-java-sms:3.2.18）** 两个适配器，均在独立包、缺配置启动失败、channel=SMS（有测试）；真实发送未联调 |
| E2 | 微信模板消息 | ⚠️ | WxJava 已接入，配置契约已测；真实发送未联调（需公众号凭据与模板） |
| E3 | 生日礼积分 | ✅ | 按等级 `birthday_points` 发放；业务时区 UTC+8；`birthday_gift_year` + `points_ledger.business_key` 双幂等（有测试）；定时任务默认开启 |
| E4 | 会员等级后台配置 | ✅ | **原本就有**（门店收银 → 会员等级 Tab，GET/PUT `/admin/v1/pos/member-levels`）；**E2E 验证改折扣并落库** |
| E5 | 积分过期 | ✅ | 已由 K3 实现（后台可配 NEVER/ROLLING_DAYS/FIFO，默认永不过期） |
| E6 | 储值充值 | ✅ | 门店代充（现金/扫码后录入）已实现：**默认关闭开关**（预付卡需资质）+ 幂等键 + 流水 + 余额锁定；有测试 |
| E7 | 评价审核 | ✅ | `GET /admin/v1/reviews` + `POST /admin/v1/reviews/{id}/status`（PUBLISHED↔HIDDEN，权限 `product.manage`，有测试）；后台页面已做并 E2E 验证 |
| E8 | 评价图片/晒单 | ✅ | 表 + 提交接口 + 列表聚合；作者校验/最多 5 张有测试 |
| E9 | 收藏与足迹页面 | ✅ | `/favorites` 与 `/history` **均通过带数据 E2E**（标题、价格、次数）；导航入口齐 |
| E10 | 会员消息中心 | ✅ | storefront `/notifications`（未读计数、标记已读、防重复）；导航入口已加；E2E 验证登录前后行为 |

## F. 商品与库存

| # | 事项 | 状态 | 依据 |
|---|---|---|---|
| F1 | 品牌管理 | ✅ | 后端 CRUD（编码唯一/被引用禁止删除，有测试）+ **后台品牌页**（列表/新建/改名/删除，含引用数提示） |
| F2 | 成本导出（ERP 口径） | ✅ | 生成器 + 导出任务资源 `sku_cost` + **后台导出中心页**（选资源→生成→轮询→下载）；含分/元双列与转义测试 |
| F3 | 盘点差异审批 | ✅ | 阈值（默认 5 件）以上挂起不调账，ADMIN 通过后才改库存并写流水；驳回保持库存不变；有 3 条测试 |
| F4 | 调拨在途库存 | ✅ | 在途**双向建模**：发出即扣源批次（因此在途天然不可售，有测试），接收才入目标批次；`GET /admin/v1/warehouse/in-transit` 提供在途明细 |
| F5 | 商品批量编辑 | ✅ | 批量改价、批量上下架、CSV 导入（整批校验/失败不落库/行号报错） |
| F6 | SKU 规格矩阵 | ✅ | 后端批量生成（编码规则/幂等/校验，有测试）+ 接口 + **后台「SKU 规格矩阵」页**（填前缀与规格值→生成→显示已建/已跳过编码） |
| F8 | 成本展示 | ✅ | 后台「成本查询」页（输入 SKU → 均价/在库金额/最近批次/成本未知提示）；接口 `cost-view` 仅 ADMIN |
| F7 | 素材存储 | ✅ | 端口 + 七牛/COS/本地适配器 + 上传接口 + **后台「素材库」页**（上传/预览/复制链接）；缺配置启动失败、路径穿越拒绝，测试齐全 |

## G. 供应商与结算

| # | 事项 | 状态 | 依据 |
|---|---|---|---|
| G1 | `/open/v1` 统一 API 契约 | ✅ | 契约文档 `docs/OPEN_API_CONTRACT.md`（鉴权签名串、接口清单、状态机、错误码、限流、Webhook）；已核对：`/open/**` 全路径强校验 HMAC，无白名单例外 |
| G2 | 供应商待办中心 | ✅ | 轮询接口 `GET /open/v1/todos` + **门户「待办」面板**（待接单/待处理退货，可刷新）；WSS 实时推送按需另立 |
| G3 | 供应商结算 | ✅ | 计算 + 退货扣减 + 后台弹窗 + **供应商门户结算面板**（含 CSV 下载）；两处均 E2E 验证 |
| G4 | 供应商 CSV | ✅ | 导出 `GET /open/v1/settlements.csv`；导入 `POST /open/v1/inventory/feed.csv`（复用解析器与校验）；均 E2E 验证；行级错误返回 400 带明细 |
| G5 | 供应商 Webhook | ✅ | 注册接口 + 事件投递（`enqueue`/`deliverDue`）+ **HMAC-SHA256 签名**（含时间戳防重放）+ 指数退避重试（最多 5 次）+ 投递记录查询；stub 验证 2 条测试 |
| G6 | 供应商退货对账 | ✅ | 按供应商汇总已同意退货 + **对账审计**（已同意 vs 结算扣减，返回差额并 WARN）；差额非 0 时不静默通过 |
| G7 | 采购入库/收货单 | ✅ | V80 采购单+收货单；收货同时写 `inventory_batch`（含批次单位成本）并更新加权平均成本；必须指定库存地点；重复收货被拒（有测试） |

## H. 平台与技术债

| # | 事项 | 状态 | 依据 |
|---|---|---|---|
| H1 | 审计 outbox 崩溃恢复 | ✅ | 入口（任务+接口）+ **按 id 重载实体再提交**（实测能消费为 DONE）；只对已注册加载器的实体生效 |
| H1b | 未注册加载器的实体恢复 | ⬜ | 无加载器时仍退回反序列化（可能失败并计 FAILED）；目前仅 asset 注册 |
| H2 | 导出文件对象存储（OSS） | ⚠️ | 阿里云 OSS 官方 SDK 适配已实现（`app.export.storage=oss`），配置缺失启动即失败（有测试）；**真实上传/签名 URL 未联调**（需凭据） |
| H20 | 后台帮助体系 + 覆盖率检查 | ✅ | 22 个页面帮助（步骤/运营建议/常见错误）+ 顶栏帮助抽屉 + `docs/HELP_INDEX.md`；`check-help-coverage.py` 已接入 verify，**新增页面不写帮助即失败**（已反证） |
| H21 | 订单行批次引用 | ✅ | 单行来源写列、**合并行走 `pick_line_order_line` 映射表**；拣货确认统一回填（有测试：两张订单行都回填成功），退货可精确回冲 |
| H22 | 列表深页 offset | ✅ | 后端 6 个 keyset 接口 + 前端**订单/售后/评价**三处已切（加载更多、无页码），E2E 覆盖；客户与供应商列表保留 offset（数据量小，非热点） |
| H19 | 文档路径契约测试 | ✅ | `DocumentedPathsExistTest` 扫描本清单中出现的接口路径并与真实路由比对；**防止"文档说做了、接口不存在"**（有测试） |
| H18 | 通知渠道连通性自检 | ✅ | 独立 `NotificationAdminController`；系统设置页「渠道自检」按钮；**E2E 验证**（发送→SENT 提示）；未知渠道 400 |
| H17 | 通知失败重试 | ✅ | 接口 + **后台通知页「重试」按钮**（仅 FAILED 显示、`:loading` 互斥）；已 SENT 拒绝重试（有测试）；页面未做浏览器验证 |
| H16 | 清理接口拒绝未知参数 | ✅ | `POST /admin/v1/maintenance/retention` 传 `?dryRun=true` 曾**被静默忽略并真实删除**；现返回 400 `UNSUPPORTED_PARAMETER`（有测试） |
| H15 | 导出处理任务默认开启 | ✅ | `export.worker` 默认开启 + V69 修正存量行；此前导出永远 PENDING |
| H3 | 导出分页 | ✅ | 查证：导出为**单次查询 + 硬上限**（`maxRows+1`，超出即失败），**不存在 offset 深翻**问题；此前的“offset 分页”描述有误 |
| H4 | 测试期调度错误日志根因 | ✅ | `ExportJobScheduler` 每任务一条 ERROR 造成刷屏，改为每轮汇总一条（含数量与首因） |
| H5 | `order_line.created_at` | ✅ | V71 补列（按订单时间回填并设 NOT NULL）+ 索引；行退款顺序改用 `created_at, id` |
| H6 | 前端 loading 缺口 | ✅ | 全量扫描 0 剩余：商品上下架、提货核销、拣货确认、售后三个动作、促销启停、券启用、通知已读、员工 PIN、POS 会员查询/解锁 |
| H7 | 列表排序 | ✅ | 六个列表均已白名单排序；**深页仍为 offset**（见 H22） |
| H8 | 移动端（uni-app） | ⚠️ | 首页/详情/购物车/**下单表单（地址+手机号→`/api/v1/checkout`，带幂等键）** 全部完成，H5 构建通过；**微信小程序真机实测未做（需 appId）** |
| H9 | 多语言文案 | ✅ | 文案 bundle + 前台语言切换（页面按当前 locale 请求）；商品译文录入/审核/发布 + 后台页；**E2E 验证**：发布 en 标题后 `/products/{id}?locale=en` 返回英文、`zh-CN` 不含英文 |
| H10 | CI 真实执行验证（GitHub/Gitea runner） | ⬜ | workflow 已写，未跑过 |
| H11 | compose 部署验证 | ✅ | 本机 Docker 实跑：lite 栈（postgres/valkey/rabbitmq/meilisearch/mailpit）全部 healthy，端口 15432/16379/15674/17700/11025 可连；另起 API 实例连 lite 库完成 **商品列表 → OTP → 下单 → mock 支付**；core 栈占用默认端口时 `make up-lite` 会正确拒绝 |
| H12 | E2E 覆盖缺口 | ✅ | 已补：券结算、退款、物流轨迹、分销设置、评价页、**售后换货全流程**（申请→审核→收货→发货→完成） |
| H13 | 性能基线（压测、慢查询） | ✅ | 新增 `scripts/bench-local.py`；本机并发 20×100：商品列表 P95 45ms/679rps、详情 P95 22ms/1434rps、促销试算 P95 90ms/559rps，0 错误；结果记入 `docs/PERF_BASELINE.md`（生产容量仍需独立压测环境） |
| H14 | 备份/恢复演练 | ✅ | 本机 pg_dump（580KB）→ 新库 `fullmall_restore` pg_restore；表数 150、商品 11、订单 68、Flyway 版本 116 与原库完全一致 |

## I. 安全与合规

| # | 事项 | 状态 | 依据 |
|---|---|---|---|
| I1 | 员工登录失败锁定/验证码 | ✅ | 已实现并有测试 |
| I2 | 会员 OTP 限流与失败计次 | ✅ | 已实现并有测试 |
| I3 | 后台敏感配置脱敏（AK 回显 `******`） | ✅ | 已实现并有测试 |
| I4 | 生产/演示隔离（mock 不进 prod） | ✅ | 就绪检查 + 运行时拒绝；启动不阻断 |
| I5 | 字段级权限（供应商只看自己字段） | ⬜ | `AGENTS.md` 说"ERP 明确需要时再做" |
| I6 | 操作审计查询 | ✅ | 接口 + **后台「操作审计」页面**（类型/操作人/时间范围/分页），E2E 已验证打开与筛选 |
| I6b | JaVers 快照查询 | ✅ | 已接到「操作审计 → 实体快照」标签，按类型+ID 查询；有 E2E（合法类型返回空列表、非法类型明确报错）；仍只有 asset 一个实体注册 |
| I9 | 操作审计覆盖面 | ✅ | 新增 `PAYMENT_SUCCEEDED`/`PAYMENT_REFUNDED`/`COUPON_REDEEMED`/`ORDER_CANCELLED_BY_SYSTEM`/`ORDER_REFUND_REQUESTED`；累计 21 处写入（券核销有断言测试，订单取消仅代码接入） |
| I7 | 个税/发票（佣金、提现合规） | ⬜ | 未涉及 |
| I8 | 隐私政策/用户协议版本化与同意记录 | ✅ | `legal_*` 表 + 接受接口 |

## J. 有意不做（明确记录，避免重复讨论）

| # | 事项 | 原因 |
|---|---|---|
| J1 | 线上匿名下单 | 大陆场景必须绑定真人；改由 OTP 访客 + 收银台代下单覆盖 |
| J2 | 客户与客服"同一身份"建模 | 两套账号体系严格隔离，不存在歧义 |
| J3 | 独立 `payment_sub_merchant` 表 | 全商城一个子商户号，不需要映射表 |
| J4 | 手填 JSON 配置业务规则 | 全部改为结构化表单，JSON 由服务端生成 |
| J5 | 部分退款自动退券 | 会造成同一张券用两次（资损） |
| J6 | — | — | 已转为功能项 D12 |

| K1 | 生日礼规则 | ✅ | 按会员时区（未设时区跟随系统，系统默认=服务器时区或 `app.timezone`）；**改生日不重复领**（年度幂等）；**生日为空不发放**（防元旦全量误发）；有测试 |
| K8 | 前端时区 | ✅ | 管理端固定东八；顾客端 `formatMemberDateTime` 按会员时区（`/member/auth/me` 返回，登录后写 cookie，未取到退回浏览器时区） |
| K2 | Admin 首次配置 | ✅ | 4 步向导（基础/运营/服务商/完成，可跳过）+ 常驻提示；后端 status/complete；**E2E 走完整个向导** |
| K3 | 积分过期 | ✅ | 后台可配 `member.points.expiry-mode`（NEVER 默认 / ROLLING_DAYS / FIFO）+ 天数；按逐笔余量扣、写 EXPIRE 流水、余额不为负、非法配置报错；任务 `member.points-expiry` 默认开启（NEVER 时空跑）（有测试） |
| K4 | 佣金 DEFER 自动触发 | ✅ | `AfterSaleService` 在 create/refund/reject/completeExchange 调用 `CommissionHoldService`；冻结期间不释放、不可提现 |
| K5 | 供应商售后 | ✅ | 模型 + 后台/供应商接口（越权 404）+ 结算扣减（有测试）+ **供应商门户退货面板**（同意/拒绝）；**E2E 已验证**连接后可见并可接受 |
| K6 | AI 接口层 | ✅ | 推荐位（规则版）+ **OpenAI 兼容文案/生图客户端**（可接内网模型）：配置即用、非 2xx/空 choices/空 url 均报错；`/ai/copy` 已接线，未配置返回 503；stub 验证 2 条 |
| K7 | AI 事件采集 | ✅ | 后端表 + 上报/统计接口；**前端埋点已接**：加购与搜索词上报（失败仅 warn，不影响交互） |

---


## L. AI 客服模块（第二期）
| 编号 | 条目 | 状态 | 说明 |
|---|---|---|---|
| L1 | 构建分版（community 不含 ai） | ✅ | `-Pcommunity`（默认）/`-Penterprise`；AI 源码在 `src-enterprise/java`；`EditionPackagingTest` 两种构建下均通过 |
| L2 | AI 骨架 + 回答约束 | ✅ | `/ai/status`、`/ai/ask`；**四道约束**：无资料不调用模型、金额必须可追溯、越权承诺拒答、声称已执行动作拒答（4 条单测） |
| L3 | 知识库（存储 + 混合召回） | ✅ | `knowledge_chunk`（V74，嵌入存 bytea）+ 关键词候选 + **应用内余弦重排**；嵌入客户端（OpenAI 兼容）；超 10 万块再切 pgvector（有测试） |
| L4 | RAG 问答 | ✅ | 检索→提示→模型→约束→引用链路已实现；无命中不调用模型、金额须可溯源、越权承诺拒答（JSON 契约 + 测试） |
| L5 | FAQ 自学习 | ✅ | 知识库 + 未命中记录 + 候选清单 + 采纳 + **后台「FAQ 知识库」页**（候选/采纳/搜索）；闭环有测试；向量语义检索未接（属 L9） |
| L6 | 演示模式（mock） | ✅ | `app.ai.mode=demo`：关键词检索 + **抽取式回答**（不调外部模型）；资料导入→检索→回答→引用链路打通（4 条单测） |
| L8 | 客户资料导入（格式与用法） | ✅ | `POST /admin/v1/ai/knowledge/import`；Tika 解析 pdf/docx/txt/md/csv/html/xlsx；切块 500 字；不支持/空文档明确报错；指南见 `docs/AI_KNOWLEDGE_IMPORT_GUIDE.md` |
| L9 | 生产模式（向量检索 + 模型生成） | ⚠️ | 对话模型已真实联调；**向量检索待实施**（pgvector 方案已记录，等真机解除 Docker 限制后落地） |
| L16 | 商品变更自动同步知识库 | ✅ | V75：`sku/product.updated_at`（触发器单调递增）+ `knowledge_sync_state` 水位；任务 `ai.knowledge-sync` 每 30 秒巡检；改价后自动替换旧价格（有测试） |
| L15 | 向量存储技术路线（ADR-001） | ✅ | 澄清测试库缺扩展的原因、"10 万"指块数、实测 54ms/100k（256 维）；升级触发条件与选定路线写成文档 |
| L12 | 结构化输出契约（JSON） | ✅ | 模型必须返回 `{answer,grounded,needsHuman,citationIds}`；解析失败即拒绝使用，不再靠词组判断 |
| L13 | 商品知识全量重建 | ✅ | `POST /admin/v1/ai/knowledge/rebuild` 索引全部在售 SKU；实测 11 商品 / 19 块 |
| L14 | 真实模型联调 | ✅ | 实测 `deepseek-v41`：问「有机鲜牛奶多少钱」→ 答 19.90 CNY + 引用真实商品；非 JSON 输出被拒 |
| L11 | AI 后台页 | ✅ | 状态/资料导入/提问测试（含引用与违规原因）；社区版优雅提示「不含 AI 模块」（E2E）；企业版导入→提问链路 E2E 通过 |
| L10 | 商品知识同步 | ✅ | `POST /admin/v1/ai/knowledge/sync?skuId=` 重建 SKU 知识块；改价后旧价格消失（有测试） |
| L7 | 训练机部署脚本（vLLM/Ollama + embedding） | ⬜ | 192.168.1.15 可达；`.8` 需 BMC 开机 |

## M. 授权与试用
| 编号 | 条目 | 状态 | 说明 |
|---|---|---|---|
| M1 | 许可证验签（Ed25519） | ✅ | 文件 `{payload,signature}`，签名覆盖 payload 原始字节；篡改/错公钥/畸形内容均拒绝（6 条单测） |
| M2 | 域名绑定与只读模式 | ✅ | 通配域名匹配 + localhost 放行；不符/过期/回拨 → **只读**（GET 通、写 402） |
| M3 | 打包预设（永久/7 天/30 天） | ✅ | `LicenseIssuer` CLI：Ed25519 签发 + **签完自检**；`--days 0` 为永久；私钥落 `~/.dsh-license/`（已 gitignore）；有测试 |
| M4 | 时钟回拨检测 | ✅ | `license_state.last_seen_at` 只前进；回拨超 24h → 只读（逻辑实现，含在评估链中） |
| M5 | 许可服务端 | ⬜ | **决定：先做离线证书**；公网服务端后置（需要时再加） |
| M6 | 后台许可页 | ✅ | 「许可证」页：状态/版本/客户/到期/功能/只读标记 + 上传新证即时生效；验证失败如实显示原因 |

## N. AI 深度参与（ADR-002）
| 编号 | 条目 | 状态 | 说明 |
|---|---|---|---|
| N1 | AI 阶段1：模型自部署切换 | ⬜ | 只改 base-url；验收：质量 ≥ 外部模型 90%、数据不出内网 |
| N2 | AI 只读工具调用 | ✅ | 白名单工具（order.status/sku.availability/sku.price）+ 参数校验 + 全只读，含写操作名被拒测试 |
| N3 | AI 内容生产（替代语义化控制） | ✅ | **已移除**"AI 改价/上下架/发券"这类语义化控制（人工点两下即可，纯为 AI 而 AI）；改为**素材生产**：多供应商（图片 openai-images/dashscope-image、视频 dashscope-video、文案 openai-chat）、异步任务队列（`ai_gen_job`）+ 结果自动入素材库、提示词与导图留痕（`ai_prompt_history`/`source_asset_ids`）、提示词优化（走 vLLM 等 openai-chat）、后台「AI 素材生成」页；API Key 加密落库；stub 全链路测试 |
| N4 | AI 阶段4：自然语言查库（可选） | 🚫 | 仅当工具化不够时；护栏缺一不可（见 ADR-002） |
| N5 | AI 检索评测 | ✅ | 三轮实测：仅商品 12% → +34 FAQ 84% → +39 FAQ **90%**（验收线 80%），见 `docs/AI_EVAL_RESULTS.md`；剩余失败以资料缺口与同义词为主 |
| O1 | 多维度规格生成 | ✅ | 服务+接口（笛卡尔积/多维 options/组合禁用/幂等）+ **后台页面支持单维/多维切换与禁用组合输入** |
| O2 | 按规格筛选批量改价 | ✅ | 服务 + 接口（预览/应用分离）+ **后台「规格改价」页**（条件→预览改前改后→确认应用）；不填条件直接拒绝 |
| O3 | 单件序列号追踪 | 🚫 | **暂缓（按业务口径）**：这是可选能力，不围绕它做大投入；已具备登记/绑定/批次清单（4 条测试），但**不提供按号追人查询**，业务也不需要 |
| O4 | 商品详情返回 SKU 可用量 | ✅ | 新增 `GET /admin/v1/products/{id}/availability`（批次累加、无批次为 0，有测试）；**前台尚未调用**（接口就绪，零破坏） |
| O5 | 规格改关联表（ADR-003） | ✅ | 关联表为唯一写入源：建商品、矩阵生成、手工回填都会写关联表并**重建 JSON 投影**；规格改价筛选走关联表（未回填即报错）；商品详情读投影（按 ADR 预期） |
| O6 | 临期预警 | ✅ | 每日巡检（07:30）+ 接口 + 后台「临期商品」页 + **站内通知运营**（`app.inventory.expiry-notify` 可关）；测试覆盖查询口径 |
| O7 | UGC 自动审核 | ✅ | 策略（敏感词/人工/异常转人工）+ HTTP 审核适配器（PASS/REVIEW/BLOCK） |
| P1 | 第三方配置入库（加密） | ✅ | 表 + AES-GCM + 读写服务 + **能力白名单**（拼错拒绝，避免死配置）+ 接口；4 条测试（加密/不回显/互斥/白名单） |
| P2 | 后台「第三方服务」页 | ✅ | 能力/服务商 + `key=value` 配置 + 启用开关；列表显示启用状态、**密钥仅显示「已配置」**、非敏感项明文；后端白名单与加密已测 |
| P3 | 适配器按配置装配 | ✅ | 存储/短信/支付/审核均按后台配置装配；demo·dev 未配置→mock，其它→报错 |
| P4 | 后台信息架构重组 | ✅ | 7 组菜单；工作台「待办事项」**每项带筛选路径**（待付款/待发货/待自提→订单页已筛，临期→临期页）；订单/售后页支持 `?status=` |
| P5 | core/lite compose 隔离 | ✅ | 口径：lite 仅作无 Docker 权宜方案，**不并行**；`make up-lite` 前置互斥检查，lite 使用 `LITE_*` 端口与独立 project（防御性） |
| P6 | bind mount 属主 | ✅ | 实机验证：wiremock 未设 user 时以 root 写入 `deploy/`，宿主删不掉；已给服务加 `user:${APP_UID}:${APP_GID}`、新增 `make fix-mount-owner`、并把不存在的 `wiremock:3` 固定为 `3.9.1`；复验容器内 `uid=1000` 且宿主可删除 |

| N7 | AI 素材生产（多供应商 / 异步 / 提示词与参考图资产化） | ✅ | `ai_provider`（同类多家、按优先级或指定；API Key 加密）、`ai_gen_job`（异步队列 + worker，多实例租约）、`ai_prompt_history`（历史提示词可直接上屏）、`source_asset_ids`（参考图/视频引导图，**真实传给模型**：dashscope `ref_img`、openai-images `image`、视频首帧）；协议 openai-images / dashscope-image / dashscope-video / openai-chat / openai-embedding；本机 Ollama `qwen2.5:0.5b` 已注册并实测优化提示词；stub 全链路测试 |
| N8 | 商品多图 / 规格专用图 / 视频编排 | ✅ | `product_media`（MAIN/GALLERY/DETAIL/VIDEO/SPEC + 排序 + 来源 UPLOAD/AI；主图唯一由部分唯一索引兜底；规格图必须绑 SKU）；上传支持 `purpose`（PRODUCT/SPEC/VIDEO/OTHER_SYSTEM），审批附件标记 OTHER_SYSTEM **不入素材库** |

| N9 | 视频分镜（分镜提示词 + 引导图） | ✅ | `ai_storyboard`/`ai_storyboard_shot`：镜头按序保存提示词、引导图（来自素材库实拍图）、时长（1–30s）；逐镜头入队生成、完成后按 job 回填资产；后台「视频分镜」页支持保存/生成/同步；含测试 |

| N10 | 多参考图 / 角色定型 / 分镜多引导图 | ✅ | 任务支持 `images[{assetId,role}]`（REFERENCE/SUBJECT/FIRST_FRAME/LAST_FRAME），服务端解析成真实 URL 传给模型；新增协议 `ark-image`/`ark-video`（火山方舟 content[]+role，多图参考）、`dashscope-multimodal`（qwen-image-edit 多图 messages）、`minimax-video`（首帧 + subject_reference 角色定型）；分镜每个镜头支持多张引导图（第一张首帧、其余参考）；各协议传参差异见 `docs/AI_PROVIDER_PARAMS.md`；含 Ark 多图/首帧、**OpenAI images/edits multipart 多文件、qwen-image-edit messages 多图、MiniMax first_frame+subject_reference** 四组官方形态测试；**本机 wiremock 官方 mock 已端到端跑通**（提交→worker→成功→素材入库） |

## 统计

| 状态 | 数量 |
|---|---|
| ✅ 已实现并验证 | 138 |
| ⚠️ 已实现未验证 / 半接线 | 9 |
| ⬜ 未实现 | 7 |
| 🚫 有意不做 / 暂缓 | 7 |
| **合计** | **162** |
| 另：接口冒烟覆盖 | 43 个接口（读 33 / 写 10） |

> 注：本表是当前代码与文档的真实汇总，不是"挑选重点"。后续每轮完成若干条即更新对应行。