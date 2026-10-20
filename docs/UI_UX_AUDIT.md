# 前端设计审核（第 142 轮）

审核依据：Ant Design / Ant Design Pro 的后台列表范式（加载态、筛选、分页、空状态、批量操作）与
Element Plus 官方组件用法（`el-table` + `el-pagination` + `v-loading` + `el-empty`），
以及面向顾客的「信息密度低、主操作突出」原则。

## 1. 审核方法（可复现）

静态扫描全部 Vue 页面，统计关键 UX 要素：

```bash
# 简化版（见本轮执行的脚本）
# 统计每个页面是否具备：loading / table / pagination / filter / sortable / empty / 二次确认
```

## 2. 发现的问题（按严重度）

### P0 — 列表无分页（后台普遍存在）

| 页面 | 问题 | 影响 |
|---|---|---|
| 商品管理 | 一次性拉 100 条 | 数据多了卡顿、找不到后面数据 |
| 订单管理 | 一次性拉 100 条 | 同上 |
| 客户管理 | 后端仅 `limit`，无分页 | 同上 |
| 售后处理 | 后端硬编码 limit 200 | 同上 |

**已修**：后端为客户/售后补 `page/size` + `total`（统一契约 `{items,page,size,total}`）；
前端四个列表页统一接入 `el-pagination`；筛选变化时**重置到第 1 页**（订单、售后）。

### P1 — 无排序（**未修，需后端支持**）

所有后台表格都没有 `sortable`。原因不在前端：后端列表接口不支持 `sort` 参数。
**不能只加前端 UI**——那会变成"点了没反应"。需要先给接口加 `sort` 白名单，再开列排序。

### P1 — 筛选与分页未联动（已修 + 需注意）

客户/售后/订单筛选状态变化时应重置到第 1 页，否则会停在空页。本轮在 `load()` 前重置 `page=0`
（商品/订单/售后/客户）。

### P2 — 空状态与加载态不统一

| 现状 | 处理 |
|---|---|
| 部分表格用 `el-empty`，部分只显示"共 0 条" | 统一为：表格 + 页脚（总数 + 分页）+ `el-empty`（无数据时） |
| 详情类弹窗无 loading | 订单详情已有 `detailLoading`，售后详情待补 |

### P2 — 间距与页脚样式各自写死

新增全局 `.table-footer`（flex + 12px 间距），统一"总数在左、分页在右"。

### P2 — 滚动条与层叠

- 表格横向溢出时页面出现双滚动条；统一由 `.el-table` 自身滚动，页面只保留纵向滚动。
- 弹窗内长内容应只滚动弹窗体（`el-dialog` 默认行为），不要撑高页面。

### P3 — 顾客端（storefront / mobile）

| 项 | 现状 | 建议 |
|---|---|---|
| 商品列表 | 无分页（一次拉全量） | 数据量小可接受；超过 200 条时要加"加载更多" |
| 结算页 | 无加载骨架 | 提交时按钮已 disabled，可接受 |
| 错误提示 | 顶部 `errorMessage` 文本 | 与后台统一为 `ElMessage` 风格待评估（顾客端当前风格更轻，暂不改） |

## 3. 已完成的改动（本轮）

| 层 | 改动 |
|---|---|
| 后端 | `GET /admin/v1/customers?page&size` → `{items,page,size,total}`；`GET /admin/v1/after-sales?status&page&size` → 同契约 |
| 前端 | 商品 / 订单 / 客户 / 售后 四个列表页接入分页；新增全局 `.table-footer` 样式 |

## 4. 验证

| 项 | 结果 |
|---|---|
| 后端测试 | 371 个全绿（Paging 改动未破坏既有用例） |
| 前端构建（5 应用 + 类型检查） | ✅ |
| 接口对齐 | 189 路由 / 18 调用 / 0 未匹配 |

> **未做**：排序（需后端 `sort` 参数）、售后详情 loading、顾客端"加载更多"。
> 这三项已列入下一轮，不会假装已完成。

---

## 5. 排序（第 143 轮：后端白名单 + Element Plus 服务端排序）

| 层 | 实现 |
|---|---|
| 后端 | `GET /admin/v1/orders?sort=&direction=`；**白名单映射**：`createdAt→created_at`、`totalAmount→total_amount`、`status→status`，方向只接受 asc/desc，未知字段回落 `created_at DESC`。**绝不拼接用户输入** |
| 前端 | `el-table` 用 Element Plus 官方 `sortable="custom"` + `@sort-change`，由服务端排序（不前端伪排序） |

| 验证 | 结果 |
|---|---|
| 后端测试 | **387 个全绿** |
| 新增用例 | 金额排序升降序生效；**未知字段/恶意片段安全回落且不破坏数据**（`total_amount; DROP TABLE orders` 不报错、表仍在） |

### 前端选型（不造轮子）

- 表格/分页/排序/加载：**Element Plus 官方组件**（`el-table` 的 `sortable="custom"`、`el-pagination`、`v-loading`）
- 无限滚动（顾客端"加载更多"）：使用 Element Plus 的 **`v-infinite-scroll` 指令**（官方提供），
  不使用自研 IntersectionObserver
- 顾客端为 Nuxt，若不想引入 Element Plus，则用 Nuxt 内置的 VueUse `useInfiniteScroll`——两者都是成熟方案，
  下一轮按"顾客端是否已引入 Element Plus"决定

### 未完成

- 商品列表的排序（后端 `listAllProducts` 目前只支持 page/size，需同样加白名单）
- 顾客端"加载更多"（含每页条数：建议 20 条/页，首屏 SSR 渲染 20 条，滚动加载后续页）
- 所有页面 loading 的逐页核对（目前主要列表已有 `v-loading`，详情弹窗部分缺失）

