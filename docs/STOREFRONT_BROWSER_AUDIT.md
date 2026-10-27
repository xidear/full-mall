# Full Mall Storefront 浏览器审计报告

- 审计对象：`web/apps/storefront`（Nuxt 3，`http://localhost:41285` / `http://127.0.0.1:41285` / `http://192.168.1.15:41285`）
- 审计方式：Playwright + 真实 Chromium，真实点击/输入/导航，断言基于 DOM 与可访问性选择器；截图与 trace 仅作证据
- 审计脚本：`web/audit/storefront-audit.spec.ts`
- 运行配置：`web/audit/playwright.audit.config.ts`（不改动 `web/playwright.config.ts`）
- 报告日期：2026-10-27
- 只读约束：未修改 storefront 源码、后端代码或数据库结构；仅新增本报告与两个审计脚本文件。结算用例会向演示库写入 1 笔演示订单（与既有 `e2e/storefront.spec.ts` 行为一致）。

---

## 一、执行摘要

本轮审计用 16 条 Playwright 用例覆盖首页、商品列表、搜索、商品详情、购物车、结算下单、订单、账户、收藏、足迹、通知、促销、领券、砍价、法律页、登录/注册、404、页脚、顶部导航、语言切换与移动端 390px。

最终结果（真实执行，未跳过任何用例）：

```
16 tests：1 passed, 15 failed, 0 skipped
耗时：4.7 分钟
进程退出码：1
```

**结论：storefront 目前无法完成一次「首页 → 列表 → 详情 → 购物车 → 结算 → 我的订单」的完整顾客旅程。**

三条 P0：

1. **商品列表 `/products` 直接 SSR 500**（`categories is not defined`）。顶部导航「全部商品」、6 个分类、首页分类磁贴、价格排序、搜索、分页、空态全部落在这一页，等于整条「找商品」链路不可用；点击后 URL 变了但页面仍停留在首页，用户感知就是「点了没反应」。
2. **法律页 `/legal/TERMS`、`/legal/PRIVACY`、`/legal/PICKUP` 全部渲染成 404**（`type=[object Object]`）。顶部「帮助中心」、导航「门店自提」、首页「查看自提规则」、结算页《服务条款》《隐私政策》都是死链——直接命中「中国地区缺合规信息」。
3. **局域网地址 `http://192.168.1.15:41285` 上，加购与结算页直接崩溃**：`crypto.randomUUID is not a function`（非安全上下文）。按钮点了没有任何提示，`/checkout` 直接变成 500 错误页。这正是用户反馈的「部分链接点击无响应」。

用户反馈的四类问题全部复现：

| 用户反馈 | 复现结果 |
|---|---|
| 应显示为链接的文本不是链接 | 页脚 ICP 备案号 / 公安备案号 / 公司主体 / 客服电话 / 邮箱全部是纯文本，页脚内 **0 个** `<a>` |
| 中国地区缺备案号/经营编号 | 备案号有文本，但无 `beian.miit.gov.cn` 链接、无公安备案链接、无营业执照/经营许可证入口 |
| 部分链接点击无响应 | `/products` 500（整类导航）、`/legal/*` 404、LAN 地址加购无反馈且结算页 500 |
| 语言切换应为下拉、切换无效 | 当前是 2 个平铺按钮；点击 English 后页面文案 0 变化、`<html lang>` 仍为 `zh-CN`、刷新后高亮回「中文」 |
| 整体 UX 差 | 错误提示直接抛技术异常（`ElMessage is not defined`、`[GET] ... 404`）、404 页无导航且为英文、移动端触控目标 22px、购物车无商品图、详情页无收藏/评价 |

### 1.1 审计快照与并行修复（必读）

- 本报告第 2–7 章描述的是 **2026-10-27 17:10 左右** 的工作区快照（git HEAD `66fa16be`，当时 `web/apps/storefront` 无本地改动）。
- 审计结束后 17:23–17:25，工作区出现另一批并行修复：`LocaleSwitcher.vue`、`useLocale.ts`、`layouts/default.vue`、`components/ProductCard.vue`、`pages/index.vue`，并新增 `error.vue`。
- 17:30 用同一脚本复跑（脚本对语言用例做了「按钮 / 下拉」双兼容加固）：

| 用例 | 17:10 首跑 | 17:30 复跑 |
|---|---|---|
| 首页 | ✅ | ✅ |
| 语言切换 | ❌ | ✅（已是下拉；文案、`html lang`、刷新持久化均通过） |
| 页脚 | ❌ | ❌（仅剩「营业执照 / 经营许可证」入口缺失；ICP、公安、`tel:`、`mailto:` 已通过） |
| 其余 13 条 | ❌ | ❌（`/legal/*` 404 等根因未改；`/products` 于 17:32 修复，见 1.2） |

- 复跑总计：**2 passed / 14 failed / 0 skipped，3.5 分钟，退出码 1**。
- 因此：**SF-04、SF-05 可视为已修复**；**SF-06 部分修复**；其余结论仍然有效。最终验收请以最新一次复跑为准。

### 1.2 17:36 抽查（第二批 P0 修复后）

第二批修复（`pages/products.vue`、`pages/legal/[type].vue`、`utils/tracking.ts`、新增 `utils/uuid.ts`）落在 17:32。17:36 用同一脚本只跑 3 条相关用例：

| 用例 | 结果 | 说明 |
|---|---|---|
| 顶部导航（全部商品） | ✅ | `/products` 已返回 200，列表页可正常打开（**SF-01 已修复**） |
| 商品列表 | ❌ | 仅剩「空态没有引导链接」1 项：`.empty-state` 内无「清空筛选 / 返回全部商品」链接 |
| 法律页 | ❌ | SSR HTML 已含「用户服务协议」，但客户端 hydration 后被替换为「页面不存在」；实测客户端请求 `/api/v1/legal/documents?type=TERMS` 返回 200（**SF-02 部分修复**，疑与 SF-16 的 SSR/CSR base 不一致同源） |
| 来源一致性 | ❌ | 局域网 `192.168.1.15` 加购已恢复（uuid 修复生效，**SF-03 已修复**）；仅 `127.0.0.1` 的 CORS 报错仍在（SF-10） |

> 上述抽查为 17:36 的瞬时状态，代码仍在并行修改中；正式验收请重跑全量。

---

## 二、测试环境与访问口径

### 2.1 环境

| 项 | 值 |
|---|---|
| 操作系统 | Linux 6.17.0-1020-oracle x86_64（Debian 12） |
| Shell | GNU bash 5.2.15(1)-release |
| 浏览器 | Playwright 1.64.1 + 本机 Chromium（`/usr/lib/chromium/chromium`，Playwright 缓存版本 1248） |
| Storefront | Nuxt dev，端口 41285 |
| API | `http://127.0.0.1:41280`（客户端实际请求 `http://192.168.1.15:41280`） |
| 数据库 | 演示数据（`fullmall-demo-postgres-1`） |
| Playwright 配置 | 根配置有 `chromium` 与 `edge`（`channel: 'msedge'`）两个 project；本机未安装 Edge，故审计只跑 `chromium` |

### 2.2 三个访问地址的实际差异（重要）

同一份代码，三个地址行为不同——这是本轮最容易误导排查的坑：

| 访问地址 | 安全上下文 | 后端 CORS 白名单 | 实测结果 |
|---|---|---|---|
| `http://localhost:41285` | 是 | 是 | 唯一可完整加购/下单的地址 |
| `http://127.0.0.1:41285` | 是 | **否**（`application.yml` 白名单只有 localhost 与 192.168.1.15） | 每次客户端请求 `/api/v1/i18n`、`/api/v1/theme`、`/api/v1/stores` 都被 CORS 拒绝 |
| `http://192.168.1.15:41285` | **否**（普通 http + 非回环 IP） | 是 | `crypto.randomUUID` 不存在 → 加购抛异常、`/checkout` 变成 500 错误页 |

证据：

```
# 127.0.0.1 作为 Origin 被后端拒绝
GET http://127.0.0.1:41280/api/v1/i18n/zh-CN  (Origin: http://127.0.0.1:41285)
→ HTTP 403 "Invalid CORS request"

# 192.168.1.15 作为 Origin 被允许
GET http://192.168.1.15:41280/api/v1/i18n/zh-CN  (Origin: http://192.168.1.15:41285)
→ HTTP 200，Access-Control-Allow-Origin: http://192.168.1.15:41285
```

因此基线用例统一跑 `http://localhost:41285`（避免把环境问题误判成 UI 缺陷），再用「来源一致性」用例单独覆盖另外两个地址。

---

## 三、设计意图（源码阅读结论）

| 模块 | 设计意图 | 代码位置 |
|---|---|---|
| 布局 | 顶部工具条（语言、订单、收藏、足迹、消息、帮助）+ 品牌 + 搜索 + 购物车 + 分类导航 + 四栏服务承诺 + 页脚合规区 | `layouts/default.vue:48-130` |
| 语言 | 文案来自 `/api/v1/i18n/{locale}`，选择持久化到 localStorage，默认 zh-CN | `composables/useLocale.ts:1-49` |
| 商品列表 | 服务端排序/筛选（分类、关键字），分页为「加载更多」，有加载失败提示与空态 | `pages/products.vue:1-136` |
| 商品详情 | 主图/规格图/图集/详情图/视频、规格切换、秒杀/买赠/拼团标签、加购、立即购买、面包屑 | `pages/product/[id].vue` |
| 购物车 | 本地 localStorage 购物车，改数量、删除、免运费、去结算 | `composables/useCart.ts`、`pages/cart.vue` |
| 结算 | 快递/自提、门店选择与定位、联系人/手机号/短信验证码、条款勾选、Mock 支付、幂等键、游客订单令牌 | `pages/checkout.vue:1-322` |
| 订单 | 订单详情、履约（提货码/物流单号）、物流轨迹、售后申请与记录 | `pages/orders/[id].vue` |
| 会员 | 收藏、足迹、消息中心、订单列表均以会员 token（cookie）识别 | `pages/account.vue`、`favorites.vue`、`history.vue`、`notifications.vue` |
| 促销 | 秒杀 / 买赠 / 拼团三类活动与拼团弹窗 | `pages/promotions.vue` |
| 合规 | 页脚应展示公司主体、ICP、公安备案、客服电话/邮箱 | `layouts/default.vue:123-124` |
| 法律 | 动态路由 `/legal/:type` 从 `/api/v1/legal/documents?type=...` 读取已发布文档 | `pages/legal/[type].vue:11-13` |

---

## 四、测试方法与脚本设计

### 4.1 方法

- **真实浏览器**：Playwright + Chromium，`page.goto` / `click` / `fill` / `selectOption` / `check`，断言 DOM 文本、可见性、属性、URL、localStorage、`<html lang>`、图片 `naturalWidth`。
- **不用截图识图代替断言**：截图只由 `only-on-failure` 与 trace 生成，作为复现证据。
- **失败必须暴露**：全部为硬断言；同页多缺陷用 `expect.soft` 聚合，测试结束仍判失败；不使用 `try/catch` 兜底，不做自动重试。
- **网络与控制台**：记录 `console.error` / `console.warning` / `pageerror` / `requestfailed` / HTTP ≥400，并按页面归因。
- **合规检查**：页脚链接用属性选择器直接检查 `href` 是否指向 `beian.miit.gov.cn`、`beian.mps.gov.cn`、`tel:`、`mailto:`。

### 4.2 脚本结构

| 用例 | 覆盖点 |
|---|---|
| 1 首页 | 标题、H1、6 个分类磁贴、商品卡、2 个主 CTA、加载期无未捕获异常 |
| 2 顶部导航 | 「全部商品」点击后必须出现可用列表页 |
| 3 商品列表 | 状态码、7 个分类筛选项、11 张商品卡、价格排序、分类筛选、关键字搜索、空态、到底提示 |
| 4 搜索 | 搜索框可访问名称、回车/点击后 URL 与结果 |
| 5 商品详情 | H1、价格、主图真实加载、规格、加购反馈、购物车角标、收藏、评价、面包屑 |
| 6 购物车 | 行项目、缩略图、＋/−、删除、空态、去逛逛 |
| 7 结算下单 | 空车提示、订单摘要、自提门店、验证码、条款、提交、订单详情、售后区、我的订单 |
| 8 促销 | 3 类活动卡、拼团弹窗开/关/校验、领券入口、砍价入口、秒杀直达结算 |
| 9 法律页 | TERMS / PRIVACY / PICKUP 可读 |
| 10 页脚 | ICP/公安/营业执照/tel/mailto/公司主体 |
| 11 语言 | 下拉形态、文案变化、`html lang`、刷新持久化 |
| 12 链接审计 | 11 个页面所有 `<a>` 的 href 有效性 + 内部链接可达性（含软 404） |
| 13 运行时质量 | 5 个关键页的控制台报错、hydration mismatch、资源 404、请求失败 |
| 14 移动端 | 390×844：整页横向溢出、触控目标 ≥44px、分类导航与折叠菜单、详情页主按钮 |
| 15 404 | 状态码、中文文案、返回入口、站点框架 |
| 16 来源一致性 | localhost / 127.0.0.1 / 192.168.1.15 三地址加购与 CORS |

### 4.3 运行方式

```bash
cd /data/full-mall/web
npx playwright test --config=audit/playwright.audit.config.ts
```

> 仓库根配置 `playwright.config.ts` 的 `testDir` 是 `./e2e`，直接执行
> `npx playwright test audit/storefront-audit.spec.ts` 会报 `No tests found`（已实测）。
> 因此新增 `audit/playwright.audit.config.ts`（`testDir: '.'`、仅 chromium），不改原配置。
> 可用环境变量覆盖地址：`STOREFRONT_BASE_URL`、`STOREFRONT_LOOPBACK_URL`、`STOREFRONT_LAN_URL`。

---

## 五、测试计划与逐项结果

> 下表「结果」以最终一次完整运行为准（`/tmp/audit-final.log`）。

### 5.1 页面级结果总览

| 页面/路径 | 设计目的 | 预期 | 实际 | 结果 |
|---|---|---|---|---|
| `/` 首页 | 分流到分类/商品/促销/自提规则 | 200，H1、6 分类、商品墙、CTA 可导航 | 结构完整，1 个 H1、6 个分类磁贴、多个商品墙；CTA 能跳转（但落到 500 列表页） | ✅ 结构 / ⚠️ 跳转目标 500 |
| `/products` 列表 | 分类/排序/搜索/分页/空态 | 200，11 件商品，筛选与排序生效 | **HTTP 500 `categories is not defined`** | ❌ P0 |
| `/products?categoryId=...` | 分类筛选 | 200，标题=分类名 | 500 | ❌ P0 |
| `/products?keyword=...` | 搜索结果 | 200，标题=搜索：关键字 | 500；客户端跳转时页面仍停在首页旧 DOM | ❌ P0 |
| `/product/:id` 详情 | 展示、规格、加购、收藏、评价 | 主图加载、规格可切、加购有反馈 | H1/价格/加购正常；3 张图片全部 404；无规格（数据仅单 SKU）；无收藏按钮；无评价区 | ❌ P1/P2 |
| `/cart` 购物车 | 改数量、删除、结算 | 行项目、缩略图、数量、小计、去结算 | 数量与删除正常；缩略图只有写死 "FM"；空态正常 | ⚠️ P2 |
| `/checkout` 结算 | 地址/自提、验证码、条款、Mock 支付 | 门店可选、验证码无报错、下单成功 | 门店与摘要正常；点「获取验证码」报 `ElMessage is not defined`；下单成功跳订单页 | ⚠️ P1 |
| `/orders/:id` 订单详情 | 状态、履约、提货码、售后 | 订单号、已付款、履约区、售后表单 | 全部正常（localhost） | ✅ |
| `/account` 我的订单 | 列出本机订单 | 至少 1 个订单卡片 | **0 个**，页面显示 `[GET] .../orders/...: 404` | ❌ P1 |
| `/favorites` 收藏 | 会员收藏列表 | 列表或「暂无收藏 + 去收藏」引导 | 仅静态提示，商品详情页没有收藏入口 | ❌ P2 |
| `/history` 足迹 | 浏览记录 | 浏览商品后出现记录 | 「还没有浏览记录」 | ⚠️ P3 |
| `/notifications` 消息 | 未读/已读 | 未读数、列表、标记已读 | 42 条未读正常展示，标记已读可用 | ✅ |
| `/promotions` 促销 | 秒杀/买赠/拼团 | 3 类卡片 + 弹窗 | 3 张卡、弹窗可开关、空提交有提示；无领券、无砍价入口 | ⚠️ P2 |
| `/legal/TERMS|PRIVACY|PICKUP` | 合规文档 | 文档标题与正文 | 全部渲染 404（标题 `404 - 法律文档不存在`） | ❌ P0 |
| `/bargain/:id` 砍价 | 发起/帮砍 | 活动信息与按钮 | 页面存在但全站无入口；无活动数据时只显示错误文案 | ⚠️ P2 |
| 登录/注册 | 会员身份入口 | 顶部有登录/注册 | **全站没有登录页、没有登录入口** | ❌ P2 |
| `/nope-404` | 404 引导 | 中文、保留导航、返回首页 | 404 正确；文案英文；无 header/footer | ❌ P2 |
| 页脚 | 合规与客服 | ICP/公安/执照/电话/邮箱可点 | 页脚 **0 个链接**，全部纯文本 | ❌ P1 |
| 语言切换 | 多语言 | 下拉、切换生效、刷新保持 | 平铺 2 按钮；文案 0 变化；lang 仍 zh-CN；刷新回中文 | ❌ P1 |

### 5.2 关键交互实测记录

1. **首页「立即选购」**：URL 变 `/products`，页面渲染 500（`categories is not defined`）。
2. **顶部「全部商品」**：URL 变 `/products`，`getByRole('heading', {name:'全部商品'})` 找不到——页面是 500。
3. **搜索「牛奶」**：URL 变 `/products?keyword=%E7%89%9B%E5%A5%B6`，H1 仍是「好物直达，门店自提更快」，商品卡仍是首页推荐——用户看到的是「点了搜索，页面没变」。
4. **商品详情加购（localhost）**：出现「已加入购物车」，购物车角标变 1，localStorage 写入成功。
5. **商品详情加购（192.168.1.15）**：无任何提示，购物车为空，`pageerror: crypto.randomUUID is not a function`。
6. **购物车**：＋ → 2，− → 1，删除 → 「购物车还是空的」+「去逛逛」，全部正常。
7. **结算**：订单摘要显示「有机鲜牛奶 × 1 ¥19.90」；点「获取验证码」后红色错误 `ElMessage is not defined`，但验证码仍被自动填入；勾选条款并提交后跳转 `/orders/<uuid>`，订单页 H1「订单 FM2026...」、状态「已付款」、履约与售后区均正常。
8. **我的订单**：下单后立刻访问 `/account`，页面显示 `[GET] "http://192.168.1.15:41280/api/v1/orders/<id>": 404` 且订单列表为空。
9. **语言**：点 English 后 `localStorage['fullmall-locale']='en'`，但页面文案逐字不变、`<html lang="zh-CN">`、刷新后高亮回到「中文」。
10. **链接审计**：未发现 `href="#"`、`javascript:`、空 href；但 8 个内部链接返回 500（全部是 `/products` 及其 query 变体）。
11. **运行时**：首页 hydration mismatch 5 条；促销页 4 条；商品详情 3 张图 404。
12. **移动端 390px**：整页无横向滚动（`scrollWidth=390`），但分类导航内容宽 698px / 可视 350px，只能横向滑动且无折叠菜单；导航链接触控高度 22px、顶部链接 38px、语言按钮宽 30px。
13. **404**：HTTP 404 正确，返回链接 `href="/"` 正确，但文案 `Page not found: /nope-404` 为英文，且页面没有 `site-header`/`site-footer`。
14. **来源一致性**：localhost 通过；127.0.0.1 出现 CORS 报错；192.168.1.15 加购失败（详见 §2.2）。

---
## 六、缺陷清单

> 严重级别：P0 阻断主流程 / P1 核心功能或合规缺失 / P2 明显体验问题 / P3 打磨项。

### P0

#### SF-01 商品列表 `/products` SSR 500，整条找商品链路不可用

- **页面/路径**：`/products`、`/products?categoryId=*`、`/products?keyword=*`（顶部导航、首页 6 个分类磁贴、首页「立即选购 / 查看全部 / 更多商品 / 工业品专区」、搜索框全部指向这里）
- **复现**：打开 `http://localhost:41285/products`，或首页点「全部商品」/ 搜索「牛奶」
- **预期**：200，H1「全部商品」或「搜索：牛奶」，商品网格、筛选、排序、分页可用
- **实际**：HTTP 500，页面正文 `500 categories is not defined`；客户端跳转时 URL 变了但 DOM 仍是首页（用户感知「点击无响应」）
- **根因线索**：`web/apps/storefront/pages/products.vue:82` 的 `activeCategory` 使用 `categories.value`，但该文件从未声明/获取 `categories`（`useAsyncData('categories')` 只写在 `layouts/default.vue:31`，不会自动成为页面作用域变量）
- **建议修复**：把分类提取为共享 composable（如 `useCategories()`，内部 `useAsyncData('categories', ...)` 并返回 `data`），在 products.vue 中 `const { data: categories } = await useCategories()`；或直接 `const { data: categories } = await useAsyncData('categories', () => $fetch(...))`。修复后补一条 `/products` 返回 200 的 e2e。

#### SF-02 法律页全部 404，合规文档不可达

- **页面/路径**：`/legal/TERMS`、`/legal/PRIVACY`、`/legal/PICKUP`（入口：顶部「帮助中心」、导航「门店自提」、首页「查看自提规则」、结算页《服务条款》《隐私政策》）
- **复现**：点击任一入口，或直接访问路径
- **预期**：显示《用户服务协议》《隐私政策》《门店自提规则》正文
- **实际**：页面标题 `404 - 法律文档不存在 | Nuxt`，H1 为 `404`；接口 `GET /api/v1/legal/documents?type=TERMS` 本身返回 200
- **根因线索**：`web/apps/storefront/pages/legal/[type].vue:10-13` 把 `computed` 的 `type` 直接塞进 `useFetch` 的 `query`，客户端请求被序列化成 `type=[object Object]`；实测该参数返回 404 `LEGAL_DOCUMENT_NOT_FOUND`
- **建议修复**：`const type = String(route.params.type || 'TERMS')`（非响应式），或 `query: { type: type.value }`；并为未知类型提供中文 404 页。修复后断言三条路径的状态码与 H1。

#### SF-03 局域网地址（192.168.1.15）加购无反应、结算页 500

- **页面/路径**：`http://192.168.1.15:41285/product/:id`、`/checkout`（文档中给同事/客户的访问地址）
- **复现**：用 LAN 地址打开商品详情 → 点「加入购物车」；再打开 `/checkout`
- **预期**：提示「已加入购物车」，角标 +1；结算页正常渲染
- **实际**：按钮无任何反馈；控制台 `pageerror: crypto.randomUUID is not a function`；`/checkout` 返回 200 但内容为 `500 crypto.randomUUID is not a function`
- **根因线索**：`web/apps/storefront/utils/tracking.ts:21` 与 `pages/checkout.vue:31` 直接调用 `crypto.randomUUID()`；该 API 仅在安全上下文（https / localhost）可用，LAN 的 http 源没有
- **建议修复**：抽出 `utils/randomId.ts`（优先 `crypto.randomUUID`，否则用 `crypto.getRandomValues` 拼 UUID v4，再退化到时间戳+随机数）；埋点失败只 warn，**不能打断业务加购**（当前 track 抛异常把 add 整条链路打断）。

### P1

#### SF-04 语言切换完全无效（文案、`html lang`、刷新持久化）

- **页面/路径**：全站顶部语言切换
- **复现**：首页点「English」→ 观察文案与 `<html lang>` → 刷新
- **预期**：导航/按钮/页脚切换为英文，`<html lang="en">`，刷新后保持英文
- **实际**：文案逐字不变（H1、导航、购物车、页脚仍是中文）；`lang` 仍 `zh-CN`；刷新后高亮回「中文」；仅 `localStorage['fullmall-locale']='en'` 被写入
- **根因线索**：`composables/useLocale.ts:34` 的 `t()` 在全站 **0 处调用**——所有文案硬编码中文；`nuxt.config.ts:17` 固定 `htmlAttrs.lang='zh-CN'`；`LocaleSwitcher.vue:14` 只改 `locale` 状态，SSR 不读 localStorage；且 `locale` 变化不会触发 `useFetch` 重新取文案（index.vue:7-9、products.vue:18-32 未 watch locale）
- **建议修复**：
  1. 用 `useCookie('fullmall-locale')` 代替 localStorage，SSR 可直接读到；
  2. `useHead({ htmlAttrs: { lang: computed(() => locale.value === 'en' ? 'en' : 'zh-CN') } })`；
  3. 导航、购物车、页脚、按钮等公共文案改用 `t('nav.cart')` 等 key（后端已有 `nav.cart / product.addToCart / checkout.pay` 等 10 个 key）；
  4. `watch(locale, () => refreshNuxtData())` 或让各页 `useFetch` 依赖 locale。

#### SF-05 语言切换是平铺按钮而非下拉菜单

- **页面/路径**：顶栏语言区
- **复现**：任意页面看右上角
- **预期**：一个语言下拉（或带 `aria-haspopup` 的菜单按钮），语言增多不撑爆顶栏
- **实际**：`<div class="locale-switcher">` 内两个 `<button>`（中文 / English）直接铺开；`select`、`role=combobox`、`aria-haspopup` 数量均为 0
- **根因线索**：`components/LocaleSwitcher.vue:13-18`
- **建议修复**：改为 `<select>`（最省事、移动端原生可用）或自制 `button[aria-haspopup="listbox"]` + `ul[role="listbox"]`；保留当前语言高亮与键盘操作（Esc 关闭、方向键选择）。

#### SF-06 页脚合规信息全部是纯文本，没有任何链接

- **页面/路径**：全站页脚
- **复现**：打开首页看页脚第二行
- **预期**：
  - `京ICP备00000000号-1` → `<a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener">`
  - `京公网安备 00000000000000号` → 公安备案查询链接（`https://beian.mps.gov.cn/#/query/webSearch?code=...`）
  - 营业执照 / 经营许可证（ICP/EDI）入口
  - 客服电话 → `tel:400-000-0000`；客服邮箱 → `mailto:support@example.com`
- **实际**：`layouts/default.vue:123-124` 用两个 `<span>` 拼接；页脚内 `<a>` 数量 = **0**
- **根因线索**：`web/apps/storefront/layouts/default.vue:122-129`
- **建议修复**：改为语义化列表 + 链接；备案号、公安备案、许可证、电话、邮箱都从 `siteSettings` 读取并在有值时渲染链接；无值时不渲染空占位。

#### SF-07 下单后「我的订单」永远空，且显示原始 404 报错

- **页面/路径**：`/account`
- **复现**：localhost 完成一次下单 → 点顶部「我的订单」
- **预期**：列出刚下单的订单卡片（订单号、状态、时间、金额）
- **实际**：0 个订单，页面显示 `[GET] "http://192.168.1.15:41280/api/v1/orders/<id>": 404`，随后显示「还没有订单」
- **根因线索**：`pages/account.vue:18-23` 的 `$fetch(.../orders/<id>)` **没有带 Authorization 或 X-Order-Token**；订单接口按归属校验，访客订单返回 404
- **建议修复**：复用 `orders/[id].vue` 的鉴权头构造（`useCookie('fullmall_member_token')` + `localStorage['fullmall.orderToken.<id>']`），或改用带会员 token 的订单列表接口；错误信息应转成「订单加载失败，请重新登录」而非原始 ofetch 文案。

#### SF-08 结算页「获取验证码」抛 `ElMessage is not defined`

- **页面/路径**：`/checkout`
- **复现**：加购 → 结算 → 点「获取验证码」
- **预期**：提示「验证码已发送」，无红色错误
- **实际**：出现红色 `ElMessage is not defined`（验证码其实已自动填入）
- **根因线索**：`pages/checkout.vue:101` 调用了未安装/未引入的 Element Plus `ElMessage`（`package.json` 无 element-plus，也没有 Nuxt 模块）
- **建议修复**：换成项目自有 toast（新增 `useToast`）或 `useState` 驱动的行内提示；若确实要 Element Plus，则安装并按 Nuxt 模块方式引入。

#### SF-09 商品详情主图、图集、详情图全部 404

- **页面/路径**：`/product/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa`
- **复现**：打开任意带媒体资源的商品
- **预期**：主图、缩略图、详情长图正常显示
- **实际**：`<img> naturalWidth=0`，控制台 3 条 404：`/assets/assets/202610/*.png`
- **根因线索**：接口 `/api/v1/products/{id}/media` 返回的 URL 已是 `/assets/assets/202610/...`（双 `assets`），且 storefront（41285）没有把 `/assets/**` 代理到后端/对象存储；后端 41280 同路径也 404（文件不存在）
- **建议修复**：统一媒体 URL 约定（后端返回绝对 URL 或 `/media/...`）+ 在 `nuxt.config.ts` 增加 `routeRules: { '/assets/**': { proxy: ... } }`；修正 seed 数据的重复前缀；补 `onerror` 占位图，避免大白块。

#### SF-10 用 127.0.0.1 访问时所有客户端 API 被 CORS 拒绝

- **页面/路径**：`http://127.0.0.1:41285` 全站
- **复现**：用 127.0.0.1 打开首页/结算
- **预期**：与 localhost 行为一致（同源或白名单内）
- **实际**：`/api/v1/i18n/zh-CN`、`/api/v1/theme`、`/api/v1/stores` 全部 `blocked by CORS policy`；`/checkout` 出现 `Failed to fetch`
- **根因线索**：`server/src/main/resources/application.yml:80` 的 `app.cors.allowed-origins` 只列了 `localhost:*` 与 `192.168.1.15:*`，没有 `127.0.0.1:*`；而 storefront 客户端使用绝对 `apiBase`（`nuxt.config.ts:9`），并非注释里说的「同源代理」
- **建议修复**：短期把 `http://127.0.0.1:41285` 加入白名单；长期改为同源代理（`NUXT_PUBLIC_API_BASE=/api` + Nitro `routeRules` proxy 到 41280），彻底消除 CORS 与「三地址行为不一致」。

### P2

#### SF-11 商品详情没有「收藏」按钮，收藏页却让用户去详情页收藏

- **复现**：详情页找收藏 → 无；`/favorites` 提示「去商品页点「收藏」试试」
- **实际**：详情页 `getByRole('button', {name:/收藏/})` = 0
- **根因**：`pages/product/[id].vue` 未实现；后端 `/api/v1/favorites` 已存在
- **建议**：详情页加收藏/取消收藏（登录态校验 + 未登录引导），收藏页加空态 CTA。

#### SF-12 商品详情没有评价区

- **复现**：详情页查找「评价」标题 → 无
- **实际**：后端 `GET /api/v1/products/{skuId}/reviews` 已实现（`ProductReviewController.java:29`），前端从未调用
- **建议**：详情页增加评分摘要 + 评价列表 + 分页；下单后可提交评价。

#### SF-13 没有领券入口（促销页只有秒杀/买赠/拼团）

- **复现**：`/promotions` 查找「领券/优惠券」链接 → 0
- **说明**：后端目前只有 POS 券（`AdminPosController`），顾客端领券接口尚缺；属于产品能力缺口，需与产品确认后再补
- **建议**：明确「顾客券中心」接口与页面，促销页加「领券」区块；若暂不支持，至少在促销页说明「暂无优惠券」。

#### SF-14 砍价页存在但全站无入口，且无活动数据时只有一行错误

- **复现**：全站查找 `a[href*="/bargain"]` → 0；直接访问 `/bargain/<id>` 需自备活动 ID
- **建议**：促销页增加砍价专区入口；提供活动列表接口/页面；无数据时给空态而不是「活动加载失败」。

#### SF-15 没有登录/注册入口，会员态只能靠结算时 OTP 获取

- **复现**：顶栏只有订单/收藏/足迹/消息，没有「登录/注册」
- **影响**：收藏、足迹、消息都提示「登录后」，但用户找不到登录入口；`fullmall.memberToken` 只有下单时才写入
- **建议**：增加 `/login`（手机号 + OTP，复用 `/api/v1/member/auth/*`），顶栏显示「登录 / 你好，会员」；收藏/足迹/消息未登录时给登录 CTA。

#### SF-16 首页/促销页 hydration mismatch + 重复请求

- **实际**：首页 5 条 hydration 警告（`Server rendered element contains more child nodes than client vdom`，归属 `<Index>`），促销页 4 条；同一页面客户端又请求一次 `/api/v1/products`
- **根因线索**：`layouts/default.vue:11` 与各页 `const base = import.meta.server ? apiInternalBase : public.apiBase` 让 SSR 与客户端 **useFetch key 不一致**，payload 无法复用 → 客户端重新请求 → hydration 时数据为空 → DOM 不一致
- **建议**：统一用相对路径（同源代理）或给 `useFetch/useAsyncData` 显式稳定 `key`（如 `key: 'home-products'`），不要在 URL 里混入仅服务端可见的 base。

#### SF-17 移动端触控目标过小（22–40px）

- **实际**：分类导航链接 56×22、顶栏导航 33×38、搜索框/按钮 40px、语言按钮宽 30px；均 < 44×44
- **根因线索**：`assets/main.css:884-900`（`.category-nav` 链接无最小高度）、`.topbar` 未设置触控内边距
- **建议**：移动端给导航链接 `min-height:44px; display:inline-flex; align-items:center; padding:0 12px`；语言菜单按钮 ≥44。

#### SF-18 移动端分类导航 8 项横向滚动，无折叠菜单

- **实际**：`.category-nav` `scrollWidth=698` / `clientWidth=350`，无 hamburger；首屏看不到「促销活动」「门店自提」
- **建议**：≤768px 改为汉堡菜单 / 横向可滑动 + 渐隐提示；或把分类收进「全部分类」抽屉。

#### SF-19 错误页脱离站点框架、英文文案、软 404

- **实际**：`/nope-404` 与 `/legal/TERMS` 的 404 页没有 `site-header/site-footer`，文案 `Page not found` / `Go back home`；`/legal/TERMS` 的 HTTP 状态是 **200**（软 404）
- **建议**：新增 `app/error.vue`（中文文案、保留 header/footer、搜索框、返回首页/全部商品），并在 `createError` 时保证状态码。

#### SF-20 错误提示直接暴露技术细节

- **实际**：`ElMessage is not defined`、`[GET] "http://192.168.1.15:41280/api/v1/orders/...": 404`
- **建议**：统一错误映射（网络/鉴权/校验/系统），面向顾客的文案 + 可展开的「详情」给开发。

#### SF-21 购物车商品缩略图写死 "FM"

- **根因**：`pages/cart.vue:18` `<div class="cart-thumb">FM</div>`
- **建议**：存 `imageUrl` 或用 SKU 图；无图时用首字占位但保留图片位。

#### SF-22 商品卡「已售 xxx+」是伪造数据

- **根因**：`components/ProductCard.vue:9` 按标题字符码求和生成 100–999
- **建议**：接真实销量字段；没有就隐藏，避免误导。

#### SF-23 全站页面标题都是 "Full Mall"

- **根因**：`nuxt.config.ts:16` 只设了全局 title，各页无 `useHead`
- **建议**：每页设置 `title`（商品名/分类名/订单号），列表页加 `description`。

#### SF-24 页脚服务条与导航重复且不可点击

- **实际**：页脚「正品保障 / 极速退款 / 门店自提 / 七天无理由」四块是 `div`，与导航「门店自提」重复
- **建议**：改为链接（分别指向售后政策/自提规则/服务承诺），或合并去重。

#### SF-25 列表侧栏「服务」筛选项是纯文本

- **根因**：`pages/products.vue:102-107` 三个 `<span class="filter-note">`
- **建议**：若支持筛选则做成 checkbox/链接，否则从「筛选」区移到说明区。

#### SF-26 搜索框没有 label

- **实际**：只有 `placeholder`，无 `aria-label`/可见 label（Playwright 通过 placeholder 兜底算出名称）
- **建议**：加 `<label for="site-search" class="sr-only">搜索商品</label>`。

### P3

| 编号 | 问题 | 位置 | 建议 |
|---|---|---|---|
| SF-27 | `useAsyncData` 返回 `null` 触发 Nuxt 警告（theme 配置不存在时） | `layouts/default.vue:13-18` | 用 `{ config: null }` 包一层或 `default: () => null` |
| SF-28 | 演示数据没有多规格商品，「规格切换」无法验证 | 数据 | 补一个 2–3 规格的演示 SKU |
| SF-29 | 结算按钮文案「Mock 支付并下单」暴露技术术语 | `pages/checkout.vue:292` | 改为「提交订单（演示支付）」 |
| SF-30 | 演示结果卡片写死「门店自提 / 提货码 654321」，与用户选择的配送方式无关 | `checkout.vue:295-299` | 用 mock 返回值渲染 |
| SF-31 | 商品详情 CSS 出现重复嵌套的 `@media (max-width:640px)` | `pages/product/[id].vue:227-234` | 合并为一个媒体查询 |
| SF-32 | 顶部「我的订单」与账户页 H1 重复；账户页没有收藏/足迹/消息入口 | `layouts/default.vue:55`、`account.vue` | 账户页做成「我的」聚合页 |
| SF-33 | 浏览足迹为空（访问商品后仍无记录） | `history.vue` | 确认后端是否在商品详情写入足迹；未登录时给登录引导 |
| SF-34 | 条款链接 `target="_blank"` 未加 `rel="noopener"` | `checkout.vue:285-287` | 补 `rel` |

---

## 七、修复方案建议（按优先级）

### 第 1 批（P0，半天内可完成）

1. `pages/products.vue` 补 `categories` 数据源（见 SF-01），并加回归用例：`GET /products → 200`。
2. `pages/legal/[type].vue` 把 `type` 改为普通字符串（见 SF-02），加回归：三条法律路径 200 + H1 正确。
3. 抽出 `utils/randomId.ts`，替换 `tracking.ts:21` 与 `checkout.vue:31` 的 `crypto.randomUUID()`；加回归：在非安全上下文（`http://<LAN-IP>`）加购成功。

### 第 2 批（P1，1–2 天）

4. 语言系统：cookie 持久化 + `useHead(htmlAttrs.lang)` + 公共文案接 `t()` + `LocaleSwitcher` 改下拉；加回归：切换后 H1/导航变化、`lang=en`、刷新保持。
5. 页脚合规区：ICP/公安/执照/客服电话/邮箱全部链接化，数据来自 `siteSettings`（建议后端补 `business_license_url`、`icp_license` 字段）。
6. `account.vue` 带鉴权头；订单列表改用会员订单接口；错误文案本地化。
7. `checkout.vue` 的 `ElMessage` 换成项目内 toast。
8. 媒体资源：统一 URL + `/assets` 代理 + 修正 seed 重复前缀。
9. 统一 API 访问口径（同源代理），或至少把 `127.0.0.1` 加入 CORS 白名单。

### 第 3 批（P2，按迭代排期）

10. 详情页收藏按钮 + 评价区（后端接口已就绪）。
11. 登录/注册页与顶栏入口。
12. 移动端导航：汉堡菜单 + 44px 触控目标。
13. `app/error.vue` 中文错误页。
14. 促销页领券/砍价入口（需产品确认接口）。
15. 统一错误提示与空态引导（每个空态给一个主 CTA）。

### 合规清单（中国市场，需法务确认）

- [ ] ICP 备案号，链接 `https://beian.miit.gov.cn/`
- [ ] 公安备案号，链接 `https://beian.mps.gov.cn/`
- [ ] 营业执照 / 增值电信业务经营许可证（ICP/EDI）公示入口
- [ ] 公司主体名称（与营业执照一致）、注册地址
- [ ] 客服电话（`tel:`）、客服邮箱（`mailto:`）、服务时间
- [ ] 用户协议、隐私政策、退换货规则、门店自提规则（当前法律页 404）
- [ ] 食品经营许可证（生鲜品类，如适用）
- [ ] 个人信息收集清单 / 第三方共享清单（隐私政策内）

---

## 八、原始输出摘要

### 8.1 执行命令

```bash
cd /data/full-mall/web
npx playwright test --config=audit/playwright.audit.config.ts
```

### 8.2 结果统计（真实输出）

```
✓   1 首页：首屏结构、分类入口、商品卡与主 CTA (1.8s)
✘   2 顶部导航：分类与“全部商品”必须打开可用的商品列表 (8.0s)
✘   3 商品列表：筛选、排序、搜索、分页、空态 (7.7s)
✘   4 搜索：输入关键字后必须给出可访问的搜索结果 (14.2s)
✘   5 商品详情：主图、规格、加购、收藏、评价 (14.1s)
✘   6 购物车：改数量、删除、结算入口 (8.2s)
✘   7 结算与下单：地址、验证码、条款、Mock 支付、订单详情、订单列表 (19.5s)
✘   8 促销：秒杀买赠拼团可交互，并提供领券与砍价入口 (14.1s)
✘   9 法律页：TERMS / PRIVACY / PICKUP 必须可读 (57.5s)
✘  10 页脚：ICP/公安备案/营业执照/客服电话与邮箱必须可点击 (32.0s)
✘  11 语言切换：应为下拉菜单，且影响文案、html lang 与刷新后持久化 (40.2s)
✘  12 链接审计：无空 href / # / javascript:，内部链接均可打开 (13.0s)
✘  13 运行时质量：控制台无报错、无 hydration mismatch、静态资源 200 (10.2s)
✘  14 移动端 390px 视口 › 移动端：无横向溢出、触控目标 >=44px、导航可用 (2.6s)
✘  15 404 页：状态码、中文文案、返回入口与站点框架 (13.7s)
✘  16 来源一致性：三个访问地址都应能加购且无 CORS 报错 (9.5s)

15 failed
1 passed (4.7m)
exit code: 1
```

### 8.3 关键断言失败原文（节选）

```
Error: /products 应返回 200（实际 SSR 500）
  Expected: 200
  Received: 500

Error: expect(locator).not.toContainText(expected) failed
  Locator: locator('body')
  Expected substring: not "categories is not defined"
  Received string: "500categories is not defined"

Error: expect(page).not.toHaveTitle(expected) failed
  Expected pattern: not /404/
  Received string: "404 - 法律文档不存在 | Nuxt"

Error: ICP 备案号应链接到 beian.miit.gov.cn
  Expected: 1  Received: 0

Error: 页脚应包含合规与客服链接
  Expected: >= 4  Received: 0

Error: 切换英文后页面文案应变化
Error: expect(locator).toHaveAttribute(expected) failed
  Locator: locator('html')  Expected: "en"  Received: "zh-CN"
Error: expect(locator).toHaveText(expected) failed
  Locator: locator('.locale-switcher .active')  Expected: "English"  Received: "中文"

Error: 主要导航/操作触控目标应 >= 44x44
Error: 移动端分类导航需横向滚动（698px / 可视 350px）且无折叠菜单

Error: 中文站点 404 应为中文文案
  Expected pattern: not /Page not found/
  Received string: "404 - Page not found: /nope-404 | Nuxt"
Error: 404 页应保留站点导航（.site-header 未找到）

Error: http://127.0.0.1:41285 不应出现 CORS / 安全上下文错误
Error: http://192.168.1.15:41285 加购应有成功提示（.success-text 未找到）
       http://192.168.1.15:41285 加购后应写入购物车  Received: null
```

链接审计实际返回：

```
"/products → HTTP 500"
"/products?categoryId=99999999-...-995 → HTTP 500"
...（共 6 个分类）
"/products?keyword=%E5%B7%A5%E4%B8%9A → HTTP 500"
```

运行时质量实际返回：

```
"/ console.error: Hydration completed but contains mismatches."
"/ hydration: 5 条"
"/product/... console.error: Failed to load resource: 404 /assets/assets/202610/88259f2d....png（共 3 张）"
"/promotions console.error: Hydration completed but contains mismatches."
"/promotions hydration: 4 条"
```

### 8.4 证据文件

- 失败截图与 trace：`web/test-results/storefront-audit-*/test-failed-*.png`、`trace.zip`
- 完整运行日志：`/tmp/audit-final.log`（本次运行）
- 脚本：`web/audit/storefront-audit.spec.ts`、`web/audit/playwright.audit.config.ts`

### 8.5 审计后复跑（2026-10-27 17:30，并行修复后）

命令同上。结果：

```
✓   1 首页：首屏结构、分类入口、商品卡与主 CTA (1.8s)
✓  11 语言切换：应为下拉菜单，且影响文案、html lang 与刷新后持久化 (5.2s)
✘  其余 14 条

2 passed (3.5m)
exit code: 1
```

主要变化：

- 语言切换已从平铺按钮改为 `<select>` 下拉；切换后文案、`html lang="en"`、刷新持久化均通过（SF-04 / SF-05 已修复）。
- 页脚新增 ICP、公安备案、客服 `tel:` / `mailto:` 链接；「营业执照 / 经营许可证」入口仍缺失（SF-06 部分修复）。
- `/products` 仍 500、`/legal/*` 仍 404、商品图片仍 404、`/account` 仍 404，其余结论不变。

> 复跑前对语言用例做了兼容加固（先判断是否存在 `<select>`，再决定 `selectOption` 还是点击按钮），
> 其余断言未改动；因此两轮结果可直接对比。

---

## 九、覆盖范围与限制

**已覆盖并真实执行**：首页、商品列表（受 500 阻断，仅验证状态与报错）、搜索、商品详情、购物车、结算/下单、订单详情、账户订单列表、收藏、足迹、通知、促销、法律页、404、页脚、顶部导航、语言切换、移动端 390px、三地址一致性、链接与运行时质量。

**未覆盖 / 受限**：

1. **多规格切换**：演示数据 11 个商品均为单 SKU，规格按钮不渲染（已在脚本中记录为失败断言）。
2. **领券/评价/收藏交互**：顾客端没有对应 UI（部分后端接口已存在），只能验证「入口缺失」。
3. **Edge 项目**：本机未安装 msedge，`web/playwright.config.ts` 的 edge project 未执行；审计配置只跑 chromium。
4. **真实支付/物流**：demo 环境使用 Mock 支付；物流轨迹需已发货订单。
5. **副作用**：结算用例每次向演示库写入 1 笔演示订单（首跑 + 复跑共 2 笔，另有探索期 1 笔），不影响源码与表结构。
6. **127.0.0.1 与 LAN 的差异**属环境/配置问题，脚本中单列「来源一致性」用例，不计入基线 UI 判定。

---

## 十、复跑与验收建议

1. 修复 P0 后先跑：
   ```bash
   cd web && npx playwright test --config=audit/playwright.audit.config.ts --grep "商品列表|法律页|来源一致性"
   ```
2. 再跑全量，目标：16 条全部通过；若保留「演示数据无多规格」断言失败，可把该断言改为数据前置条件（seed 增加多规格商品）。
3. 建议把本审计脚本纳入 CI 的 smoke 阶段（只跑 chromium），并在合并前要求 P0/P1 为零。

---

## 十一、修复后最终回归（2026-10-27 18:50）

执行：

```bash
cd web
npx playwright test --config=audit/playwright.audit.config.ts --reporter=line
```

结果：**16 passed / 0 failed / 0 skipped，1.1 分钟，exit 0**。

修复映射与逐项说明见 [STOREFRONT_FIX_SUMMARY.md](STOREFRONT_FIX_SUMMARY.md)。
本轮之后，P0/P1 全部关闭；多规格切换已能渲染单规格 chip，收藏/评价/领券/砍价入口已补齐，
媒体资源由后端 `/assets/**` + Nitro 代理提供，语言切换为 cookie 驱动并真正改变可见文案。
