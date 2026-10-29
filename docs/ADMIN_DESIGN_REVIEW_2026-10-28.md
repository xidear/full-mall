# 后台设计审查（2026-10-28）

> 触发：admin 商品页出现「裂图 / 切语言后选择框与实际值不一致 / 编辑页 500 / 菜单没有图标和折叠按钮」。
> 结论：这四类现象不是四个独立 bug，而是**四类设计缺陷**在具体页面上的投影。本文按"缺陷类"给出根因、影响面、系统性修法与落地状态，避免下一次换个页面又出现同类问题。

## 0. 一页结论

| # | 缺陷类 | 典型现象 | 根因（一句话） | 系统性修法 | 状态 |
|---|---|---|---|---|---|
| A | 资源地址没有唯一来源，且没有降级 | 商品列表裂图、alt 文案撑破缩略图 | 图片地址是"相对路径 + 各前端自己猜"，没有统一解析与失败兜底 | API 统一提供 /images/**；前端统一 resolveMediaUrl() + AdminThumb 组件 | 已修 |
| B | 异步状态与错误语义混乱 | 切到「日本語」后表单还是英文；页面只闪一下 toast | 失败时不清空旧数据；404 被当成业务状态吞掉；无请求竞态保护 | present=false 表达"没有译文"；useAsyncState() 统一 加载/失败/数据 三态 | 已修（商品页、译文、模板） |
| C | 接口契约与错误码漂移 | 商品编辑页 500 "Internal server error" | 前端调 GET /admin/v1/products/{id}，后端没实现；405 落到兜底处理器变成 500 | 补 GET 详情聚合接口；异常处理器补齐 405/415/缺参 等映射 | 已修 |
| D | 后台骨架靠手写，缺成熟能力 | 菜单无图标、不能折叠、各页各写一套状态 | 无图标包、无状态/请求层、无统一错误块 | 引入 @element-plus/icons-vue、@vueuse/core、el-sub-menu 折叠；统一 composable | 已修（骨架 + 商品相关页） |
| E | 信息架构割裂 | 译文是独立模块、规格矩阵和商品脱钩、模板还带价格 | 按"表"而不是按"任务"划分模块 | 译文并入商品编辑页（按启用语言 tab + AI 翻译 + 本地化图）；矩阵改为可复用模板（tags+checkbox、不含价格） | 已修 |

## A. 资源地址与降级

**现象**：商品列表 10 个商品 9 个裂图，alt 文案竖排出格（首行"供应商直发测试商品"无图反而正常）。

**根因链**：

1. 演示数据的 `sku.image_url` 存的是相对路径 `/images/products/headphone.svg`（`server/src/main/resources/db/demo/images.sql`），与 `MediaUrlResolver` 里"对外输出一律绝对 http(s) URL"的规定不一致；
2. 这些 SVG 只存在于 `web/apps/storefront/public/images/products/`，admin 的 public 目录里没有；
3. admin 的 Vite 只代理 `/api`、`/admin`、`/store`、`/supplier`、`/open`、`/mock`，**没有 `/images`、`/assets`**，于是 img src 打到 admin 自己 → 404；
4. 前端直接 `<img :src="row.skus[0].imageUrl">`，没有 error 兜底，浏览器显示裂图，alt 文本把 44px 的格子撑开。

**系统性修法（已落地）**：

- 后端把 `/images/**` 映射到 classpath 静态资源（`StaticAssetConfig`），演示图随 jar 一起发布 → **图片只有一个来源：API**；
- admin / storefront 的 dev server 与 nginx 都把 `/images`、`/assets`、`/media` 转发到 API；
- 前端统一入口 `src/media.ts#resolveMediaUrl()`：绝对地址原样、`/` 开头按同源、其余补 `/`；
- 统一组件 `components/AdminThumb.vue`：地址为空或加载失败都回落到占位块，`object-fit: cover` + `overflow: hidden`，不再出现 alt 撑破布局；
- 列表页、编辑页素材区、译文图片、矩阵 SKU 图全部改用该组件。

**同类风险（已登记，未改）**：`MaterialView`、`AiMediaView` 等页面仍各自写 img 标签，后续应统一替换为 `AdminThumb`。

## B. 异步状态与错误语义

**现象**：译文弹窗中把语言从 en 切到 ja，接口返回 404 TRANSLATION_NOT_FOUND，只闪了一下提示，选择框停在「日本語」但表单仍是英文内容；点「保存草稿」会把英文写进 ja。

**根因链**：

1. `TranslationsView.refreshTranslation()` 用 `catch {}` 把**所有**失败当成"还没有译文"——404、500、断网三种情况无法区分；
2. 失败时**不清空** title/description，于是旧语言内容残留；
3. 切换语言时没有请求令牌/顺序保护，快速切换会出现"后发先至"覆盖；
4. 语言列表写死在组件里（en/ja），与「系统设置」无关，改配置页面不变。

**系统性修法（已落地）**：

- 后端新增 `GET /admin/v1/products/{id}` 详情聚合 + `present=false` 表达"该语言还没有译文"（不再用 404 表达正常状态）；
- 新增 `composables/useAsyncState.ts`：统一 loading/error/data 三态，失败时**清空 data**、保留 error，并用自增 token 丢弃过期响应；
- 商品编辑页按启用语言渲染 tab，每个语言的字段是**独立对象**，结构上不可能串台；
- 语言列表来自 `GET /admin/v1/i18n/locales`（系统设置 `enabled_locales` 的唯一来源）；
- 页面加载失败时显示可重试的错误块，并**禁用保存**（避免用空表单覆盖真实数据）。

**同类风险**：`OrdersView`、`PromotionsView`、`WarehouseView` 等仍大量"catch 后只弹 toast、不清旧数据"，建议逐步迁移到 `useAsyncState()`。

## C. 接口契约与错误码

**现象**：打开 `/products/{id}/edit`，接口 `GET /admin/v1/products/{id}` 返回 500 INTERNAL_ERROR。

**根因链**：

1. `AdminCatalogController` 只实现了 `PUT /{productId}`，没有 `GET /{productId}`；
2. Spring 抛 `HttpRequestMethodNotSupportedException`，`GlobalExceptionHandler` 没有映射它，落到兜底 → 500；
3. 前端与后端没有契约校验，"前端调用了不存在的接口"只能在运行时暴露。

**系统性修法（已落地）**：

- 新增 `GET /admin/v1/products/{productId}`（`ProductDetailView` 聚合商品 + SKU + 素材 + 全部语言译文 + 矩阵模板 id）；
- `GlobalExceptionHandler` 补齐：405（METHOD_NOT_ALLOWED，带可用方法列表）、415（UNSUPPORTED_MEDIA_TYPE）、缺参 400（MISSING_PARAMETER）、参数校验 400；
- 新增回归测试 `GlobalExceptionHandlerTest`、`ProductDetailAndMatrixTemplateTest`。

**同类风险**：`ImportCsv` 的映射写成了 `/admin/v1/products/products/import`（类级 RequestMapping 已含 /products），属于"路径写错但没人调用"的隐性债。

**顺带发现（已修）**：后台页面调用顾客端 `/api/v1/categories` 时，`MemberAuthFilter` 会把 staff token 当成会员 token 解析失败 → 401「登录已失效」。
语义上"无效 token 不降级为游客"是对的，问题在于后台混用了顾客端接口；已新增 `GET /admin/v1/categories`（ADMIN/OPERATOR），商品编辑页改用它。

## D. 后台骨架（Vue 生态）

**现象**：左侧菜单只有文字、不能折叠；页面里没有统一的状态层与错误块，每个页面重复造轮子。

**根因**：admin 只有 Vue 3 + Element Plus + vue-router，缺少成熟后台的常规能力（图标体系、状态持久化、请求状态层）。

**系统性修法（已落地）**：

- `@element-plus/icons-vue`：分组 + 条目全部带图标；
- `el-sub-menu` + collapse + `useLocalStorage`：折叠状态持久化，折叠后显示图标 + 悬浮子菜单；
- `@vueuse/core`：useLocalStorage 等组合式工具，避免自写 localStorage 逻辑；
- `useAsyncState()`：请求状态单点；
- `AdminThumb`、`AssetPicker` 等组件复用。

**选型记录**：见 `docs/requirements/TECHNOLOGY_DECISIONS_V2.md` §8（前端栈）与 `docs/ADR-006_SKU_MATRIX_TEMPLATE.md`。

## E. 信息架构

### 问题 1：商品译文是独立模块

运营要"先到译文页、按商品找、选语言、保存、审核、发布"，与"编辑商品"割裂；且不支持本地化图片、不支持 AI 翻译。

**修法（已落地）**：译文并入商品编辑页「译文」标签：

- 按「系统设置 → 已启用语言」渲染语言 tab；
- 每个语言：标题、描述、**本地化商品图**、状态标签（草稿/机器翻译/已审核/已发布/源文已变更）；
- 按钮：AI 翻译、保存草稿、审核、发布（状态机门禁不变：草稿 → 审核 → 发布）；
- AI 翻译走 `TranslationService`（先查翻译记忆，命中不重复计费；术语表优先），只写 DRAFT；
- 老地址 `/translations` 重定向到商品管理；`TranslationsView` 删除。

### 问题 2：规格矩阵与商品脱钩

原来的矩阵页要求手填"商品 ID"、用纯文本 `颜色:黑,白` 描述维度，还要填价格。

**修法（已落地）**：规格矩阵改为**模板**：

- 模板 = 维度 + 取值 + 禁用组合，**不含价格**（未绑定商品时价格没有意义）；
- 维度/取值用**标签**编辑，组合用**勾选框**（取消勾选即禁用该组合）；
- 模板可独立创建、可被多个商品复用；商品编辑页"选择模板 → 输入前缀和价格 → 应用"生成 SKU；
- 模板被引用时删除会被拒绝（先解绑再删）。

### 问题 3：编辑页保存语义不清

编辑态点"保存"只提交标题/描述/类目，SKU 改动被静默丢弃（还弹"已保存"）。

**修法（已落地）**：基础信息与 SKU 价格各有独立保存入口，保存后明确提示保存了什么；SKU 改价走 `POST /admin/v1/products/skus/batch-price`（整批成功或整批失败）。

## F. 防回归门禁

1. **错误码**：框架级异常必须有明确 HTTP 状态；新增 `GlobalExceptionHandlerTest`。
2. **契约**：详情页统一用一个聚合接口，不允许"页面发起 4 个请求、任一失败只弹 toast"。
3. **状态**：异步页面用 `useAsyncState()`；禁止 `catch {}` 吞错；"资源为空"用 present=false / 空数组表达，不用 404。
4. **图片**：一律 `AdminThumb`；新增静态图片目录必须挂到 API（`StaticAssetConfig` + 各端代理）。
5. **语言**：语言列表只来自 `GET /admin/v1/i18n/locales`，禁止前端硬编码。

## G. 本次未完成（后续 TODO）

| 项 | 说明 |
|---|---|
| 其余页面迁移 | OrdersView / PromotionsView / WarehouseView / PosMembersView 等仍是 toast + 旧数据残留 |
| 图片组件统一 | MaterialView、AiMediaView 的 img 尚未换成 AdminThumb |
| 前端契约检查 | 建议引入 OpenAPI 生成的客户端或 CI 里的路径对照检查，避免"调用了不存在的接口" |
| 路由 401 处理 | router.beforeEach 里 loadMe 失败即清空登录态，网络抖动也会被当成未登录，建议区分 401/5xx |
| 类目管理 | 商品编辑页已改为读取 /api/v1/categories，但类目增删改页面仍未提供 |

## H. 验证记录（2026-10-28，本地 dev）

| 验证项 | 结果 |
|---|---|
| 后端测试（GlobalExceptionHandlerTest / ProductDetailAndMatrixTemplateTest / TranslationDraftTest / ProductTranslationWorkflowServiceTest / ProductBasicsUpdateTest / ProductMediaTest / ServerContextTest） | 22 passed |
| admin 类型检查 + 生产构建（vue-tsc --noEmit && vite build） | 通过 |
| e2e `web/e2e/translations.spec.ts`（chromium） | 2 passed（译文链路 + 旧地址重定向） |
| 商品列表 10 张缩略图 | `naturalWidth > 0` 全部为真（不再裂图） |
| 页面 HTTP 错误（列表 / 编辑页 / 模板页） | 0 |
| 接口抽查 | `GET /admin/v1/products/{id}` 200；`GET /admin/v1/products/skus/batch-price` 405；`GET /images/products/milk.svg` 200；`GET /admin/v1/i18n/locales` 200；模板创建/应用/删除 201·200·409；AI 翻译 200（写入 DRAFT） |

截图（`docs/review-2026-10-28/`）：`products-list.png`、`products-list-collapsed.png`、`product-edit-translations.png`、`matrix-template-tags-checkbox.png`。

## I. 本次改动清单（便于 review）

**后端**

- 迁移 `V123__admin_product_detail_i18n_matrix.sql`：译文 image_url、矩阵模板四张表、product.sku_matrix_template_id、enabled_locales 默认值
- `GET /admin/v1/products/{id}`（AdminProductDetailService / ProductDetailView）
- `GET /admin/v1/products/{id}/translations`（列表含未录入语言）、`POST .../auto-translate`
- `GET /admin/v1/i18n/locales`
- 矩阵模板 CRUD + `POST /admin/v1/sku-matrix-templates/{id}/apply`
- GlobalExceptionHandler 405/415/缺参/参数校验；StaticAssetConfig /images/**

**前端（admin）**

- App.vue：图标、分组子菜单、折叠（持久化）
- media.ts、composables/useAsyncState.ts、components/AdminThumb.vue
- ProductListView.vue：缩略图组件 + 「译文」入口
- ProductEditView.vue：四标签页（基础/素材/规格/译文），译文含 AI 翻译与本地化图
- SkuMatrixView.vue：模板化 + 标签/勾选
- SettingsView.vue：新增「已启用语言」
- vite.config.ts：/images、/assets、/media 代理


## J. 全仓扫描（第二轮，2026-10-28 12:00-12:45）

第一轮只把"商品管理"这条线修透。第二轮用脚本把五类缺陷扫到全部代码：

### J.1 方法

1. **接口契约对照**：脚本解析后端全部 Controller（类级 RequestMapping + 方法级 Get/Post/Put/Patch/Delete），
   抽取 6 个前端应用（admin / store-console / supplier-portal / support-console / storefront / mobile）
   全部 api.get/post/put/patch/delete 调用，做方法 + 路径归一化后求差集，再逐个用真实 HTTP 复核。
2. **图片清单**：统计各应用 img 标签数量与 @error 兜底数量。
3. **空 catch 清单**：列出所有 catch {} 并逐条判断"合法忽略"还是"吞错"。
4. **框架骨架**：检查其余控制台是否有侧边栏/菜单，判断是否需要图标与折叠。

### J.2 契约缺口（扫描 + HTTP 复核结果）

| 前端调用 | 后端实际 | 现象 | 处置 |
|---|---|---|---|
| GET /admin/v1/inventory/batches/expiring | 无 | 临期商品页 404，只弹一条 toast | 新增接口（复用每日巡检同一查询） |
| GET /admin/v1/promotions/group-buys | 只有顾客端 /api/v1/promotions/group-buys/{code} | 拼团记录弹窗 404 | 新增后台列表接口（按 promotionId 过滤 + 限量） |
| GET /admin/v1/faq | 有，但 SQL 报错 | 500 could not determine data type of parameter $1 | FAQ 列表改双分支查询（无关键词不带过滤条件） |
| POST /admin/v1/products/import | 实际路径被写成 /admin/v1/products/products/import | 调用落到 /{productId}，报"参数 productId 类型不正确" | 修正映射为 /import |
| GET /admin/v1/ai/status、POST /admin/v1/ai/ask | 社区版没有 AI 模块 | 404 | **设计如此**：AiView 用 404 识别"社区版"并提示，保留 |
| POST /admin/v1/ops/containers | 只有 GET /containers + POST /containers/{host}/{id}/{action} | 扫描误报 | 前端实际调用带路径的 POST，无需改 |

复核（重启后实测）：expiring 200（返回临期批次）、group-buys 200、faq 200、import 400（行校验，语义正确）。

### J.3 图片缺口

| 应用 | 裸 img | 处置 |
|---|---|---|
| admin | 11 处（MaterialView 4、AiMediaView 2、AssetPicker 2、AdminThumb/AssetImage 自身 2、注释 1） | 新增 AssetImage 组件（保留宿主 class 与事件 + resolveMediaUrl + 失败占位），已全部替换；AdminThumb 用于固定尺寸缩略图 |
| storefront | 5 处内容图（购物车行、商品主图、图集、详情图、商品卡） | 新增 utils/media.ts（resolveMediaUrl + onImageError 占位图），已接入；静态 /images/hero.svg 不需要 |
| store-console / supplier-portal / support-console / mobile | 0 处（用文字/颜色卡片，无图片位） | 无需改 |

### J.4 吞错清单

空 catch 共 21 处，逐条判定：

| 类别 | 数量 | 例子 | 处置 |
|---|---|---|---|
| 合法的"用户取消/浏览器能力缺失" | 18 | ElMessageBox 取消、clipboard 失败有提示、localStorage 损坏有提示、WS 脏包下一轮轮询兜底 | 保留（都不是静默失败） |
| 真吞错（失败等于"没有数据"） | 3 | AfterSalesView 入库地点、support-console 商品下拉、admin 路由守卫 loadMe | 已修：前两处显示显式错误文案；路由守卫只在 401/403 清登录态，网络抖动不再把人踢出 |

### J.5 框架骨架

- admin：Element Plus + 图标 + 分组折叠（第一轮已做）。
- store-console：单页 POS 控制台，顶部 tab 切换（不是多模块后台），加侧边栏反而是负担 → 保留。
- supplier-portal / support-console：单用途工作台，自有轻量样式，无 Element Plus 依赖 → 保留；若要统一，属于"换壳"级别改造，单列 TODO。

### J.6 本轮新增回归测试

- FaqListTest：无关键词/空关键词/有过滤关键词三条路径（防"参数类型推断"类 500 复发）。


### J.7 扫描中额外发现（测试与数据依赖）

- e2e `admin.spec.ts` 硬编码演示标题"有机鲜牛奶"，演示数据增加后商品落到第 2 页 → 测试误红。
  已改为"列表渲染出商品行"的稳定断言（不依赖具体种子数据）。
- 后台菜单分组默认展开：Element Plus 的 default-openeds 只在初始化时生效，登录态异步加载导致分组重渲染、默认展开丢失；
  改为挂载后显式 open() 每个分组（实测 8/8 全部展开）。
- 并行工作区提示：本仓同时有另一个进程以 Docker(root) 方式构建 `server/target`，会让 `mvn package` 报
  Operation not permitted。本轮已在隔离目录 /tmp/fm-server-build 构建并运行 API，避免与它互相踩。


### J.8 第二轮修复中再次暴露的设计问题（已修）

- **弹窗被容器盖住**：AssetPicker 的选择弹窗使用 el-dialog 默认内联渲染，放进「规格」标签页后
  被 el-tabs 的标签栏盖住，点击素材无效（e2e 超时）。改为 append-to-body。
  这是一类问题：**组件被复用到有层叠上下文的新容器时，弹窗/Dropdown 必须 teleport 到 body**。
- **规格图入口丢失**：商品编辑页改成矩阵生成 SKU 后，原先"每行一个规格图选择器"消失；
  现在规格表里每行都有「选择规格图」（写入 product_media role=SPEC，绑定 skuId），能力没有退化。
- **e2e 依赖具体页面结构**：`admin-pickers.spec` 假设规格图选择器在首屏；已改为先切到「规格」标签。


## K. 第三轮：按"设计规范"扫呈现层（2026-10-28 15:00-16:20）

用户反馈"菜单里很多项目都看不到了"，追问"是否按设计规范测过页面"。之前的验证只到"接口 200"，
没有验证"页面能不能读"。本轮新增可执行审计脚本，并修掉它扫出来的问题。

### K.1 新增门禁

| 脚本 | 作用 | 接入 |
|---|---|---|
| scripts/check-api-alignment.py（重写） | 前端调用 ↔ 后端路由，**方法+路径**，覆盖 /api /admin /store /supplier /open /support /mock | Makefile contract-check；CI 已有步骤（之前版本只扫 /api/v1 且不分方法，等于假绿） |
| scripts/audit-ui.mjs（新增） | 39 个后台路由逐页检查：对比度 < 3:1、裂图、控制台报错、HTTP 错误、横向溢出 | make ui-audit（需 API 41280 + admin 41281） |

### K.2 审计扫出并修掉的真问题

| # | 现象 | 根因 | 修法 |
|---|---|---|---|
| 1 | 侧栏"有位置、没文字" | Element Plus 的 --el-menu-text-color 默认 #303133 压在深色侧栏上；只写 .el-menu-item{color} 会被组件库规则吃掉 | 改用 EP 变量（--el-menu-text-color / -hover- / -active-）+ 显式同权重颜色；8/8 分组默认展开 |
| 2 | 菜单展开时"位置占着、内容看不见"（我的第一版修法） | 用 menu.open() 连续补开 8 个分组，把 EP 的折叠动画打断在半途 | 改为"登录态就绪后再渲染 el-menu"，用 default-openeds 一次性初始化 |
| 3 | /themes、/pos、/warehouse 等页面报 401「登录已失效」 | 后台复用顾客端 /api/v1/**，而 MemberAuthFilter 把员工 token 当会员 token 解析失败就 401 | 过滤器识别出员工会话后按员工身份放行；两种会话都无效才 401 |
| 4 | /integrations 500 | 又一处 (? IS NULL OR capability = ?) 未标注类型 → could not determine data type of parameter $1 | 改成 ?::text；全仓已无未标注的 ? IS NULL 写法 |
| 5 | 直接打开 /suppliers 返回 JSON 404 | Vite 代理键 '/supplier' 前缀匹配了前端路由 /suppliers | 代理键加 ^ 锚点（^/supplier/ 等 9 条） |
| 6 | 禁用按钮白字压浅红底（1.7:1）、文字按钮 2.2:1、tag 文字 2.1:1、提示框灰字 2.8:1、占位图文字 2.9:1 | 直接用组件库主题色当字色 | 统一用 EP 变量覆盖（success/ danger/ warning/ info 的 text-color，disabled-text-color，placeholder，alert 标题/正文，select tags） |
| 7 | 部分页面 .muted 被局部改成 #999（2.8:1） | 每个视图各写一份 .muted | 删除局部覆盖，统一走全局 .muted |

### K.3 结果

`make ui-audit`：39 个路由，0 个问题（对比度阈值 3:1；WCAG AA 正文要求 4.5:1，后续可再收紧）。
`make contract-check`：后端 350 条路由 / 前端 161 处调用，未匹配 0。

### K.4 遗留

- 审计阈值目前 3:1，若要严格 AA（4.5:1）还需再收一轮（主要在浅色 tag / 小字号提示）。
- audit-ui 依赖本机 Playwright chromium，暂未进 CI（CI 的 web job 没有起后端）；若要进 CI，需先加一个启动 API + 种子的 job。


## L. 第四轮：规格改价改为"商品内弹窗"（2026-10-28 16:30-17:00）

用户判断：原「规格改价」是独立页面、按"存储=256G"跨商品匹配——200 个商品都有 256G 时会被一起改，
而价格本应是"某商品某 SKU"的事实。结论：这个功能应在商品管理内部，作用域是单个商品。

### L.1 设计（按运营操作顺序）

1. 商品列表「操作」列新增 **规格改价** 按钮；
2. 弹窗左侧勾**维度**（存储 / 颜色…），右侧出现该维度的**取值**（勾选框）；
3. 语义：**同维度多值是"或"，跨维度是"且"** —— 勾「颜色=红」+「存储=128G,256G」只命中红×128G、红×256G；
4. 底部「加价/减价」+ 金额，实时预览命中 SKU 的改前/改后；
5. 「确定改价」整批成功或整批失败；减价后 ≤0 直接拒绝并指出具体 SKU。

### L.2 后端

| 接口 | 说明 |
|---|---|
| GET /admin/v1/products/{id}/sku-dimensions | 该商品真实存在的维度与取值（来自 sku_option_value 关联表） |
| POST /admin/v1/products/{id}/price-rule/preview-multi | match: 维度 → 取值数组，维度间 AND、值间 OR |
| POST /admin/v1/products/{id}/price-rule/apply-multi | 同上，整批落库 |

- `SkuPriceRuleService` 重写：原来的"单维度单值 + SKU_FILTER_SINGLE_ONLY"限制取消；
- 旧的 `preview/apply`（单值）保留并委托到新实现，避免破坏老调用方；
- 一个值都不勾 → 400（不做"无差别改整个商品"）；
- 回归测试 `SkuPriceRuleMultiTest`：维度交集、单维度只命中该维度、空选择拒绝、减价越界拒绝、维度只暴露真实组合。

### L.3 前端

- 新增 `components/SkuPriceDialog.vue`（商品内弹窗）；
- `ProductListView` 每行「规格改价」按钮；
- 删除独立页面 `PriceRuleView.vue`、菜单项；旧地址 `/price-rule` 重定向到 `/products`；
- 帮助文档（help/catalog.ts）同步改写。

### L.4 验证

- 接口：用「无线蓝牙耳机」生成 颜色×存储 4 个 SKU → 勾 红×{128G,256G} 预览命中 2 个、应用后绿×… 仍为原价；
- 界面：截图 `docs/review-2026-10-28/price-dialog-selected.png`（左侧维度、右侧取值、底部加减+金额+预览表）。
- 测试：`SkuPriceRuleMultiTest` 4 项通过。


### L.5 弹窗内"维度名与取值上下不齐"（2026-10-28 17:10）

现象：模板弹窗里，左侧"维度名"输入框与右侧取值标签框高度不一致、上下错位。

根因（三个尺寸混用）：

1. 维度名输入框是 Element Plus 默认尺寸 32px；
2. 取值输入框和「添加」按钮用了 size="small"（24px）；
3. 标签框 min-height:40px + padding:6px，且带 1px 虚线边框（又高出 2px）。

修法：

- 三个控件统一用默认尺寸（32px）；
- 标签框去掉边框改为 inset 阴影（阴影不占布局），单行时正好 32px，与输入框严格等高；
- 标签换行时容器向下生长（row-gap 8px），同行其它控件保持顶部对齐（align-items: flex-start）；
- 删除维度按钮显式 height:32px。

验证：脚本量取 5 个元素的 getBoundingClientRect，1050px 与 1440px 两个宽度下均为 top=411 / height=32；标签换行（6 个取值）时容器 104px、其它控件仍在顶部。截图：docs/review-2026-10-28/matrix-dimension-aligned.png。


## M. 品牌重复：不是 seed 重复播种，是 e2e 测试不清理（2026-10-28 17:20）

用户发现品牌管理里有 24 条同名「E2E 品牌」。排查结论：

| 可能原因 | 结论 |
|---|---|
| 种子重复播种 | **否**：`db/demo/seed.sql` 里根本没有 brand 插入；演示库只有「演示品牌甲/乙」各 1 条 |
| 种子自身重复 | **否**：同名只有「E2E 品牌」，且编码各不相同（E2E-BR-xxxxxx） |
| 测试重复创建 | **是**：`web/e2e/bargain-and-brand.spec.ts` 每次运行都 POST 新建一条 `E2E 品牌`（编码带随机后缀），**从不删除**；跑了 24 次就留下 24 条 |

两层设计问题：

1. **模型层缺约束**：`brand` 只有 `code` 唯一，`name` 没有唯一性。品牌名会被用来做促销/券的"指定品牌"范围，同名多条会让筛选出现两个一样的选项。
2. **测试不自清理**：测试数据写进演示库，越跑越脏；而且测试行为与标题不符（标题写"creates and deletes"，实际只创建）。

修法：

- 迁移 `V130__brand_name_unique.sql`：先按 `lower(btrim(name))` 去重（引用商品改指向保留项），再建唯一索引；
- `BrandService`：创建/改名都校验同名（忽略大小写与首尾空格）→ 409 `BRAND_NAME_DUPLICATED`；
- e2e：名字带随机后缀 + 断言重名 409 + **测试末尾删除自建品牌**，保证可重复运行；
- 清理演示库里历史遗留的 24 条（引用数为 0，删除安全）。

验证：品牌列表现在 2 条（演示品牌甲/乙）；`BrandServiceTest` 3 项通过（含新增"同名拒绝、改名撞名拒绝、改回自己名字不报错"）。


## N. 首次进入的引导：域名 + 商城名称 + 「安装示例数据」开关（2026-10-28 17:30-18:00）

需求（用户）：首次进入的引导里要能填**域名**和**商城名称**，并加一个「安装示例数据」勾选框；
不勾选 = 完全空白，勾选 = 安装示例数据。

### N.1 后端

| 接口 | 说明 |
|---|---|
| GET /admin/v1/setup/data | 当前数据规模（商品/订单/会员/门店）+ 已选模式 + 本环境是否有示例种子 |
| POST /admin/v1/setup/data {mode} | DEMO=安装示例数据（幂等）；BLANK=清空为空白商城 |

- 新增设置键 `onboarding.data-mode`（DEMO / BLANK），**重启后依然生效**：
  `DemoDataSeeder` 启动时读这个键，BLANK 就跳过播种（否则每次重启都会把示例数据塞回来）。
- BLANK 的安全阀：只有"仍在首次配置阶段（未标记完成）"或"没有订单/会员"才允许清空，否则 409 并说明原因。
- 清空范围：除"配置/身份/审计/资产/合规"外的业务表全部 TRUNCATE（CASCADE 处理外键），
  **表名按 pg_tables 动态生成**——硬编码表名会在迁移新增/删除表后直接 500（`product_review_image` 就踩过）。
- `DemoDataSeeder.installNow()`：按与启动播种相同的顺序（seed → staff → images → extra → promotions）幂等安装。

### N.2 前端

「首次配置」第 1 步新增：

- **商城名称**（site_name）
- **对外域名**（site_domain）：只填 host[:port]，带说明（分享/SEO canonical/支付回调用它，可稍后在系统设置改）
- **示例数据** 勾选框：默认勾选；说明里实时显示"当前商品 41 / 订单 293 / 会员 490"
- 取消勾选且有数据时弹二次确认（清空不可撤销）

### N.3 验证

- `SetupDataModeTest` 4 项通过：首次阶段清空生效、已有订单/会员时拒绝、test profile 无种子时明确报错、非法模式 400
- 接口实测：`GET /setup/data` 返回 41/293/490；`POST {DEMO}` 幂等（数量不变）；`{mode:"NOPE"}` → 400
- 界面：截图 `docs/review-2026-10-28/setup-onboarding.png`

### N.4 注意

演示库里订单/会员已有数据（293 单 / 490 人），此时取消勾选会真的清空——所以加了二次确认。
若你希望"空白"只清商品、保留会员与订单，我可以把范围收窄（当前按"完全空白"理解）。


## O. 对齐/尺寸一致性：全量扫描与统一修复（2026-10-28 18:10-18:40）

用户第 N 次指出"选素材按钮没对齐"，并明确要求：**不要一处一处指，自己扫全部**。

### O.1 检查器（并入 make ui-audit）

在 `scripts/audit-ui.mjs` 新增"同一行控件尺寸/对齐"规则：

1. 收集所有控件（`el-input / el-select / el-input-number / el-button / el-checkbox / el-radio-group / el-switch / el-textarea / img / admin-thumb / asset-image-fallback`）；
2. 按父容器分组，再按"垂直中心 8px 内"聚成同一行（换行的两行不会被误判）；
3. 同一行内：**高度差 > 4px** 或 **top 差 > 4px** → 报错（el-tag 作为徽标不参与高度比较，但要同一行）；
4. **同时打开"要点开才看得到"的界面**：商品列表的「规格改价」弹窗、规格矩阵的「新建模板」弹窗、商品编辑的「译文 / 规格」标签页——
   之前两次对齐 bug 都发生在弹窗里，只扫路由必然漏掉（`INTERACTIONS` 表驱动）。

### O.2 扫出的问题与修复

| 位置 | 问题 | 修法 |
|---|---|---|
| 商品编辑 → 译文 → 本地化商品图 | 缩略图 64px，URL 输入框 32px、选素材按钮 30px：三者垂直中心对齐但视觉错位 | 缩略图改 32px（与输入框等高），整行 32/32/30，top 差 ≤ 1px |
| 规格矩阵 → 新建模板（上一轮已修） | 维度名 32 / 取值框 40+2px 边框 / 输入 small 24 | 统一 32px + inset 阴影替代边框 |
| AI 素材 → 挂载栏 | 一个默认尺寸 select + 一个 `size="small"` select | 去掉 small |
| 首次配置页头部 | 按钮 32px 与状态 tag 24px 同行 | tag 视为徽标（规则豁免），但保留同行校验 |
| 各弹窗内提示框 | 低对比度 2.04（`.admin-main` 限定，弹窗被 teleport 到 body 后规则失效） | 规则去掉 `.admin-main` 限定 |
| 译文页缩略图占位 | "无图" 文字 2.9:1（AdminThumb 漏改） | 与 AssetImage 统一为 #5b615c |

### O.3 验证

- `make ui-audit`：40 个路由 + 4 个弹窗/标签页状态，**0 问题**
- 反证：把译文缩略图临时改回 64px，检查器立刻报"同一行控件高度不一致 64 vs 32"（top 差 16px）
- admin 构建通过；e2e（admin / translations / admin-pickers）通过
- 截图：`docs/review-2026-10-28/translation-row-aligned.png`


## P. 富文本详情怎么翻译（2026-10-28 19:20-19:50）

用户问题：商品详情是富文本，翻译怎么省事又方便？

### P.1 三种方案对比

| 方案 | 结构风险 | 能否逐段审核 | 成本 | 结论 |
|---|---|---|---|---|
| 整段 HTML 交给模型，要回 HTML | 高：模型会改标签/属性（style/class/内联 video/base64） | 不能 | 改一个字也要整篇重翻 | 不采用 |
| 取纯文本，人工重排 | 中：列表/表格/图文顺序会丢 | 部分 | 整篇 | 不采用 |
| **只替换文本节点**（采用） | **零**：DOM 只换文本，标签与属性原样 | **能**：每段一条 | **命中翻译记忆不计费** | 采用 |

### P.2 落地规则

1. 详情用富文本编辑器产出**受限 HTML**（p/h2-h3/ul/ol/li/strong/em/a/img/video/figure/figcaption/br）；
2. 翻译时用 jsoup（MIT，固定 1.18.3）解析，按块级元素（p/h1-h6/li/td/figcaption/blockquote）取值；
3. **段 = 文本节点**：`<p>这是一款<strong>陶瓷</strong>马克杯</p>` 拆成 3 段 —— 改一个词只重翻那个词（记忆按文本哈希命中）；
4. 行内标签（strong/em/a/br）保留，首尾空白保留（中英混排不会粘在一起）；
5. 跳过 code/pre/script/style 与 [data-no-translate]（品牌名、SKU 编码、法务原文）；
6. 图片/视频**不动**（语言无关）；需要本地化图时用图片上的 data-locale-src 覆盖（后续）；
7. 译文写入 product_translation.detail_html，走原状态机（草稿 → 审核 → 发布），前台按 locale 取，未发布回退默认语言。

### P.3 接口与代码

- POST /admin/v1/products/{id}/translations/{locale}/auto-translate-detail
  → 返回 {html, segments, cached, details[]}，并把译文写入草稿的 detail_html
- RichTextTranslationService：解析 → 逐文本节点翻译 → 回填
- 前端「译文」标签页新增「翻译富文本详情」按钮 + 译文预览（只读），提示「共 N 段，命中记忆 M 段（未重复计费）」

### P.4 验证（实测）

输入：

    <h2>商品亮点</h2>
    <p>这是一款<strong>陶瓷</strong>马克杯，容量 350ml。</p>
    <ul><li>可微波炉加热</li><li>可洗碗机清洗</li></ul>
    <figure><img src="/images/products/mug.svg" alt="马克杯"><figcaption>杯身细节</figcaption></figure>
    <pre><code>SKU-MUG-350</code></pre>

第一次调用：segments=7, cached=0；标签、img 的 src/alt 原样，`<code>SKU-MUG-350</code>` 未被翻译。
**第二次调用：segments=7, cached=7（全部命中记忆，0 次 provider 调用）**。

单元测试 RichTextTranslationTest 4 项通过：结构与行内标签保留、code/data-no-translate 跳过、首尾空白保留且命中计数正确、空 HTML 不报错。

### P.5 还没做（下一步）

1. **富文本编辑器**（TipTap，MIT）与商城详情渲染（目前详情需通过接口/SQL 写入）；
2. 图片按语言本地化（data-locale-src）；
3. 段落级「只重翻这一段」的 UI（数据已按段记录，前端加按钮即可）。


## Q. 素材库 AI 生成入口 + 分类/标签/理解型搜索（2026-10-28 20:20-21:10）

用户反馈：上传素材处要有「AI 生成」按钮（配置了生图/生视频模型时可用），复用提示词；
素材要分类/标签、理解型搜索，不能只按文件名；并指出「AI 生图没显示出来」。

### Q.1 之前理解不一致的地方（这就是「没显示出来」的原因）

| 用户期望 | 旧代码实际 |
|---|---|
| 上传区有「AI 生成」 | 只有工具栏一个「AI 生成变体」，且**强制先选参考图**，否则报「先选择至少一张参考图」 |
| 图片/视频都能生成 | 只筛 kind=IMAGE，已配置的 mock-video 完全用不上 |
| 生成结果显示出来 | mock 供应商返回 wiremock 容器内地址 localhost:8089，浏览器不可达 → 全部显示「无图」 |
| 复用提示词 | 有 /ai/media/prompts 接口，但只在那一个变体弹窗里用 |
| 分类/标签/理解型搜索 | 素材库与选择器都只有文件名前端过滤 |

### Q.2 修复

1. **新增组件 AiGeneratePanel**（图片/视频共用）：按已配置模型显示「图片 / 视频」切换、供应商下拉、尺寸/时长、
   提示词 + 负向提示词、最近提示词一键复用；生成后自动入库并触发标签/索引。
2. **素材库上传区**新增「AI 生成」按钮 → 弹窗内嵌该面板（原「AI 生成变体」保留，用于参考图改图）。
3. **mock 生图改为返回 b64_json**（不再是 wiremock 内部 URL）→ 适配器落盘到本站存储，素材 URL 变成 /assets/ai/… ，
   浏览器可直接显示；并清理了 23 条指向 localhost:8089 的历史素材。
4. **AI 任务用途枚举修正**：面板原来传 AI_GENERATED（素材用途），而 ai_gen_job 只接受 MAIN/SPEC/DETAIL/VIDEO/COPY → 建任务直接 500。
5. **生成结果自动打标签**：新增事件 AiMediaService.AssetGenerated + AiAssetIndexingListener，
   用提示词当描述、按确定性规则抽标签（最多 8 个）、并写入向量索引。
6. **AssetPicker 重写**：服务端分页（el-pagination）+ 标签筛选 + 关键词（名称/描述/标签）+「理解型搜索」开关（语义检索），
   每个素材显示标签与相似度。

### Q.3 验证（实测）

- 生成一次图片：POST /admin/v1/ai/media/jobs（purpose=MAIN）→ 200 → 12s 内 SUCCEEDED；
  入库素材 url=/assets/ai/mock-image/&lt;jobId&gt;.png、caption=提示词、tags=[白底商品主图,陶瓷马克杯,柔和侧光,桌面场景]
- 关键词搜「马克杯」命中该素材；/admin/v1/assets/tags 出现这些标签；语义搜「白底的杯子」有命中；
- 界面：素材库 8 张缩略图全部 naturalWidth>0；「AI 生成」弹窗含 图片/视频 切换、6 个最近提示词（截图 ai-generate-panel.png）
- 顺带修：embedding 建索引泄漏数据库连接（20 条后全站 500），reindex 现在返回 indexed=30

### Q.4 仍未做

1. 用视觉模型自动描述图片内容（当前描述来自提示词/人工；本机未装 VLM，如 llava）——接口已预留（无供应商会明确报错）；
2. 富文本详情编辑器（TipTap）与商城详情渲染；
3. e2e 上传自清理（避免再堆垃圾素材）。


## R. 后台壳滚动、视觉模型、富文本详情、e2e 自清理（2026-10-28 22:00 — 10-29 09:40）

### R.1 后台壳滚动（用户报：菜单长时点菜单，右侧内容停在上一次滚动位置，像空白页）

根因：整个页面用 body 滚动（`.admin-shell{min-height:100vh}`），侧栏不是固定的。

修法（Element Plus admin 常规做法）：
- `html,body,#app{height:100%}`、`body{overflow:hidden}`；
- `.admin-shell{height:100vh;overflow:hidden}`；侧栏 `height:100vh;overflow-y:auto`（菜单自己滚）；
- 右侧容器 `flex-direction:column; height:100vh`，`el-main{flex:1;overflow-y:auto}`（主区自己滚）；
- `router.afterEach` + `nextTick` 把 `.admin-main` 与窗口滚回顶部。

验证（Playwright）：滚到 951px → 点菜单「AI 客服」→ 主区 scrollTop=0、侧栏 top=0/height=720、body overflow=hidden。

### R.2 视觉模型：走供应商配置，主程序解耦

按用户要求：视觉模型可能是**远端公共模型服务**或**本地 CPU 推理服务**，主程序只认配置。

- `ai_provider` 新增 `vision` 标记（迁移 V134）；`PUT /admin/v1/ai/providers/{code}` 可设置；
- `GET /admin/v1/ai/providers/vision` 返回是否已配置（未配置时给明确提示，不影响其它功能）；
- `POST /admin/v1/assets/{id}/describe` → 取 vision=true 的供应商 → 多模态对话 → 写入描述/标签并重建索引；
- 没有视觉供应商时返回 409 `AI_VISION_UNAVAILABLE`（不静默降级成"用文件名当描述"）。
- 本机测试模型：`ollama pull moondream`（1.7GB，轻量、较新），以 `ollama-vision` 供应商接入
  （endpoint http://127.0.0.1:41289/v1）。**不默认启用**：删掉该供应商或停掉 ollama 服务，程序照常运行。

实测：`describe` 200，返回模型对该图的真实描述；小模型不按 JSON 输出时，描述整段保存、
标签用本地规则从描述里抽（`AssetTextKeywords`），仍可被语义检索命中。

### R.3 富文本详情（TipTap + 服务端白名单 + 商城渲染）

- 编辑器：`components/RichTextEditor.vue`（TipTap 2.11.5，MIT；标题/列表/加粗斜体/引用/图片/视频）；
- 后台：商品编辑页新增「详情」标签；`PUT /admin/v1/products/{id}` 增加 `detailHtml`；
- 服务端：`RichTextSanitizer`（jsoup Safelist）白名单清洗后才入库；
- 商城：`GET /api/v1/products/{id}/detail?locale=` 返回**该语言已发布**的详情（未发布回退默认语言），详情页 `v-html` 渲染；
- 译文联动：`describe` 之外，`POST /translations/{locale}/auto-translate-detail` 只翻文本节点，结构/图片/视频不动。

测试踩到的坑（已修）：
1. `Safelist.relaxed()` 自带 `img/src` 的 http/https 协议白名单，会把 `/images/...` 整段删掉 → 
   `removeProtocols("img","src",...)` + 自定义 `isSafeMediaUrl`（允许站内相对路径、http(s)、data:image|video）；
2. jsoup 默认 prettyPrint 会给 `<figcaption>` 里插换行空白 → 清洗时关闭 prettyPrint。

实测：编辑器输入 + 加粗 → 保存 → `GET /detail` 返回 `<p>…<strong>加粗内容</strong></p>`；
XSS 用例（script / onerror / javascript:）全部被清洗，正常段落保留。`RichTextSanitizerTest` 3 项通过。

### R.4 e2e 上传自清理

- `admin-pickers.spec`：上传名改成 `e2e-picker-<ts>.png`，用例结束 `DELETE /admin/v1/assets/{id}`；
- `material-variant.spec`：参考图与生成结果都删掉；
- 验证：连跑两条用例后素材总数不变（10 张），不再累积 m.png/picker.png。

### R.5 其它验证

- `make ui-audit`：40 路由 + 5 个弹窗/标签页状态（新增「详情（富文本）」），0 问题；
- `make contract-check`：后端 366 路由 / 前端 164 调用，0 未匹配；
- admin / storefront 构建通过；admin.spec / admin-pickers / material-variant 通过。

### R.6 仍未完成 / 已知限制

1. **storefront dev server（41285）当前是 Nuxt 错误页**（并行工作区的改动导致，`/` 同样 500）；
   `npm run build` 通过，详情渲染只能靠构建产物验证，浏览器验收被这个服务阻塞。
2. moondream 是英文小模型，描述偏英文；如需中文描述可在供应商里换成更强的远端视觉模型（无需改代码）。
3. 迁移号与另一并行工作区多次撞号（V134/V135），最终约定：视觉标记=V134，review_media=V136。


### R.7 收尾修复（10-29 10:00-10:25）

1. **已删除素材仍出现在列表 → 缩略图 404**：`/admin/v1/assets`（BaseCrud）不过滤 `deleted_at`。
   素材库、AI 素材生成、视频分镜三处都改走 `/admin/v1/assets/search`（过滤软删除 + 支持标签/关键词/分页）。
   素材库同时补上服务端分页、标签筛选、关键词搜索、上传时间列。
2. **契约检查漏判全限定注解**：`@org.springframework.web.bind.annotation.GetMapping("/vision")` 之前匹配不到，
   脚本改为允许包名前缀；现在 368 路由 / 168 调用 / 0 未匹配。
3. 视频素材不再用 `<img>` 渲染（浏览器会 404 刷错），改为占位标记。

最终验证：ui-audit 40 路由 0 问题；contract-check 368/168/0；admin 构建通过；
admin + admin-pickers e2e 通过；后端 18 个单测通过；素材库 20 行 / 7 个标签 / 0 个 HTTP 错误。

