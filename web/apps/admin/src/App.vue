<script setup lang="ts">
import HelpDrawer from './components/HelpDrawer.vue'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useLocalStorage } from '@vueuse/core'
import {
  Bell, Box, Brush, ChatDotRound, Coin, Collection, Connection, CreditCard, DataBoard,
  Discount, Document, Download, Expand, Fold, Goods, Grid, Key, List, MagicStick,
  Monitor, OfficeBuilding, Odometer, Picture, Present, PriceTag, Service, Setting, Ticket,
  Tickets, Timer, UserFilled, Wallet
} from '@element-plus/icons-vue'
import { VALIDATION, formatDateTime } from '@fullmall/api-client'
import { api } from './api'
import { auth, changePassword, logout } from './auth'

const route = useRoute()
const router = useRouter()
// 首次配置提示放 afterEach 而不是路由守卫：守卫里 await 会拖慢每次跳转，而“缺配置”只是提醒，
// 晚出现可接受、失败也不该阻断导航（loadSetupStatus 内部失败时静默清空）。
router.afterEach((to) => {
  if (to.name !== 'login') {
    void loadSetupStatus()
  }
  // 后台是"侧栏固定 + 主区独立滚动"：换页必须把主区滚回顶部，
  // 否则从长页面点菜单进入短页面会看到一片空白（要手动往上拖，拖上去左侧菜单又看不到）。
  void nextTick(() => {
    document.querySelector('.admin-main')?.scrollTo({ top: 0 })
    window.scrollTo({ top: 0 })
  })
})
const passwordDialog = ref(false)
const passwordSaving = ref(false)
const passwordError = ref('')
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 菜单折叠：状态持久化，刷新后保持（后台长时间使用，不该每次重新展开）
const collapsed = useLocalStorage('fullmall.admin.menu-collapsed', false)
const asideWidth = computed(() => (collapsed.value ? '72px' : '236px'))

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
interface NotificationRow {
  id: string
  title: string
  eventType: string
  createdAt: string
  /** 未读 = readAt 为空（后端只存已读时间，没有布尔字段）。 */
  readAt?: string | null
}

/**
 * 通知提醒走轮询（30s）+ 页面重新可见时立即刷新，而不是 WebSocket。
 *
 * <p>理由：仓库里已有的 WebSocket 只服务于客服会话（SupportWebSocketHandler，按会话订阅）；
 * 后台通知是"每人一份、低频、丢一条不影响业务"的场景，用 WS 要额外处理鉴权、心跳、
 * 多实例广播与断线重连，收益不抵成本。真需要"秒级强提醒"时再上 SSE（Spring SseEmitter，
 * 一个方向、几乎零协议成本），仍然不需要 WS 的双向能力。
 */
const NOTIFY_POLL_MS = 30_000
const unreadCount = ref(0)
const recentNotifications = ref<NotificationRow[]>([])
const notifyError = ref('')
let notifyTimer: ReturnType<typeof setInterval> | undefined

async function loadNotifications() {
  if (!auth.token) return
  notifyError.value = ''
  try {
    const [count, list] = await Promise.all([
      api.get<{ unread: number }>('/admin/v1/notifications/unread-count'),
      api.get<NotificationRow[]>('/admin/v1/notifications', { limit: 6, recipientType: 'STAFF' })
    ])
    unreadCount.value = count.unread ?? 0
    recentNotifications.value = list
  } catch (cause) {
    // 明确提示，不静默当作"没有通知"
    notifyError.value = '通知加载失败：' + (cause as Error).message
  }
}

async function openNotification(item: NotificationRow) {
  try {
    await api.post('/admin/v1/notifications/' + item.id + '/read')
  } catch (cause) {
    ElMessage.error('标记已读失败：' + (cause as Error).message)
    return
  }
  await loadNotifications()
  await router.push('/notifications')
}

function onVisibilityChange() {
  if (document.visibilityState === 'visible') void loadNotifications()
}

/**
 * 实时通道：EventSource 发不出 Authorization 头，所以先换一次性 token 再连流。
 * 流里只推"未读数变化"；连接失败/断开就静默降级回 30s 轮询（轮询一直在跑，不是兜底谎言）。
 */
let notifyStream: EventSource | undefined

async function openNotificationStream() {
  if (!auth.token || typeof EventSource === 'undefined') return
  try {
    const issued = await api.post<{ token: string }>('/admin/v1/notifications/stream-token')
    const base = (import.meta.env.VITE_API_BASE as string | undefined) ?? ''
    notifyStream = new EventSource(base + '/api/v1/notifications/stream?token=' + issued.token)
    notifyStream.addEventListener('notifications', () => { void loadNotifications() })
    notifyStream.onerror = () => {
      notifyStream?.close()
      notifyStream = undefined
    }
  } catch {
    // 拿不到流 token 不影响使用：轮询继续，页面上不会出现"看起来实时其实没连上"
  }
}

onMounted(() => {
  void loadNotifications()
  notifyTimer = setInterval(() => { void loadNotifications() }, NOTIFY_POLL_MS)
  document.addEventListener('visibilitychange', onVisibilityChange)
  void openNotificationStream()
})

onUnmounted(() => {
  if (notifyTimer) clearInterval(notifyTimer)
  notifyStream?.close()
  document.removeEventListener('visibilitychange', onVisibilityChange)
})

const isLoginPage = computed(() => route.name === 'login')
const activeMenu = computed(() => {
  if (route.path.startsWith('/products')) return '/products'
  return route.path
})

/**
 * 菜单：分组带图标，条目带图标；分组用 el-sub-menu，折叠时显示分组图标 + 悬浮子菜单。
 * 商品译文已并入商品管理（编辑页「译文」标签），不再是独立模块。
 */
const menuGroups = [
  { key: 'workbench', label: '工作台', icon: DataBoard, items: [
    { path: '/', label: '经营总览', icon: Odometer, roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'catalog', label: '商品', icon: Goods, items: [
    { path: '/products', label: '商品管理', icon: Goods, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/sku-matrix', label: '规格矩阵模板', icon: Grid, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/brands', label: '品牌管理', icon: Collection, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/reviews', label: '评价审核', icon: ChatDotRound, roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'trade', label: '交易', icon: List, items: [
    { path: '/orders', label: '订单管理', icon: Tickets, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/after-sales', label: '售后处理', icon: Service, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/pos', label: '门店收银', icon: CreditCard, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/warehouse', label: '仓储作业', icon: Box, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/suppliers', label: '供应商', icon: OfficeBuilding, roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'stock', label: '库存', icon: Box, items: [
    { path: '/inventory', label: '库存与批次', icon: Box, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/expiring', label: '临期商品', icon: Timer, roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'marketing', label: '营销', icon: Present, items: [
    { path: '/promotions', label: '促销管理', icon: Present, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/pos-promotions', label: 'POS 专属促销', icon: Discount, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/coupons', label: '优惠券', icon: Ticket, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/pos-members', label: '会员与优惠券', icon: UserFilled, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/customers', label: '客户管理', icon: UserFilled, roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'finance', label: '财务', icon: Wallet, items: [
    { path: '/costs', label: '成本查询', icon: Coin, roles: ['ADMIN'] },
    { path: '/exports', label: '导出中心', icon: Download, roles: ['ADMIN', 'OPERATOR'] }
  ] },
  { key: 'ops', label: '运维', icon: Monitor, items: [
    { path: '/ops', label: '运维中心', icon: Monitor, roles: ['ADMIN'] }
  ] },
  { key: 'system', label: '系统', icon: Setting, items: [
    { path: '/notifications', label: '通知中心', icon: Bell, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/ai', label: 'AI 客服', icon: Service, roles: ['ADMIN'] },
    { path: '/themes', label: '主题配置', icon: Brush, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/staff', label: '员工与权限', icon: UserFilled, roles: ['ADMIN'] },
    { path: '/permissions', label: '权限矩阵', icon: Key, roles: ['ADMIN'] },
    { path: '/license', label: '许可证', icon: Document, roles: ['ADMIN'] },
    { path: '/audit', label: '操作审计', icon: Tickets, roles: ['ADMIN'] },
    { path: '/materials', label: '素材库', icon: Picture, roles: ['ADMIN', 'OPERATOR'] },
    // 视频分镜不再是独立菜单：它是「AI 素材生成」里的一个标签（同一条流水线、同样产出素材）
    { path: '/ai-media', label: 'AI 素材生成', icon: MagicStick, roles: ['ADMIN', 'OPERATOR'] },
    { path: '/integrations', label: '第三方服务', icon: Connection, roles: ['ADMIN'] },
    { path: '/markets', label: '市场配置', icon: Setting, roles: ['ADMIN'] },
    { path: '/shipping-rates', label: '运费档位', icon: Setting, roles: ['ADMIN'] },
    { path: '/tax-rules', label: '税率规则', icon: Setting, roles: ['ADMIN'] },
    { path: '/settings', label: '系统设置', icon: Setting, roles: ['ADMIN'] }
  ] }
]
// 默认全部展开：后台是"常用入口直达"的形态，分组只是归类，不应该让运营每次额外点开。
//
// 做法：等登录态就绪后再渲染 el-menu，用 default-openeds 一次性初始化。
// 不要用 menu.open() 手动补开：Element Plus 的展开是折叠动画，连续对 8 个分组 open()
// 会把动画打断在半途，留下"位置占着、内容看不见"的菜单（2026-10-28 用户截图现象）。
const openGroupKeys = menuGroups.map(group => group.key)
// 必须等 auth.user 就绪再渲染菜单：初始化时若可见项为空，default-openeds 会落空，
// 表现为分组全部收起、菜单看起来是空的。
const menuReady = computed(() => Boolean(auth.user))

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
  // 阈值 8 与 VALIDATION.passwordMinLength 重复（AGENTS.md §9 要求单一来源）；若 schema 调大，
  // 这里会先放行再由后端拒绝，而提示文案读的是常量，会出现“提示 N 位、8 位却放行”的错觉。
  if (passwordForm.newPassword.length < 8) {
    passwordError.value = `新密码至少 ${VALIDATION.passwordMinLength} 位`
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
    <el-aside :width="asideWidth" class="admin-aside">
      <div class="admin-brand" :class="{ 'is-collapsed': collapsed }">
        <span class="admin-brand-mark">FM</span>
        <div v-if="!collapsed">
          <strong>Full Mall</strong>
          <small>运营后台</small>
        </div>
      </div>
      <el-menu
        v-if="menuReady"
        :default-active="activeMenu"
        :default-openeds="openGroupKeys"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        class="admin-menu"
      >
        <el-sub-menu v-for="group in visibleGroups" :key="group.key" :index="group.key">
          <template #title>
            <el-icon><component :is="group.icon" /></el-icon>
            <span>{{ group.label }}</span>
          </template>
          <el-menu-item v-for="menu in group.items" :key="menu.path" :index="menu.path">
            <el-icon><component :is="menu.icon" /></el-icon>
            <template #title>{{ menu.label }}</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
      <div class="admin-aside-footer" :class="{ 'is-collapsed': collapsed }">
        <div v-if="!collapsed" class="admin-user">
          <strong>{{ auth.user?.displayName || '未登录' }}</strong>
          <span>@{{ auth.user?.username || '-' }}</span>
        </div>
        <el-button v-if="!collapsed" link class="logout-button" @click="handleLogout">退出</el-button>
      </div>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-title">
          <el-button
            text
            class="collapse-toggle"
            :title="collapsed ? '展开菜单' : '收起菜单'"
            data-testid="menu-collapse-toggle"
            @click="collapsed = !collapsed"
          >
            <el-icon :size="18"><component :is="collapsed ? Expand : Fold" /></el-icon>
          </el-button>
          <div>
            <h1>{{ route.meta.title }}</h1>
            <p>商品、订单、库存、门店与主题配置统一管理。</p>
          </div>
        </div>
        <div class="header-actions">
          <!-- 未读提醒：轮询 30s + 页面重新可见时立即刷新（不用 WebSocket，理由见 notifyPollInterval） -->
          <el-popover placement="bottom-end" :width="380" trigger="click" @show="loadNotifications">
            <template #reference>
              <el-badge :value="unreadCount" :max="99" :hidden="unreadCount === 0" class="notify-badge">
                <el-button circle :aria-label="unreadCount > 0 ? '未读通知 ' + unreadCount + ' 条' : '通知'">
                  <el-icon><Bell /></el-icon>
                </el-button>
              </el-badge>
            </template>
            <div class="notify-list">
              <p v-if="notifyError" class="notify-empty">{{ notifyError }}</p>
              <button v-for="item in recentNotifications" v-else :key="item.id"
                      class="notify-item" :class="{ 'is-unread': !item.readAt }"
                      @click="openNotification(item)">
                <span class="notify-title">{{ item.title }}</span>
                <span class="notify-meta">{{ item.eventType }} · {{ formatDateTime(item.createdAt) }}</span>
              </button>
              <p v-if="!notifyError && recentNotifications.length === 0" class="notify-empty">暂无通知</p>
              <div class="notify-footer">
                <el-button link type="primary" @click="$router.push('/notifications')">查看全部</el-button>
              </div>
            </div>
          </el-popover>
          <el-button @click="openPasswordDialog">修改密码</el-button>
          <HelpDrawer />
          <el-button type="primary" plain @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <!-- 首次配置提醒只提示不阻断：缺支付/短信配置时后台仍要能用，否则进不来把配置补完。 -->
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

<style scoped>
.notify-badge {
  margin-right: 8px;
}

.notify-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 360px;
  overflow-y: auto;
}

.notify-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.notify-item:hover {
  background: #f6f4ee;
}

.notify-item.is-unread .notify-title {
  font-weight: 600;
}

.notify-title {
  color: #1f2421;
  font-size: 13px;
}

.notify-meta {
  color: #8a8f8a;
  font-size: 11px;
}

.notify-empty {
  margin: 8px 0;
  color: #8a8f8a;
  font-size: 12px;
  text-align: center;
}

.notify-footer {
  margin-top: 6px;
  border-top: 1px solid #ece9e2;
  padding-top: 6px;
  text-align: right;
}
</style>
