/**
 * 路由清单被两处消费：App.vue 菜单按 path 指向这里；scripts/check-help-coverage.py 正则扫描本文件，
 * 新增页面必须同步补 help/catalog.ts 条目（redirect 路由也算一页：当前 /price-rule、/ops 缺条目，检查即失败）。
 */
import { createRouter, createWebHistory } from 'vue-router'
import { ApiError } from '@fullmall/api-client'
import { auth, loadMe, clearAuth } from './auth'
import LoginView from './views/LoginView.vue'
import DashboardView from './views/DashboardView.vue'
import ProductListView from './views/ProductListView.vue'
import ProductEditView from './views/ProductEditView.vue'
import InventoryView from './views/InventoryView.vue'
import OrdersView from './views/OrdersView.vue'
import PosReportsView from './views/PosReportsView.vue'
import PosMembersView from './views/PosMembersView.vue'
import SuppliersView from './views/SuppliersView.vue'
import WarehouseView from './views/WarehouseView.vue'
import PermissionsView from './views/PermissionsView.vue'
import AfterSalesView from './views/AfterSalesView.vue'
import CouponsView from './views/CouponsView.vue'
import AiView from './views/AiView.vue'
import BrandsView from './views/BrandsView.vue'
import CostsView from './views/CostsView.vue'
import ExportsView from './views/ExportsView.vue'
import SkuMatrixView from './views/SkuMatrixView.vue'
import LicenseView from './views/LicenseView.vue'
import ExpiringView from './views/ExpiringView.vue'
import IntegrationsView from './views/IntegrationsView.vue'
import MaterialView from './views/MaterialView.vue'
import PosPromotionsView from './views/PosPromotionsView.vue'
import FaqView from './views/FaqView.vue'
import AiMediaView from './views/AiMediaView.vue'
import SetupView from './views/SetupView.vue'
import AuditLogView from './views/AuditLogView.vue'
import ReviewsView from './views/ReviewsView.vue'
import PromotionsView from './views/PromotionsView.vue'
import ThemeStudioView from './views/ThemeStudioView.vue'
import NotificationsView from './views/NotificationsView.vue'
import CustomersView from './views/CustomersView.vue'
import StaffView from './views/StaffView.vue'
import SettingsView from './views/SettingsView.vue'
import OpsView from './views/OpsView.vue'
import MarketsView from './views/MarketsView.vue'
import ShippingRatesView from './views/ShippingRatesView.vue'
import TaxRulesView from './views/TaxRulesView.vue'
import StoresView from './views/StoresView.vue'

// history 模式依赖服务端把未知路径回落 index.html（dev 靠 Vite、生产靠 nginx try_files）；
// 少了这条回落，用户在 /orders 按 F5 得到的是 404 而不是页面。
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { title: '登录' } },
    { path: '/', name: 'dashboard', component: DashboardView, meta: { title: '经营总览' } },
    // 下面两个 /products 子路由不在菜单里，侧边栏高亮靠 App.vue 的 activeMenu 按前缀兜住。
    { path: '/products', name: 'products', component: ProductListView, meta: { title: '商品管理' } },
    { path: '/products/new', name: 'product-new', component: ProductEditView, meta: { title: '新建商品' } },
    { path: '/products/:id/edit', name: 'product-edit', component: ProductEditView, meta: { title: '编辑商品' } },
    { path: '/inventory', name: 'inventory', component: InventoryView, meta: { title: '库存与批次' } },
    { path: '/notifications', name: 'notifications', component: NotificationsView, meta: { title: '通知中心' } },
    { path: '/themes', name: 'themes', component: ThemeStudioView, meta: { title: '主题配置' } },
    { path: '/orders', name: 'orders', component: OrdersView, meta: { title: '订单管理' } },
    { path: '/pos', name: 'pos', component: PosReportsView, meta: { title: '门店收银' } },
    { path: '/stores', name: 'stores', component: StoresView, meta: { title: '门店管理' } },
    { path: '/pos-members', name: 'pos-members', component: PosMembersView, meta: { title: '会员与优惠券' } },
    { path: '/suppliers', name: 'suppliers', component: SuppliersView, meta: { title: '供应商与凭证' } },
    { path: '/warehouse', name: 'warehouse', component: WarehouseView, meta: { title: '仓储作业' } },
    { path: '/permissions', name: 'permissions', component: PermissionsView, meta: { title: '权限矩阵' } },
    { path: '/after-sales', name: 'after-sales', component: AfterSalesView, meta: { title: '售后处理' } },
    { path: '/promotions', name: 'promotions', component: PromotionsView, meta: { title: '促销管理' } },
    { path: '/coupons', name: 'coupons', component: CouponsView, meta: { title: '优惠券' } },
    { path: '/ai', name: 'ai', component: AiView, meta: { title: 'AI 客服' } },
    { path: '/brands', name: 'brands', component: BrandsView, meta: { title: '品牌管理' } },
    { path: '/costs', name: 'costs', component: CostsView, meta: { title: '成本查询' } },
    { path: '/exports', name: 'exports', component: ExportsView, meta: { title: '导出中心' } },
    { path: '/sku-matrix', name: 'sku-matrix', component: SkuMatrixView, meta: { title: '规格矩阵模板' } },
    { path: '/license', name: 'license', component: LicenseView, meta: { title: '许可证' } },
    // 规格改价已改为商品列表里的弹窗（作用域是单个商品）；旧地址重定向
    { path: '/price-rule', redirect: { path: '/products' } },
    { path: '/expiring', name: 'expiring', component: ExpiringView, meta: { title: '临期商品' } },
    { path: '/integrations', name: 'integrations', component: IntegrationsView, meta: { title: '第三方服务' } },
    { path: '/materials', name: 'materials', component: MaterialView, meta: { title: '素材库' } },
    { path: '/pos-promotions', name: 'pos-promotions', component: PosPromotionsView, meta: { title: 'POS 专属促销' } },
    // 商品译文已并入商品管理（编辑页「译文」标签）；旧地址重定向，避免收藏夹失效
    { path: '/translations', redirect: { path: '/products' } },
    { path: '/faq', name: 'faq', component: FaqView, meta: { title: 'FAQ 知识库' } },
    { path: '/ai-media', name: 'ai-media', component: AiMediaView, meta: { title: 'AI 素材生成' } },
    // 视频分镜已并入「AI 素材生成」的第二个标签；旧地址重定向并带上 tab，收藏夹/文档里的链接不失效
    { path: '/ai-storyboard', redirect: () => ({ path: '/ai-media', query: { tab: 'storyboard' } }) },
    { path: '/setup', name: 'setup', component: SetupView, meta: { title: '首次配置' } },
    { path: '/audit', name: 'audit', component: AuditLogView, meta: { title: '操作审计' } },
    { path: '/reviews', name: 'reviews', component: ReviewsView, meta: { title: '评价审核' } },
    { path: '/customers', name: 'customers', component: CustomersView, meta: { title: '客户管理' } },
    { path: '/staff', name: 'staff', component: StaffView, meta: { title: '员工与权限' } },
    { path: '/markets', name: 'markets', component: MarketsView, meta: { title: '市场配置' } },
    { path: '/shipping-rates', name: 'shipping-rates', component: ShippingRatesView, meta: { title: '运费档位' } },
    { path: '/tax-rules', name: 'tax-rules', component: TaxRulesView, meta: { title: '税率规则' } },
    { path: '/settings', name: 'settings', component: SettingsView, meta: { title: '系统设置' } },
    { path: '/ops', name: 'ops', component: OpsView, meta: { title: '运维中心' } }
  ]
})

router.beforeEach(async to => {
  if (to.name === 'login') {
    return true
  }
  if (!auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (!auth.user) {
    try {
      await loadMe()
    } catch (cause) {
      // 只有"登录态无效"才清空：网络抖动/5xx 清空登录态会让运营莫名其妙被踢出
      const status = cause instanceof ApiError ? cause.status : 0
      if (status === 401 || status === 403) {
        clearAuth()
        return { name: 'login', query: { redirect: to.fullPath } }
      }
      // 其它错误保留 token，交给页面自己的错误块提示，避免"刷新一次就被登出"
      return true
    }
  }
  return true
})

export default router