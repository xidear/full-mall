<script setup lang="ts">
import HelpDrawer from './components/HelpDrawer.vue'
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from './api'
import { auth, changePassword, logout } from './auth'

const route = useRoute()
const router = useRouter()
router.afterEach((to) => { if (to.name !== 'login') { void loadSetupStatus() } })
const passwordDialog = ref(false)
const passwordSaving = ref(false)
const passwordError = ref('')
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const setupMissing = ref<string[]>([])
// 登录后进入任意页面时刷新一次（避免在路由守卫里做请求）

// 首次配置提示：只提示不阻断（可稍后继续）
interface SetupItem { key: string; configured: boolean; hint: string }
async function loadSetupStatus() {
  if (route.name === 'login' || !auth.token) {
    return
  }
  try {
    const status = await api.get<{ completed: boolean; items: SetupItem[] }>('/admin/v1/setup/status')
    setupMissing.value = status.items.filter(item => !item.configured).map(item => item.hint)
  } catch {
    // 提示失败不影响使用，也不弹错误（避免登录后骚扰）
    setupMissing.value = []
  }
}
const isLoginPage = computed(() => route.name === 'login')
const activeMenu = computed(() => {
  if (route.path.startsWith('/products')) return '/products'
  return route.path
})

const menuGroups = [
  { key: 'workbench', label: '工作台', items: [
    { path: '/', label: '经营总览', roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'catalog', label: '商品', items: [
    { path: '/products', label: '商品管理', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/sku-matrix', label: 'SKU 规格矩阵', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/price-rule', label: '规格改价', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/brands', label: '品牌管理', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/reviews', label: '评价审核', roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'trade', label: '交易', items: [
    { path: '/orders', label: '订单管理', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/after-sales', label: '售后处理', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/pos', label: '门店收银', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/warehouse', label: '仓储作业', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/suppliers', label: '供应商', roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'stock', label: '库存', items: [
    { path: '/inventory', label: '库存与批次', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/expiring', label: '临期商品', roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'marketing', label: '营销', items: [
    { path: '/promotions', label: '促销管理', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/coupons', label: '优惠券', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/pos-members', label: '会员与优惠券', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/customers', label: '客户管理', roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'finance', label: '财务', items: [
    { path: '/costs', label: '成本查询', roles: ['ADMIN'] },
    { path: '/exports', label: '导出中心', roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'system', label: '系统', items: [
    { path: '/notifications', label: '通知中心', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/ai', label: 'AI 客服', roles: ['ADMIN'] },
    { path: '/themes', label: '主题配置', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/staff', label: '员工与权限', roles: ['ADMIN'] },
    { path: '/permissions', label: '权限矩阵', roles: ['ADMIN'] },
    { path: '/license', label: '许可证', roles: ['ADMIN'] },
    { path: '/audit', label: '操作审计', roles: ['ADMIN'] },
    { path: '/materials', label: '素材库', roles: ['ADMIN', 'OPERATOR'] },
    { path: '/ai-actions', label: 'AI 变更审批', roles: ['ADMIN'] },
    { path: '/integrations', label: '第三方服务', roles: ['ADMIN'] },
    { path: '/settings', label: '系统设置', roles: ['ADMIN'] }
  ] }
]
const visibleGroups = computed(() => menuGroups
  .map(group => ({
    ...group,
    items: group.items.filter(item => !auth.user || item.roles.includes(auth.user.role))
  }))
  .filter(group => group.items.length > 0))

async function handleLogout() {
  await logout()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}

function openPasswordDialog() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordError.value = ''
  passwordDialog.value = true
}

async function submitPassword() {
  passwordError.value = ''
  if (passwordForm.newPassword.length < 8) {
    passwordError.value = '新密码至少 8 位'
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }
  passwordSaving.value = true
  try {
    await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    passwordDialog.value = false
    ElMessage.success('密码已修改，请重新登录')
    await router.replace('/login')
  } catch (cause) {
    passwordError.value = cause instanceof Error ? cause.message : '修改密码失败'
  } finally {
    passwordSaving.value = false
  }
}
</script>

<template>
  <router-view v-if="isLoginPage" />

  <el-container v-else class="admin-shell">
    <el-aside width="236px" class="admin-aside">
      <div class="admin-brand">
        <span class="admin-brand-mark">FM</span>
        <div>
          <strong>Full Mall</strong>
          <small>运营后台</small>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="admin-menu">
        <template v-for="group in visibleGroups" :key="group.key">
          <div class="admin-menu-group">{{ group.label }}</div>
          <el-menu-item v-for="menu in group.items" :key="menu.path" :index="menu.path">
            {{ menu.label }}
          </el-menu-item>
        </template>
      </el-menu>
      <div class="admin-aside-footer">
        <div class="admin-user">
          <strong>{{ auth.user?.displayName || '未登录' }}</strong>
          <span>@{{ auth.user?.username || '-' }}</span>
        </div>
        <el-button link class="logout-button" @click="handleLogout">退出</el-button>
      </div>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div>
          <h1>{{ route.meta.title }}</h1>
          <p>商品、订单、库存、门店与主题配置统一管理。</p>
        </div>
        <div class="header-actions">
          <el-button @click="openPasswordDialog">修改密码</el-button>
          <HelpDrawer />
          <el-button type="primary" plain @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <el-alert
          v-if="setupMissing.length"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 12px"
          title="首次配置尚未完成">
          <template #default>
            <span>还差 {{ setupMissing.length }} 项（如 {{ setupMissing[0] }}）。可稍后继续，不影响使用。</span>
            <el-button link type="primary" @click="$router.push('/setup')">去完成配置</el-button>
          </template>
        </el-alert>
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="passwordDialog" title="修改密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-alert v-if="passwordError" :title="passwordError" type="error" show-icon :closable="false" />
      </el-form>
      <template #footer>
        <el-button @click="passwordDialog = false">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="submitPassword">保存</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>