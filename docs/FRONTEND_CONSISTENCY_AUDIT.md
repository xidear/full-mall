# 前端一致性审计：六类问题 × 六个端 × 模板 / 语言 / 地区

> 触发问题（2026-10-28）：搜索框文字错位、语言切换与页面风格割裂、分类不高亮、“已经到底了”没有设计、
> 法律页暴露内容 hash、页脚演示/版权信息写死、结算未登录也能下单且没有自动建号提示、定位未授权无引导、
> 商品详情购买按钮高度不一致且缺少详情/参数/评价分区、顾客端 URL 暴露内部 UUID。

## 0. 结论

这六个原始问题不是六个孤立的 bug，而是六条**前端设计基线缺失**：

1. **排版基线**：工具类只在局部 scoped 生效，标签变成可见文字；
2. **控件语境**：工具条控件没有继承深浅底色，出现“白胶囊”；
3. **导航状态**：高亮来自静态 CSS，而不是真实路由；
4. **边界状态**：加载 / 空 / 到底 / 失败没有统一设计语言；
5. **信息投影**：内部字段（hash、枚举、UUID、locale 代码）直接进入用户视野，版本与生效时间没有绑定；
6. **环境与授权**：演示文案、品牌、版权没有配置化，无法适配多模板 / 多地区 / 付费去版权。

同一批缺陷在 **admin、门店工作台、供应商端、客服工作台、移动端** 都存在同源版本。本审计按同一基线逐端检查，
并在本轮修复了能量化、可验证的部分；未完成项单独列在 §6，不伪装成已完成。

## 1. 六条基线（可直接作为 Code Review 检查项）

| # | 现象 | 本质 | 判定标准 | 跨端检查点 |
|---|---|---|---|---|
| 1 | 搜索框前出现“搜索商品”四个字，占位符右移 | `.sr-only` 只写在组件的 scoped 样式里，布局里的 label 没有隐藏 | 全局必须有 `.sr-only`；输入框文字距边框一致 | 所有页面/组件是否依赖局部工具类 |
| 2 | 深色顶栏里的语言下拉是白色胶囊 | 控件没有读取容器语境（深/浅底、尺寸、圆角） | 工具条控件用 `currentColor` / 上下文 token；暗底不出现白底控件 | admin 顶栏、客服顶栏、门店顶栏、POS |
| 3 | 点“美妆个护”后导航仍高亮“全部商品” | 高亮写死为 `:first-child`，没有绑定 path + query | 每个导航项由路由状态计算 `active` 与 `aria-current` | 顶部导航、侧边筛选、后台菜单、门店 tab |
| 4 | “已经到底了”是一行裸文字 | 列表终态没有设计语言 | loading / empty / end / error 四态都有组件、文案、图标与下一步动作 | 商品列表、订单、评价、售后、消息、收藏、足迹 |
| 5 | 法律页显示 `PICKUP · 1.0 · ZH-CN`、内容 hash | 领域元数据没有“面向用户的投影” | 只显示业务版本 + 生效区间 + 状态；未来生效要显示“将于”；内部 hash 不进页面 | 法律页、订单详情、发票、许可页、后台详情 |
| 6 | 页脚固定“开源跨境商城 / 金额单位：分 / 演示数据” | 环境、品牌、授权写死 | demo 文案只在 demo；品牌/副标题可配；版权可开关并绑定付费权益 | 六个端的页脚/头部/登录页 |
| 7 | 结算页不登录也能下单，验证码错误直接抛技术消息 | 身份模型与地区合规未抽象 | 先登录后回跳；CN 显示自动建号；号码支持国际区号；错误走投影层 | PC 结算、移动结算、登录页 |
| 8 | 未授权定位时下拉为空，只写“未授权定位” | 权限没有状态机 | 有明确 CTA、拒绝可重试、无门店有替代方案 | 结算自提、门店选择、地图选点 |
| 9 | 购买区四个控件高度不一致；详情页缺少分区 | 控件高度没有 token；信息架构把详情/参数/评价揉在购买区 | 同一行控件统一 44px；详情页用 tab 分区 | PC/移动商品详情 |

## 2. 跨端矩阵（本轮检查结果）

图例：✅ 已按基线处理；⚠️ 部分处理 / 有明确待办；❌ 仍是缺口；— 不适用。

| 端 | 多模板 | 多语言 | 多地区 | 状态高亮 | 工具条控件 | 空/终态 | 内部信息 | 演示/授权 |
|---|---|---|---|---|---|---|---|---|
| Storefront PC | ✅ 主题 token 化（mall/industrial/fresh/minimal） | ✅ 中/英切换，金额/日期跟随语言 | ✅ `default_market` 驱动法律文档/备案/号码提示 | ✅ 分类 code + path/query | ✅ 深色顶栏语言控件 | ✅ ListEnd + EmptyState | ⚠️ 商品/砍价仍用 UUID（见 §6） | ✅ demo 与 prod 分离，版权可配 |
| Mobile（uni-app） | ✅ 跨页共享主题并持久化 | ❌ 尚无语言切换（见 §6） | ⚠️ 市场已用于主题/条款，文案仍中文 | — 无导航 | ⚠️ 主题条自绘 | ✅ 空态/主题化 | ⚠️ 订单状态已投影，商品 ID 仍在 URL | ⚠️ 无演示标识 |
| Admin | — Element Plus 自有主题 | ❌ 运营端中文（见 §6） | ✅ 新增运营地区配置 | ✅ vue-router 菜单 | ✅ el-select | ⚠️ “没有更多了”按钮（保留兼容测试） | ✅ AI 状态/素材角色/导入类型已投影 | ✅ 登录页演示账号 + 环境说明 |
| Store console | — 自有深色头部 | ❌ 中文 | — | ✅ tab 显式 active | ✅ 深色 tab | ⚠️ 部分表格空态 | ✅ demo 提货码有标识 | ✅ `VITE_APP_MODE` |
| Supplier portal | — 自有 | ❌ 中文 | — | — | ✅ 浅色表单控件 | ⚠️ 空表提示 | ✅ 金额/状态/日期已投影；不再显示 UUID 片段 | — |
| Support console | — 自有深色头部 | ❌ 中文 | — | ✅ 会话 active | ✅ 深色下拉已修 | ⚠️ 空会话提示 | ✅ 角色已投影 | — |

## 3. 本轮已修复（按端）

### 3.1 Storefront PC

| 问题 | 修复 | 位置 |
|---|---|---|
| 搜索文字错位 | 全局 `.sr-only`；输入框占位色改用 token | [main.css](web/apps/storefront/assets/main.css)、[default.vue](web/apps/storefront/layouts/default.vue) |
| 语言控件割裂 | 去掉白色胶囊，改为继承顶栏的透明胶囊 + currentColor 箭头，保留原生 select 可访问性 | [LocaleSwitcher.vue](web/apps/storefront/components/LocaleSwitcher.vue) |
| 分类不高亮 | `active` 由 path + query 计算；补 `aria-current`；URL 改用分类 code | [default.vue](web/apps/storefront/layouts/default.vue)、[products.vue](web/apps/storefront/pages/products.vue) |
| “已经到底了” | 新增 `ListEnd`（细线 + 勾选图标 + “已展示全部 N 件商品”，不额外放“清空筛选”这类多余动作）与 `EmptyState`（图标/标题/说明/动作） | [ListEnd.vue](web/apps/storefront/components/ListEnd.vue)、[EmptyState.vue](web/apps/storefront/components/EmptyState.vue) |
| 法律页内部信息 | 去掉 hash、locale 代码、类型代码；显示“修订版本 + 现行/即将生效/已失效 + 生效/失效区间”；未来生效显示“将于…生效”；当前语言无译本时显式提示并回退中文 | [legal/[type].vue](web/apps/storefront/pages/legal/[type].vue)、[LegalDocument.java](server/src/main/java/com/fullmall/legal/domain/LegalDocument.java) |
| 页脚演示/版权 | `site_tagline`、`default_market`、`branding_attribution` 配置化；demo 只在 demo 显示“只读演示/演示站”；生产不出现“演示数据”；版权可隐藏 | [default.vue](web/apps/storefront/layouts/default.vue)、[V122__site_policy_and_ui_copy.sql](server/src/main/resources/db/migration/V122__site_policy_and_ui_copy.sql)、[SettingsView.vue](web/apps/admin/src/views/SettingsView.vue) |
| 结算登录/合规 | 未登录先显示登录卡；提交时跳登录并带 `redirect=/checkout`；登录后回跳；保存结算草稿；CN 显示“未注册手机号验证后自动创建账号”；号码支持国际区号 | [checkout.vue](web/apps/storefront/pages/checkout.vue)、[login.vue](web/apps/storefront/pages/login.vue) |
| 定位授权 | 显式状态机（idle/requesting/granted/denied/unavailable/unsupported）；“允许定位并排序”“重新获取位置”；拒绝后提示浏览器站点设置；无门店可改快递 | [checkout.vue](web/apps/storefront/pages/checkout.vue) |
| 技术报错 | 新增 `apiErrorMessage`：优先业务文案/错误码，其次状态码人话，绝不显示 `[POST] "/api/...": 400`；技术细节只进 console | [apiError.ts](web/apps/storefront/utils/apiError.ts) |
| 订单 URL UUID | 路由改为 `orders/[orderNo].vue`，后端新增 `GET /api/v1/orders/no/{orderNo}`；结算/账户链接用订单号 | [orders/[orderNo].vue](web/apps/storefront/pages/orders/[orderNo].vue)、[OrderController.java](server/src/main/java/com/fullmall/order/api/OrderController.java) |
| 详情页信息架构 | 购买区只保留决策信息；新增“商品详情 / 规格参数 / 用户评价 / 售后保障”tab；图集与主图合并为同一列（修复 grid 错位） | [product/[id].vue](web/apps/storefront/pages/product/[id].vue) |
| 控件高度 | 全局 `--control-height: 44px`，按钮/数量框/输入框统一高度；移动端保留 sticky 购买条 | [main.css](web/apps/storefront/assets/main.css)、[product/[id].vue](web/apps/storefront/pages/product/[id].vue) |
| 商品图片 URL | 数据库/演示种子可保留相对路径；顾客端输出边界由 `MediaUrlResolver` 统一转绝对 http(s)。优先 `MEDIA_PUBLIC_BASE_URL`（CDN/对象存储），未配置时跟随后台 `site_domain + site_scheme`；换域名/换存储只改配置，不迁移商品图 | [MediaUrlResolver.java](server/src/main/java/com/fullmall/platform/media/MediaUrlResolver.java)、[CatalogService.java](server/src/main/java/com/fullmall/catalog/app/CatalogService.java)、[ProductMediaService.java](server/src/main/java/com/fullmall/catalog/app/ProductMediaService.java) |
| 多模板 | 页面 scoped 样式中的硬编码颜色改为 `var(--brand/accent/surface/line/muted)` 或 `color-mix`；促销、砍价、订单、收藏、足迹、消息、错误页均覆盖 | 见各页面 style 块 |

### 3.2 Mobile

| 问题 | 修复 | 位置 |
|---|---|---|
| 主题只对首页生效 | `useMobileTheme` 改为模块级共享状态 + 本地持久化；每个页面绑定 `:style="themeVars"` | [theme.ts](web/apps/mobile/src/theme.ts)、各 page |
| 金额写死 `¥` | 新增 `formatMoney(amount, currency)`，按 CNY/HKD/MOP/TWD/USD 取符号 | [money.ts](web/apps/mobile/src/utils/money.ts) |
| 11 位手机号写死 | 放宽为 `^\+?[0-9]{6,20}$`，提示支持国家区号 | [checkout.vue](web/apps/mobile/src/pages/checkout/checkout.vue) |
| 结算无条款接受 | 增加条款勾选 + 调用 `/legal/acceptances`，并把 acceptanceId 传入下单 | [checkout.vue](web/apps/mobile/src/pages/checkout/checkout.vue) |
| 订单状态原样显示 | 增加状态投影，未知机器状态显示“处理中” | [orders.vue](web/apps/mobile/src/pages/orders/orders.vue) |
| SKU 编码暴露 | 商品详情优先显示规格值，其次“规格 1/默认规格” | [detail.vue](web/apps/mobile/src/pages/product/detail.vue) |

### 3.3 Admin / 门店 / 供应商 / 客服

| 端 | 修复 |
|---|---|
| Admin | AI 任务状态、素材角色、导入文档类型改为中文标签；系统设置新增站点副标题、运营地区、版权声明；登录页保留演示账号提示 |
| Store console | 门店 tab 显式 active；演示提货码/只读提示保留 |
| Supplier | 退货/结算金额按币种格式化；状态中文化；日期用统一时区；去掉 UUID 片段 |
| Support | 客服角色中文化；深色顶栏下拉改为透明胶囊，消除系统白框 |

## 4. 多模板 / 多语言 / 多地区检查清单

### 4.1 多模板（mall / industrial / fresh / minimal）

- 所有页面只允许使用语义 token：`--bg`、`--surface`、`--surface-muted`、`--text`、`--muted`、`--line`、`--brand`、`--brand-dark`、`--accent`、`--radius`、`--control-height`。
- 对比度：暗色 industrial 主题下，输入框/下拉/卡片必须使用 `--surface` + `--text`，不能出现白底黑字卡片。
- 预览方式：`/products?theme=industrial`、`?theme=fresh`、`?theme=minimal`；后台「主题配置」保存后走 `/api/v1/theme`。
- 移动端：主题状态跨页面共享并持久化；切换后返回购物车/详情仍是同一模板。

### 4.2 多语言

- 金额与日期使用 `currentLocale()`，不写死 `zh-CN`。
- 页面不显示 `ZH-CN`、`en` 等内部 locale 代码；法律页只显示“修订版本”和“生效区间”。
- 新增文案必须同时提供 zh-CN/en；数据库迁移未执行时，关键键在前端有兜底，避免出现 `footer.note.prod` 这类 key。
- 本轮已覆盖：顶栏/导航、搜索占位、分类名称、页脚、法律页 chrome、商品详情 tab 与购买按钮、结算/登录核心文案。
- 仍未覆盖：购物车/账户/收藏/足迹/消息等页面级文案，以及移动端、后台、门店、供应商、客服界面（见 §6）。数据型文案（商品标题、分类名、法律正文）在缺少对应语言时保留原文，并给出“暂无译本”的显式提示，而不是伪装成已翻译。

### 4.3 多地区

- `default_market`（CN/HK/MO/TW）决定：法律文档 market、登录号码提示、备案信息是否展示。
- `app.timezone`（IANA，默认 `Asia/Shanghai`）决定未登录顾客看到的时间；登录会员优先用会员时区。迁移见 `V124__site_timezone.sql`，后台“站点默认时区”可改。
- 媒体基址与存储服务商都以数据库为准：

| 配置 | 来源 | 说明 |
|---|---|---|
| `site_domain` / `site_scheme` | `system_setting`（后台「站点信息」） | 商城域名与协议 |
| `media_public_base_url` | `system_setting`（后台「媒体 / 静态资源基址」） | CDN/对象存储/独立图片域名；空则跟随 `site_domain` |
| `storage_provider` | `system_setting`（后台「存储适配器」） | local/mock/qiniu/cos 等；下一次上传生效 |
| `MEDIA_PUBLIC_BASE_URL` | 环境变量 | **仅开发/CI 兜底**；数据库有值时以数据库为准 |
| `app.asset.qiniu.*` / `app.asset.cos.*` | 环境变量/密钥管理 | 对象存储密钥 |

数据库中的商品图可以保持相对路径，对外 API 输出一律是绝对 URL；换域名或换存储只改数据库配置，不迁移商品图。
- CN 才展示 ICP/公安备案/营业执照；非 CN 只展示公司主体与客服信息。
- 结算自动建号提示按市场显示；号码校验不再限定中国大陆 11 位。
- 邮箱/账号登录属于市场能力，当前后端仅手机 OTP；见 §6。

## 5. 明确的产品决策（本轮建议）

| 决策 | 建议 | 理由 |
|---|---|---|
| 详情页结构 | 购买区只放价格/规格/数量/购买按钮；详情、参数、评价、售后分 tab | 京东/天猫的购买决策与信息浏览分离；避免详情图把购买按钮推走 |
| 控件高度 | 同一行统一 44px；数量框与按钮同高 | 44px 是触控目标下限；同高是视觉基线，不是“看起来差不多” |
| 订单 URL | 用 `orderNo`，不用 UUID | 可读、可客服沟通、可分享；UUID 是内部实现 |
| 分类 URL | 用分类 `code`，不用 UUID | 同上；旧 `categoryId` 继续兼容 |
| 版权 | 付费后默认去除，可自愿保留 | 见 §7 |
| 演示文案 | 只在 demo 显示 | 生产出现“演示数据”会直接损害信任 |

## 6. 未完成 / 下一步（不伪装成已完成）

1. **商品 URL 仍用 UUID**：`product_translation.slug` 已存在，但顾客 API 未暴露 slug；需要后端补 slug 查询与 301 旧链接。
2. **砍价活动 URL 仍用 UUID**：活动没有业务 code，需产品定义活动编号。
3. **邮箱 / 账号登录**：后端 `member_otp` 只有 phone 列；需要 identifier + channel 迁移、邮件 OTP、市场能力开关。
4. **移动端语言切换**：目前移动端只共享主题；文案仍是中文，需补 locale 包与页面字典（或复用后端 i18n）。
5. **Admin/门店/供应商/客服界面语言**：当前中文；若出海运营，需要与顾客端同一套 i18n key。
6. **版权与许可证联动**：当前是 `branding_attribution` 设置；企业版应读取 license feature `branding-removal`，避免未付费也能关闭。
7. **详情页规格参数**：当前用 SKU options + 库存/效期字段生成；商品级属性表（品牌、产地、材质等）需要后端属性模型。
8. **全量后端测试暂被并行改动阻塞**：`ProductTranslationWorkflowServiceTest` 仍按旧构造器创建 `ProductTranslationWorkflowService`/Repository.Row，导致 `test-compile` 失败；与本次前端/法律/设置改动无关，需由该改动的负责人补齐。

## 7. 版权收费建议（与 LICENSING_AND_BUILD.md 对齐）

- **社区版**：免费，保留 “Powered by Full Mall”。
- **标准买断**：一次性授权费，含“去版权”，默认隐藏版权，可自愿显示。
- **企业年费**：年费含去版权、升级、AI/多店等能力；到期进入只读但版权恢复显示（或保留只读期宽限）。
- **不建议**把“去版权”单独拆成很小的年费项：它更适合作为付费档位的权益，降低解释成本。
- 技术实现分两步：当前 `branding_attribution` 设置已可用；下一步把 license `features` 里的 `branding-removal` 映射为公共品牌接口，社区版强制显示。

## 8. 验证（已执行）

| 项 | 结果 |
|---|---|
| Storefront build | ✅ `npm run build --workspace @fullmall/storefront` |
| Admin build | ✅ `npm run build --workspace @fullmall/admin`（含 2026-10-28 并行改动） |
| Store console build | ✅ `npm run build --workspace @fullmall/store-console` |
| Supplier build | ✅ `npm run build --workspace @fullmall/supplier-portal` |
| Support build | ✅ `npm run build --workspace @fullmall/support-console` |
| Mobile H5 build | ✅ `npm run build:h5`（uni-app H5） |
| Backend compile | ✅ `mvn -B -q -Dmaven.test.skip=true compile`（Docker Maven 3.9 / JDK 21） |
| Settings unit test | ✅ `SystemSettingServiceTest`（在并行改动前执行；当前全量 test-compile 被并行的 `ProductTranslationWorkflowServiceTest` 构造器不一致阻塞，见 §6） |
| 运行时验证 | ✅ 本地 API（全新验证库，Flyway 全量迁移）+ Playwright / Chromium |

运行时实测（2026-10-28，隔离库 `fullmall_ui_verify`）：

| 断言 | 实测 |
|---|---|
| 搜索框 label 不可见、占位符左对齐 | ✅ 占位符距边框 16px，无可见“搜索商品”字样 |
| 分类高亮跟随 URL | ✅ `/products?category=BEAUTY` → 仅“美妆个护”带 `active` + `aria-current` |
| 语言控件不再白底 | ✅ computed `background-color: rgba(0,0,0,0)`，深色顶栏内为透明胶囊 |
| 列表终态 | ✅ 仅“已展示全部 1 件商品”，无多余“清空筛选” |
| 法律页 | ✅ 无“内容哈希”；显示“现行版本 / 修订版本 1.0 / 2026/10/27 10:56 起生效”；英文无译本时显示回退提示 |
| 页脚 | ✅ demo 才有“演示站/演示数据”；prod 显示“价格含税 · 门店自提 · 七天无理由” |
| 结算登录优先 | ✅ 未登录点“登录后下单” → `/login?redirect=/checkout`，登录表单含自动建号提示 |
| 控件高度 | ✅ 数量框 + 三个按钮实测均 44px |
| 详情分区 | ✅ tab：商品详情 / 规格参数 / 用户评价 / 售后保障；规格表 5 行 |
| 多模板 | ✅ mall / industrial / fresh / minimal 的 `--text`、`--bg`、导航文字色均随模板切换（industrial 深色下为 `#e8edf0`） |
| 演示数据 | ✅ 扩容到 41 件商品 / 46 个 SKU；`/api/v1/products?size=20` 首屏 20 件、total 41，可验证“加载更多” |
| 商品图绝对 URL | ✅ 商品列表 20 件、全部 SKU 图片均为 `http(s)://`；配置 `MEDIA_PUBLIC_BASE_URL` 或站点域名后输出随之变化 |
| Hydration | ✅ 无 hydration mismatch（修复主题 SSR/客户端不一致与站点时区） |

## 8.1 演示数据扩容（本轮）

原演示库只有 11 件商品，无法覆盖分页、加载更多、多规格、分类筛选等真实场景。新增：

- `server/src/main/resources/db/demo/extra_products.sql`：30 件商品 / 35 个 SKU，覆盖 6 个分类（每类 6–8 件），包含：
  - 单规格与多规格商品（香蕉 1kg/2kg、键盘 87/104 键、口红两色、水壶 750ml/1L）；
  - 生鲜短保商品（批次/效期策略）；
  - 有划线价与无划线价、低客单与高客单；
  - 每 SKU 都有仓库库存，保证加购/结算链路可测。
- `DemoDataSeeder` 每次 dev/demo 启动都执行该 SQL（幂等），已有演示库无需清库即可扩容；当前演示库为 **41 件商品 / 46 个 SKU**。
- 审计脚本 `web/audit/storefront-audit.spec.ts` 已从“11 张卡”改为“首屏 20 张 + 加载更多 + 终态”，避免演示数据增加后测试假失败。

## 8.2 相似类型扫描（全代码）

扫描范围：`server/src/main/java`、`server/src/main/resources`、`web/apps`、`web/packages`、`docs`；
方法：按模式 grep + 人工确认，命中数为粗计，不是精确 AST 统计。

| 问题类型 | 扫描模式（示例） | 粗计 | 代表位置 | 本轮状态 |
|---|---|---|---|---|
| 配置来源不一致（env vs DB） | `@Value`、`system_setting`、`rawValue(` | ~40 | 存储服务商、媒体基址、SMS/支付/物流 provider | 媒体基址/存储服务商已改为 DB 优先；SMS/支付/物流仍是 DB 字段存在但主要读 env |
| 内部标识外泄 | `contentHash`、`skuCode`、`route.params.id`、`categoryId=` | ~30 | 购物车/促销 SKU 编码、商品/砍价 UUID | 订单 URL/分类 URL 已改业务标识；购物车/促销 SKU 编码已隐藏；商品/砍价 URL 待办 |
| 原始错误直接展示 | `cause.message`、`errMsg`、`error.message` | 143 | Admin/门店/供应商/客服/移动端 | 顾客端已用 `apiErrorMessage`；后台/其他端仍有大量直出 |
| 地区/语言/货币硬编码 | `zh-CN`、`'CN'`、`¥`、`11 位` | 170 | 移动端、结算、法律页、页面文案 | 核心链路已抽出 `useSite/useRegionPolicy/currentLocale`；页面级文案仍有中文写死 |
| 硬编码颜色 | `#RRGGBB` | 71 | 顾客端页面、移动端根样式 | 顾客端主题 token 化；移动端已跨页主题；少量品牌色/首帧 fallback 保留 |
| 边界状态缺失 | `已经到底`、`没有更多`、`暂无` | 62 | Admin load-more、移动端空态 | 顾客端已统一 ListEnd/EmptyState；Admin/移动端仍有裸文案 |
| 演示/生产标识散落 | `appMode`、`只读演示`、`演示数据` | ~20 | Storefront/Store console/Admin | Storefront 已按 appMode 分离；其余端仍靠环境变量分支 |
| URL/文件命名暴露内部 ID | `[id].vue`、`route.params.id` | ~10 | 商品、砍价、订单 | 订单已改 `[orderNo]`；商品/砍价待办 |
| 静态资源地址/域名散落 | `/images/`、`localhost:8080`、`/media` | ~30 | seed、文档、旧注释 | API 输出已统一绝对 URL；seed 保留相对路径；文档仍有旧端口 |
| 演示数据/测试脆弱 | 固定卡片数、固定总条数 | 1 处 | `storefront-audit.spec.ts` | 已改为首屏 20 + 加载更多 + 终态 |

结论：**同一类问题确实不止用户点到的那几处**。本轮优先修了“顾客可见 + 会直接影响测试/运营”的部分；
剩余项已按上述类型列入 §6，不会假装全部完成。

## 9. 运行时验收脚本

已执行脚本（本地，不写入仓库）：

```text
/products?category=BEAUTY
/legal/PICKUP（zh-CN / en）
/product/{id}（tabs、控件高度）
/checkout（prod 模式登录回跳）
/products?theme=industrial|fresh|minimal
```

复跑步骤：

1. 启动 API（带 `APP_SECRET_KEY`）与 storefront；
2. 依次访问上述地址；
3. 断言同 §8 表；
4. 记录截图与 `console.error`，失败必须暴露，不允许用“看起来正常”代替。

> 说明：本轮验证使用隔离库，不修改演示库数据；验证结束后保留截图作为证据。
