# 权限说明（自动生成）

> 本文件由 `scripts/generate-permission-help.mjs` 生成，**不要手改**。
> 改说明请改 [`docs/permissions.catalog.json`](permissions.catalog.json)，改接口映射请改
> `PermissionService.requiredPermission()`，然后执行 `npm run generate:permissions`。
> 漂移由 `make contract-check` 拦住。

## 关系总则

- **继承**：本项目没有权限继承，也没有父子层级：每个权限各自独立，必须逐个授予。
- **超管**：ADMIN 角色在 PermissionService 里直接短路返回 true，始终拥有全部权限，不进入本矩阵。
- **菜单**：菜单可见性按角色（roles）判断，不由权限控制：取消某个权限后，菜单仍在，但该页所有请求会 403。
- **兜底**：权限过滤器对未登记路径默认要求 permission.manage，因此新增接口若忘了登记映射，会落到它身上。

## 权限清单

| 权限码 | 名称 | 分组 | 拦截的接口 |
|---|---|---|---|
| `dashboard.view` | 查看经营总览 | 工作台 | `/admin/v1/dashboard/**` |
| `product.manage` | 商品管理 | 商品 | `/admin/v1/products/**`<br>`/admin/v1/reviews/**` |
| `order.manage` | 订单管理 | 交易 | `/admin/v1/orders/**`<br>`/admin/v1/after-sales/**` |
| `customer.view` | 客户与会员 | 客户 | `/admin/v1/customers/**` |
| `inventory.view` | 库存与批次 | 库存 | `/admin/v1/inventory/**` |
| `warehouse.manage` | 仓储作业 | 库存 | `/admin/v1/warehouse/**` |
| `promotion.manage` | 促销与分销 | 营销 | `/admin/v1/promotions/**`<br>`/admin/v1/distribution/**（不含 /admin/v1/distribution/withdrawals/**）` |
| `pos.manage` | 门店收银 | 门店 | `/admin/v1/pos/**` |
| `withdrawal.review` | 提现审核与打款 | 财务 | `/admin/v1/distribution/withdrawals/**` |
| `supplier.manage` | 供应商与凭证 | 供应商 | `/admin/v1/suppliers/**`<br>`/admin/v1/supplier-returns/**` |
| `notification.view` | 通知中心 | 运营 | `/admin/v1/notifications/**` |
| `theme.manage` | 主题配置 | 运营 | `/admin/v1/theme/**` |
| `staff.manage` | 员工与权限 | 系统 | `/admin/v1/staff/**`<br>`/admin/v1/stores/**` |
| `settings.manage` | 系统设置 | 系统 | `/admin/v1/settings/**`<br>`/admin/v1/setup/**`<br>`/admin/v1/audit/**` |
| `permission.manage` | 权限矩阵 | 系统 | `/admin/v1/permissions/**` |

### `dashboard.view` — 查看经营总览（工作台）

查看经营总览：营收、订单量、库存告警等汇总指标。

- **勾选后可做**：
  - 打开「经营总览」并读取汇总指标与走势
- **取消后**：
  - 总览页保持可见但接口返回 403，页面显示空数据与错误提示
- **关联接口**：`/admin/v1/dashboard/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `product.manage` — 商品管理（商品）

维护商品、SKU、品牌、规格矩阵，并审核商品评价。

- **勾选后可做**：
  - 新建/编辑/上下架商品与 SKU、改价改库存
  - 维护品牌与规格矩阵模板
  - 审核、回复、隐藏商品评价
- **取消后**：
  - 商品页只能看不能改（接口 403）
  - 评价审核无法提交处理结果
- **关联接口**：`/admin/v1/products/**`、`/admin/v1/reviews/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `order.manage` — 订单管理（交易）

处理订单与售后：发货、取消、退款、售后审核。

- **勾选后可做**：
  - 查看与操作订单（发货、取消、备注）
  - 处理售后申请：同意/拒绝/退款
- **取消后**：
  - 订单列表与售后页的写操作全部 403；只读展示取决于接口实现，通常整页无数据
- **关联接口**：`/admin/v1/orders/**`、`/admin/v1/after-sales/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `customer.view` — 客户与会员（客户）

查看客户与会员资料、积分与消费记录。

- **勾选后可做**：
  - 查看会员列表与详情、消费与积分流水
- **取消后**：
  - 客户页无数据（接口 403）
- **关联接口**：`/admin/v1/customers/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `inventory.view` — 库存与批次（库存）

查看库存与批次、临期商品。

- **勾选后可做**：
  - 查看库存余量、批次与效期、临期清单
- **取消后**：
  - 库存相关页面无数据
- **关联接口**：`/admin/v1/inventory/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `warehouse.manage` — 仓储作业（库存）

执行仓储作业：入库、拣货、盘点、调拨。

- **勾选后可做**：
  - 创建与确认入库单、拣货单、盘点单、调拨单
- **取消后**：
  - 仓储作业页只能看不能操作
- **关联接口**：`/admin/v1/warehouse/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `promotion.manage` — 促销与分销（营销）

营销：促销活动、优惠券、分销（不含提现审核）。

- **勾选后可做**：
  - 创建/编辑促销与优惠券
  - 查看与管理分销关系与佣金
- **取消后**：
  - 营销类页面无法保存；分销提现仍需单独的 withdrawal.review
- **关联接口**：`/admin/v1/promotions/**`、`/admin/v1/distribution/**（不含 /admin/v1/distribution/withdrawals/**）`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `pos.manage` — 门店收银（门店）

门店收银台相关操作：开单、结账、班次交接。

- **勾选后可做**：
  - 进入门店收银台并完成收款、退款、班次管理
- **取消后**：
  - 收银相关接口 403
- **关联接口**：`/admin/v1/pos/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `withdrawal.review` — 提现审核与打款（财务）

资金动作：审核分销提现并打款。

- **勾选后可做**：
  - 审核提现申请、标记打款结果
- **取消后**：
  - 提现审核一律 403；这是与 promotion.manage 分开的独立授权，默认只给 ADMIN
- **关联接口**：`/admin/v1/distribution/withdrawals/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `supplier.manage` — 供应商与凭证（供应商）

供应商档案、凭证与供应商售后。

- **勾选后可做**：
  - 维护供应商资料与资质凭证
  - 处理供应商退货与售后
- **取消后**：
  - 供应商与供应商售后页面无法提交
- **关联接口**：`/admin/v1/suppliers/**`、`/admin/v1/supplier-returns/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `notification.view` — 通知中心（运营）

通知中心：站内通知与消息记录。

- **勾选后可做**：
  - 查看通知列表与详情
- **取消后**：
  - 通知中心无数据
- **关联接口**：`/admin/v1/notifications/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `theme.manage` — 主题配置（运营）

主题配置：PC 商城与微信小程序的主题、配色、首页模块。

- **勾选后可做**：
  - 修改并保存主题配置（按应用端分别保存）
- **取消后**：
  - 主题工作室无法保存，预览仍可看
- **关联接口**：`/admin/v1/theme/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `staff.manage` — 员工与权限（系统）

员工账号与门店：建号、改角色、设 PIN、绑定门店。

- **勾选后可做**：
  - 查看员工列表、设置 PIN、绑定/解绑门店
  - （接口层同时覆盖 /admin/v1/stores 门店列表）
- **取消后**：
  - 员工页与门店列表接口 403
- **关联接口**：`/admin/v1/staff/**`、`/admin/v1/stores/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。
- **注意**：门店（/admin/v1/stores）与员工共用本权限：没有独立的门店权限码。

### `settings.manage` — 系统设置（系统）

系统设置、首次配置引导与审计恢复。

- **勾选后可做**：
  - 修改系统设置（含支付、上传上限等）
  - 首次配置引导、审计事件恢复
- **取消后**：
  - 设置与引导无法保存；审计恢复不可用
- **关联接口**：`/admin/v1/settings/**`、`/admin/v1/setup/**`、`/admin/v1/audit/**`
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。

### `permission.manage` — 权限矩阵（系统）

权限矩阵本身；同时是未登记接口的兜底权限。

- **勾选后可做**：
  - 查看与修改角色权限矩阵
  - 访问任何未在过滤器里登记路径的接口（兜底）
- **取消后**：
  - 无法打开或保存权限矩阵；未登记的新接口也会 403
- **关联接口**：`/admin/v1/permissions/**`（同时是所有未登记路径的兜底权限）
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。
- **注意**：给这个权限等于给出「改授权」的能力，非必要不要授予运营角色。
