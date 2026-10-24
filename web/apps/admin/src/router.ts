import { createRouter, createWebHistory } from 'vue-router'
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
import PriceRuleView from './views/PriceRuleView.vue'
import ExpiringView from './views/ExpiringView.vue'
import IntegrationsView from './views/IntegrationsView.vue'
import MaterialView from './views/MaterialView.vue'
import AiActionsView from './views/AiActionsView.vue'
import PosPromotionsView from './views/PosPromotionsView.vue'
import TranslationsView from './views/TranslationsView.vue'
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

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { title: '登录' } },
    { path: '/', name: 'dashboard', component: DashboardView, meta: { title: '经营总览' } },
    { path: '/products', name: 'products', component: ProductListView, meta: { title: '商品管理' } },
    { path: '/products/new', name: 'product-new', component: ProductEditView, meta: { title: '新建商品' } },
    { path: '/inventory', name: 'inventory', component: InventoryView, meta: { title: '库存与批次' } },
    { path: '/notifications', name: 'notifications', component: NotificationsView, meta: { title: '通知中心' } },
    { path: '/themes', name: 'themes', component: ThemeStudioView, meta: { title: '主题配置' } },
    { path: '/orders', name: 'orders', component: OrdersView, meta: { title: '订单管理' } },
    { path: '/pos', name: 'pos', component: PosReportsView, meta: { title: '门店收银' } },
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
    { path: '/sku-matrix', name: 'sku-matrix', component: SkuMatrixView, meta: { title: 'SKU 规格矩阵' } },
    { path: '/license', name: 'license', component: LicenseView, meta: { title: '许可证' } },
    { path: '/price-rule', name: 'price-rule', component: PriceRuleView, meta: { title: '规格改价' } },
    { path: '/expiring', name: 'expiring', component: ExpiringView, meta: { title: '临期商品' } },
    { path: '/integrations', name: 'integrations', component: IntegrationsView, meta: { title: '第三方服务' } },
    { path: '/materials', name: 'materials', component: MaterialView, meta: { title: '素材库' } },
    { path: '/ai-actions', name: 'ai-actions', component: AiActionsView, meta: { title: 'AI 变更审批' } },
    { path: '/pos-promotions', name: 'pos-promotions', component: PosPromotionsView, meta: { title: 'POS 专属促销' } },
    { path: '/translations', name: 'translations', component: TranslationsView, meta: { title: '商品译文' } },
    { path: '/faq', name: 'faq', component: FaqView, meta: { title: 'FAQ 知识库' } },
    { path: '/ai-media', name: 'ai-media', component: AiMediaView, meta: { title: 'AI 素材生成' } },
    { path: '/setup', name: 'setup', component: SetupView, meta: { title: '首次配置' } },
    { path: '/audit', name: 'audit', component: AuditLogView, meta: { title: '操作审计' } },
    { path: '/reviews', name: 'reviews', component: ReviewsView, meta: { title: '评价审核' } },
    { path: '/customers', name: 'customers', component: CustomersView, meta: { title: '客户管理' } },
    { path: '/staff', name: 'staff', component: StaffView, meta: { title: '员工与权限' } },
    { path: '/settings', name: 'settings', component: SettingsView, meta: { title: '系统设置' } }
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
    } catch {
      clearAuth()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
  return true
})

export default router