import { createRouter, createWebHistory } from 'vue-router'
import { auth, loadMe, clearAuth } from './auth'
import LoginView from './views/LoginView.vue'
import DashboardView from './views/DashboardView.vue'
import ProductListView from './views/ProductListView.vue'
import ProductEditView from './views/ProductEditView.vue'
import InventoryView from './views/InventoryView.vue'
import OrdersView from './views/OrdersView.vue'
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