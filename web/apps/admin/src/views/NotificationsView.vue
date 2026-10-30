<script setup lang="ts">
import { formatDateTime } from '@fullmall/api-client'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Notification {
  id: string
  recipientType: string
  recipientId: string
  eventType: string
  channel: string
  title: string
  content: string
  status: string
  readAt?: string
  sentAt?: string
  errorMessage?: string
  createdAt: string
}

const filters = reactive({
  channel: '',
  recipientType: ''
})
/** 只看未读：纯前端过滤已加载的这批（接口没有 unread 参数，不假装有）。 */
const onlyUnread = ref(false)
const notifications = ref<Notification[]>([])
const visibleNotifications = computed(() =>
  // 未读的判据是 readAt 为空（后端没有布尔 read 字段）
  onlyUnread.value ? notifications.value.filter(item => !item.readAt) : notifications.value)
const loading = ref(false)
// 正在标记已读的通知：避免重复请求
const markingId = ref('')
const markingAll = ref(false)
const retryingId = ref('')
const error = ref('')

const channelLabels: Record<string, string> = {
  IN_APP: '站内通知',
  SMS: '短信',
  EMAIL: '邮件',
  WECHAT: '微信',
  APP: 'APP 推送'
}
const statusLabels: Record<string, string> = {
  PENDING: '待发送',
  SENT: '已发送',
  FAILED: '发送失败'
}
const eventLabels: Record<string, string> = {
  ORDER_PAID: '支付成功',
  PICKUP_READY: '备货完成',
  ORDER_SHIPPED: '订单发货',
  ORDER_COMPLETED: '订单完成'
}
const recipientLabels: Record<string, string> = {
  STAFF: '管理员',
  CUSTOMER: '顾客'
}

onMounted(load)

/**
 * 通知 → 目标页面。后端只存 eventType，跳转关系放前端一张表里，
 * 免得把路由散落到模板字符串里；没有映射的通知不给"查看"按钮（避免点了跳到不相关页面）。
 */
const eventRoutes: Record<string, string> = {
  ORDER_CREATED: '/orders',
  ORDER_PAID: '/orders',
  ORDER_CANCELLED: '/orders',
  ORDER_SHIPPED: '/orders',
  ORDER_COMPLETED: '/orders',
  PICKUP_READY: '/orders',
  PAYMENT_SUCCEEDED: '/orders',
  PAYMENT_FAILED: '/orders',
  REFUND_REQUESTED: '/after-sales',
  REFUND_COMPLETED: '/after-sales',
  STOCK_LOW: '/inventory',
  NEAR_EXPIRY: '/expiring',
  SUPPLIER_RETURN: '/suppliers'
}

function routeOf(notification: Notification): string | null {
  return eventRoutes[notification.eventType] ?? null
}

async function markAllRead() {
  if (markingAll.value) return
  markingAll.value = true
  try {
    const result = await api.post<{ marked: number }>('/admin/v1/notifications/read-all')
    ElMessage.success('已全部标记为已读（' + (result?.marked ?? 0) + ' 条）')
    await load()
  } catch (cause) {
    ElMessage.error('全部已读失败：' + (cause instanceof Error ? cause.message : ''))
  } finally {
    markingAll.value = false
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    // 后端已按"本人 + 广播"过滤；这里不再传 recipientType（客户通知不属本列表）
    notifications.value = await api.get<Notification[]>('/admin/v1/notifications', {
      limit: 200,
      channel: filters.channel || undefined
    })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '通知加载失败'
  } finally {
    loading.value = false
  }
}

// 重试投递：仅 FAILED 可重试，已 SENT 的重试会重复发短信（后端也会拒绝）
async function retryDelivery(notification: Notification) {
  if (retryingId.value) return
  retryingId.value = notification.id
  try {
    await api.post(`/admin/v1/notifications/${notification.id}/retry`)
    ElMessage.success('已重新发送')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '重试失败')
  } finally {
    retryingId.value = ''
  }
}

async function markRead(notification: Notification) {
  if (markingId.value) return
  markingId.value = notification.id
  try {
    await api.post(`/admin/v1/notifications/${notification.id}/read`)
    ElMessage.success('已标记为已读')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
  } finally {
    markingId.value = ''
  }
}

function label(map: Record<string, string>, key: string) {
  return map[key] || key
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>通知中心</h2>
        <p class="muted">支付、备货、发货、完成等事件会按渠道发送给顾客，并同步给运营人员。</p>
      </div>
      <div class="toolbar">
        <el-switch v-model="onlyUnread" active-text="只看未读" style="margin-right: 8px" />
        <el-select v-model="filters.channel" placeholder="渠道" clearable style="width: 140px" @change="load">
          <el-option label="站内通知" value="IN_APP" />
          <el-option label="短信" value="SMS" />
          <el-option label="邮件" value="EMAIL" />
          <el-option label="微信" value="WECHAT" />
          <el-option label="APP 推送" value="APP" />
        </el-select>
        <el-button :loading="markingAll" @click="markAllRead">全部已读</el-button>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-table :data="visibleNotifications" stripe>
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="接收方" width="110">
        <template #default="{ row }">{{ label(recipientLabels, row.recipientType) }}</template>
      </el-table-column>
      <el-table-column label="范围" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.recipientId === 'ALL'" type="info" size="small">广播</el-tag>
          <span v-else>仅本人</span>
        </template>
      </el-table-column>
      <el-table-column label="事件" width="120">
        <template #default="{ row }">{{ label(eventLabels, row.eventType) }}</template>
      </el-table-column>
      <el-table-column label="渠道" width="110">
        <template #default="{ row }">{{ label(channelLabels, row.channel) }}</template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'SENT' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'">
            {{ label(statusLabels, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="已读" width="80">
        <template #default="{ row }">{{ row.readAt ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button v-if="row.status === 'FAILED'" link type="danger"
                     :loading="retryingId === row.id" @click="retryDelivery(row)">重试</el-button>
          <el-button v-else-if="!row.readAt" link type="primary" @click="markRead(row)">标记已读</el-button>
          <!-- 只有配置了 eventType → 页面的映射才给"查看"，避免点了跳到不相关页面 -->
          <el-button v-if="routeOf(row)" link type="primary" @click="$router.push(routeOf(row)!)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !notifications.length" description="暂无通知记录" />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>