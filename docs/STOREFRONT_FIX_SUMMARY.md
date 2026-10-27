# Storefront 修复方案总结（2026-10-27）

## 1. 方法与证据

- 测试计划/逐项结果/原始输出：[STOREFRONT_BROWSER_AUDIT.md](STOREFRONT_BROWSER_AUDIT.md)（真实 Chromium + Playwright，DOM/可访问性断言，截图仅作证据）。
- 审计脚本：`web/audit/storefront-audit.spec.ts`；配置：`web/audit/playwright.audit.config.ts`。
- 最终回归命令：

```bash
cd web
npx playwright test --config=audit/playwright.audit.config.ts --reporter=line
# 16 passed (1.1m), exit 0
```

## 2. 修复映射

| 缺陷 | 根因 | 修复 | 验证 |
|---|---|---|---|
| SF-01 /products SSR 500 | `products.vue` 使用未声明的 `categories` | 增加 `useAsyncData('products-categories')`；修复 `useHead` 在 `activeCategory` 之前求值的 TDZ | 列表 200、分类/排序/搜索/分页通过 |
| SF-02 法律页 404 / 客户端变 404 | `computed type` 直接进 `useFetch query` → `[object Object]`；SSR/CSR key 不一致 | query 用 `type.value`，显式 `key: legal-${type}`，pending 时不抛 404 | TERMS/PRIVACY/PICKUP 可读，状态码正确 |
| SF-03 LAN 加购/结算崩溃 | `crypto.randomUUID` 非安全上下文不可用 | 新增 `utils/uuid.ts`（`getRandomValues` 回退），tracking/checkout 统一使用 | 192.168.1.15 加购/结算通过 |
| SF-04 语言切换无效 | 全站没有调用 `t()`，SSR 不读 localStorage | `useLocale` 改 cookie（SSR/CSR 一致）+ `t()` 覆盖导航/首页/商品卡/页脚；切换后整页重载 | English 文案、`html lang`、刷新持久化通过 |
| SF-05 语言按钮平铺 | 逐语言渲染 `button` | 改为原生 `<select>` 下拉，按 `options` 扩展 | 下拉形态通过 |
| SF-06 页脚合规信息不可点 | 页脚纯文本 | ICP→beian.miit.gov.cn、公安备案→beian.mps.gov.cn、公司主体/客服电话 tel:/邮箱 mailto:、营业执照/经营编号→gsxt.gov.cn | 页脚链接断言通过 |
| SF-07 我的订单为空 + 原始 404 | 订单请求未带会员/访客 token | account.vue 逐单构造 `Authorization` + `X-Order-Token`，`allSettled` 聚合与友好错误 | 下单后订单列表/详情通过 |
| SF-08 ElMessage 未定义 | 结算页调用未安装的 Element Plus | 改用行内 notice 状态与 5s 自动消失 | 获取验证码无异常 |
| SF-09 商品媒体 404 | 后端没有静态资源映射；媒体 URL 双 `assets` 前缀 | 新增 `StaticAssetConfig`（`/assets/**` → `app.asset.local.directory`）；Nitro `/assets/**` 代理；图片 `onerror` 占位 | 媒体 200 |
| SF-10 127.0.0.1 CORS 拒绝 | 后端白名单缺 127.0.0.1 | 加入 127.0.0.1:41281–41286 | 三地址来源一致性通过 |
| SF-11 详情页无收藏 | 前端未实现 | 详情页收藏/取消收藏（会员 token；未登录提示） | 收藏按钮通过 |
| SF-12 详情页无评价 | 前端未调用公开评价接口 | 增加评价区（评分、内容、日期、空态） | 评价标题通过 |
| SF-13/14 无领券/砍价入口 | 促销页缺入口 | 增加「领券中心」「砍价活动」入口与可访问落地页（无数据时空态，不 404） | 促销/链接审计通过 |
| SF-15 无登录入口 | 只有结算 OTP，无登录页 | 新增 `/login`（手机号 + OTP）、`useMember` cookie/localStorage 双写、顶栏登录/会员态 | 登录页/会员态可访问 |
| SF-16 hydration mismatch | SSR/CSR base 不同导致 payload key 不一致；购物车/会员/订单在 setup 读 localStorage | 显式稳定 key；localStorage 读取移到 `onMounted`；promotions 固定 key | 运行时质量通过 |
| SF-17/18 移动端触控/导航 | 导航 22–40px、8 项横滚无折叠 | 移动端 44px 触控目标；新增「☰ 全部分类」折叠菜单 | 390px 移动端通过 |
| SF-19/20 错误页技术文案 | 无 error.vue / 原始 ofetch 错误 | 新增中文 `error.vue`（保留 header/footer、404 标题、返回入口）；订单/收藏错误友好化 | 404 测试通过 |
| SF-21 购物车缩略图写死 FM | 购物车未存图 | CartLine 增加 `imageUrl`，加购写入，缩略图渲染 + 首字占位 | 购物车通过 |
| SF-22 伪造销量 | 用标题字符码生成 | 移除假销量 | 通过 |
| SF-23 全站 title 相同 | 只有全局 title | 首页/列表/详情/购物车/结算/法律/登录/404 设置 `useHead` | 页面 title 正确 |
| SF-24/25 服务条/筛选纯文本 | 使用 div/span | 页脚服务条、列表侧栏服务项改为可点链接 | 链接审计通过 |
| SF-26 搜索无 label | 只有 placeholder | 增加 `label for=site-search` + aria-label | 可访问名称通过 |

## 3. 修改文件

- 布局/组件：`layouts/default.vue`、`components/LocaleSwitcher.vue`、`components/ProductCard.vue`
- 页面：`pages/index.vue`、`products.vue`、`product/[id].vue`、`cart.vue`、`checkout.vue`、`account.vue`、`legal/[type].vue`、`promotions.vue`、`login.vue`、`coupons.vue`、`bargain/index.vue`、`error.vue`
- 组合式：`composables/useLocale.ts`、`useMember.ts`、`useCart.ts`、`useOrders.ts`、`utils/uuid.ts`、`utils/tracking.ts`
- 配置/后端：`nuxt.config.ts`、`application.yml`、`StaticAssetConfig.java`、`V119__compliance_and_ui_copy.sql`

## 4. 测试计划覆盖

首页、商品列表（分类/排序/搜索/分页/空态）、商品详情（图/规格/加购/收藏/评价）、购物车、结算下单、订单详情/列表、账户、收藏、足迹、通知、促销（秒杀/买赠/拼团/领券/砍价）、法律页、页脚合规、语言切换、链接审计、运行时质量、移动端 390px、404、三地址来源一致性。

## 5. 仍未做的产品级事项

- 顾客端优惠券领取接口/券中心列表（当前为可访问空态页，等后端券能力）。
- 砍价活动列表接口（当前为可访问空态页；单活动页已有）。
- 真实多规格演示数据（当前多数 SKU 单规格；详情页已能渲染 1 个规格 chip）。
- 商品评价提交入口在订单页已有后端接口，前端发表表单可下一迭代补。
- 领券/砍价/评价属于产品能力缺口，不是本次 P0/P1 阻断项；审计报告的 P3 表保留跟踪。
