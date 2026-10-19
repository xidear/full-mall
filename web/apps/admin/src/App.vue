<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { auth, changePassword, logout } from './auth'

const route = useRoute()
const router = useRouter()
const passwordDialog = ref(false)
const passwordSaving = ref(false)
const passwordError = ref('')
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const isLoginPage = computed(() => route.name === 'login')
const activeMenu = computed(() => {
  if (route.path.startsWith('/products')) return '/products'
  return route.path
})

const allMenus = [
  { path: '/', label: '经营总览', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/products', label: '商品管理', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/orders', label: '订单管理', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/after-sales', label: '售后处理', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/promotions', label: '促销管理', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/pos', label: '门店收银', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/pos-members', label: '会员与优惠券', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/suppliers', label: '供应商', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/warehouse', label: '仓储作业', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/customers', label: '客户管理', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/inventory', label: '库存与批次', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/notifications', label: '通知中心', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/themes', label: '主题配置', roles: ['ADMIN', 'OPERATOR'] },
  { path: '/staff', label: '员工与权限', roles: ['ADMIN'] },
  { path: '/permissions', label: '权限矩阵', roles: ['ADMIN'] },
  { path: '/settings', label: '系统设置', roles: ['ADMIN'] }
]
const menus = computed(() => allMenus.filter(menu => !auth.user || menu.roles.includes(auth.user.role)))

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
        <el-menu-item v-for="menu in menus" :key="menu.path" :index="menu.path">
          {{ menu.label }}
        </el-menu-item>
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
          <el-button type="primary" plain @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
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