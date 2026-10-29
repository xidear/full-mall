# 前端验收清单（本轮会话反馈）

> 真实验收：Playwright + Chromium 真浏览器。本会话没有 browser MCP 工具，因此使用 Playwright 直连 Chromium。

## 1. 反馈问题类型

1. 排版基线：搜索框 label 错位、购买控件高度不一致。
2. 控件语境：语言切换与深色顶栏割裂。
3. 状态与路由：分类高亮、URL 暴露 UUID。
4. 边界状态：已经到底了、清空筛选、空态。
5. 信息投影：法律页 hash/版本/生效时间；内部字段外泄。
6. 配置与品牌：域名、媒体基址、存储服务商、demo/prod、版权。
7. 结算身份与合规：先登录、回跳、自动建号、手机号以外身份、地区政策。
8. 权限状态：定位授权与拒绝后的重试。
9. 错误呈现：用户模糊、管理员详细、分级告警。
10. 商品详情：购买区控件高度、详情/参数/评价/售后分区。
11. 多端：PC/移动端主题、语言、地区；Admin/门店/供应商/客服。
12. 演示数据：商品太少，无法测分页/多规格。
13. 验证一致性：前端/后端/数据库规则不统一。
14. 资金/积分原子性：并发扣减、回退、过期、提现。
15. 工程约束：写入 AGENTS.md。

## 2. 验收步骤

### 商品列表
打开 /products?category=BEAUTY：
- 搜索框前无可见“搜索商品”文字；
- 仅“美妆个护”高亮，aria-current=page；
- 底部只有“已展示全部 N 件商品”，无“清空筛选”；
- 商品足以触发加载更多并最终到终态。

### 语言与模板
- 深色 topbar 的语言控件为透明胶囊，非白底；
- 切 English 后分类为 Beauty & care，html lang=en；
- ?theme=industrial|fresh|minimal 背景/文字/导航整体切换；
- 移动端切换机械模板后，购物车/详情主题保持一致。

### 法律页
/legal/PICKUP：
- 无“内容哈希”、无 ZH-CN、无原始类型代码；
- 显示现行版本/修订版本/生效时间；
- 未来生效显示“将于…”；过期显示“已失效”；
- 无英文译本时显示显式回退提示。

### 结算与定位
- 未登录进入 /checkout 显示登录提示；
- 点登录后下单跳 /login?redirect=/checkout，登录后回跳；
- CN 显示自动建号披露；
- 切门店自提时，定位未授权出现“重新获取位置/允许定位”；
- 不显示请求路径、HTTP 状态码、堆栈。

### 商品详情
- 数量框/加购/立即购买/收藏均为 44px；
- 有商品详情/规格参数/用户评价/售后保障四个 tab；
- 规格表有分类、库存、批次/效期、SKU 选项、规格编号；
- 图集与主图同列，不挤走购买区。

### 页脚与图片
- prod 不出现演示文案；demo 才显示；
- 商品 API imageUrl 为绝对 http(s) URL；
- 改数据库 media_public_base_url / site_domain 后输出变化；
- 跨域 CDN 图片不裂图。

### 验证与资金
- 修改 docs/validation.schema.json 后执行 npm run generate:validation；
- 运行 mvn -Dtest=ValidationRulesContractTest test 通过；
- 并发积分兑换/回退/过期/提现：无负余额、无重复流水、无部分写入。

## 3. 本次真实浏览器结果

### PC（29 项，全部通过）
搜索 label 不可见、占位符正确、分类高亮 + aria-current、语言控件透明、美妆 6 件、ListEnd 无清空筛选、法律页无 hash + 版本/生效、详情 4 tab、控件 44px、规格表 5 行、评价 tab、未登录结算跳登录、自动建号披露、定位重试 CTA、三种主题颜色、英文分类、html lang=en、prod 页脚无演示文案、英文法律回退、console error 0。

截图：/tmp/accept-products.png、/tmp/accept-legal.png、/tmp/accept-product.png、/tmp/accept-login.png、/tmp/accept-theme-*.png。

### Mobile H5（390×844，全部通过）
商品加载、主题切换（#f5f5f5 -> #10161b）、币种格式化、console error 0。
截图：/tmp/accept-mobile.png。

### 后端
- compile 通过；
- ValidationRulesContractTest 通过；
- MemberPointsServiceTest / PointsExpiryTest / PointsFifoTest / StoredValueServiceTest 共 12 用例通过；
- 佣金/提现测试受并行改动 test-compile 缺失类阻塞，未完成全量执行。

## 4. 尚未做浏览器验收
Admin Web、门店工作台、供应商工作台、客服工作台：构建通过，但浏览器 E2E 需要对应角色与种子数据，建议下一轮补跑。

## 5. 本次端口
API 41290（隔离库 fullmall_ui_verify）、Storefront 41296（prod）、Mobile H5 41286。正式环境使用 41280/41285/41286。
