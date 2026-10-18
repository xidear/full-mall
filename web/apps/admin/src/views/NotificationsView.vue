<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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
const notifications = ref<Notification[]>([])
const loading = ref(false)
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

async function load() {
  loading.value = true
  error.value = ''
  try {
    notifications.value = await api.get<Notification[]>('/admin/v1/notifications', {
      limit: 200,
      channel: filters.channel || undefined,
      recipientType: filters.recipientType || undefined
    })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '通知加载失败'
  } finally {
    loading.value = false
  }
}

async function markRead(notification: Notification) {
  try {
    await api.post(`/admin/v1/notifications/${notification.id}/read`)
    ElMessage.success('已标记为已读')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
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
        <el-select v-model="filters.recipientType" placeholder="接收方" clearable style="width: 130px" @change="load">
          <el-option label="管理员" value="STAFF" />
          <el-option label="顾客" value="CUSTOMER" />
        </el-select>
        <el-select v-model="filters.channel" placeholder="渠道" clearable style="width: 140px" @change="load">
          <el-option label="站内通知" value="IN_APP" />
          <el-option label="短信" value="SMS" />
          <el-option label="邮件" value="EMAIL" />
          <el-option label="微信" value="WECHAT" />
          <el-option label="APP 推送" value="APP" />
        </el-select>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-table :data="notifications" stripe>
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ new Date(row.createdAt).toLocaleString('zh-CN') }}</template>
      </el-table-column>
      <el-table-column label="接收方" width="110">
        <template #default="{ row }">{{ label(recipientLabels, row.recipientType) }}</template>
      </el-table-column>
      <el-table-column prop="recipientId" label="接收人" width="140" />
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
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button v-if="!row.readAt" link type="primary" @click="markRead(row)">标记已读</el-button>
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