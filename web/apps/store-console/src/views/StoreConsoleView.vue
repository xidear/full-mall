<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  pickupStatusLabel,
  pickTaskStatusLabel,
  type PickTask,
  type PickTaskLine,
  type PickupDetail,
  type Store
} from '@fullmall/api-client'
import { ApiClient } from '@fullmall/api-client'

const api = new ApiClient(import.meta.env.VITE_API_BASE || 'http://localhost:8080')
const appMode = import.meta.env.VITE_APP_MODE || 'dev'
const wsBase = import.meta.env.VITE_WS_BASE || 'ws://localhost:8080'
const stores = ref<Store[]>([])
const storeId = ref('')
const tasks = ref<PickTask[]>([])
const loading = ref(false)
const error = ref('')
const success = ref('')
const showAll = ref(false)
const socketStatus = ref<'connected' | 'polling'>('polling')
let socket: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let pollTimer: ReturnType<typeof setInterval> | null = null

const pickupCode = ref('')
const pickupDetail = ref<PickupDetail | null>(null)
const pickupLoading = ref(false)

onMounted(async () => {
  await loadStores()
  await loadTasks()
  connectSocket()
  pollTimer = setInterval(() => {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      loadTasks()
    }
  }, 20_000)
})

onBeforeUnmount(() => {
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (pollTimer) clearInterval(pollTimer)
  socket?.close()
})

function connectSocket() {
  if (!storeId.value) return
  socket?.close()
  const url = `${wsBase}/ws/store?storeId=${encodeURIComponent(storeId.value)}`
  socket = new WebSocket(url)
  socket.onopen = () => {
    socketStatus.value = 'connected'
  }
  socket.onmessage = event => {
    try {
      const payload = JSON.parse(String(event.data)) as { type?: string }
      if (payload.type === 'store_task_changed') {
        loadTasks()
      }
    } catch {
      // Ignore malformed push payload; the next poll/reload will reconcile.
    }
  }
  socket.onerror = () => {
    socketStatus.value = 'polling'
  }
  socket.onclose = () => {
    socketStatus.value = 'polling'
    reconnectTimer = setTimeout(connectSocket, 5_000)
  }
}

async function loadStores() {
  try {
    stores.value = await api.get<Store[]>('/api/v1/stores')
    storeId.value = stores.value[0]?.id ?? ''
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '门店加载失败'
  }
}

async function loadTasks() {
  if (!storeId.value) return
  loading.value = true
  error.value = ''
  try {
    tasks.value = await api.get<PickTask[]>('/store/v1/pick-tasks', {
      storeId: storeId.value,
      status: showAll.value ? undefined : 'PENDING'
    })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '任务加载失败'
  } finally {
    loading.value = false
  }
}

async function onStoreChange() {
  pickupDetail.value = null
  await loadTasks()
  connectSocket()
}

function pendingLines(task: PickTask) {
  return task.lines.filter(line => line.pickedQuantity < line.quantity)
}

async function confirmPick(task: PickTask, line: PickTaskLine) {
  if (!line.suggestedBatchId) {
    error.value = '该任务缺少建议批次，请检查库存分配'
    return
  }
  error.value = ''
  success.value = ''
  try {
    await api.post(`/store/v1/pick-tasks/${task.id}/confirm`, {
      batchId: line.suggestedBatchId,
      quantity: line.quantity
    })
    success.value = `已确认拣货：${line.title || '商品'} × ${line.quantity}，批次 ${line.suggestedBatchNo || '—'}`
    await loadTasks()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '确认拣货失败'
  }
}

async function loadPickup() {
  const code = pickupCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入提货码')
    return
  }
  pickupLoading.value = true
  error.value = ''
  success.value = ''
  pickupDetail.value = null
  try {
    pickupDetail.value = appMode === 'demo'
      ? await api.get<PickupDetail>(`/mock/pickups/${code}`)
      : await api.get<PickupDetail>(`/store/v1/pickups/${code}`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '提货信息拉取失败'
  } finally {
    pickupLoading.value = false
  }
}

async function verifyPickup(method: 'PICKUP_CODE' | 'PHONE') {
  if (appMode === 'demo') {
    ElMessage.info('演示模式只展示提货信息，不执行真实核销')
    return
  }
  if (!pickupDetail.value) {
    return
  }
  const code = pickupDetail.value.pickupCode
  const value = method === 'PHONE' ? pickupDetail.value.customerPhone : code
  error.value = ''
  success.value = ''
  try {
    const updated = await api.post<{ status: string; usedCount: number; totalUses: number }>(
      `/store/v1/pickups/${code}/verify`,
      { verificationType: method, verificationValue: value }
    )
    success.value = updated.status === 'COMPLETED'
      ? '核销完成，订单已履约'
      : `本次核销成功，已使用 ${updated.usedCount}/${updated.totalUses} 次`
    pickupDetail.value = await api.get<PickupDetail>(`/store/v1/pickups/${code}`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '核销失败'
  }
}

function remainingLabel(detail: PickupDetail) {
  return `已核销 ${detail.usedCount} / 共 ${detail.totalUses} 次，剩余 ${detail.remainingUses} 次`
}
</script>

<template>
  <div class="panel">
    <div class="panel-head">
      <div>
        <h2>今日拣货任务</h2>
        <p class="muted">选择门店后直接点击商品行确认拣货，不需要输入任务 ID 或批次 ID。</p>
      </div>
      <div class="store-picker">
        <el-select v-model="storeId" placeholder="选择门店" style="width: 260px" @change="onStoreChange">
          <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
        </el-select>
        <el-button @click="loadTasks">刷新任务</el-button>
        <el-checkbox v-model="showAll" @change="loadTasks">显示全部任务</el-checkbox>
        <div class="live-status">
          <span class="dot" :class="socketStatus"></span>
          {{ socketStatus === 'connected' ? '实时推送已连接' : '轮询中' }}
        </div>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-alert v-if="success" :title="success" type="success" show-icon :closable="false" class="mb-16" />

    <el-empty v-if="!loading && !tasks.length" description="当前门店暂无拣货任务" />
    <div v-for="task in tasks" :key="task.id" class="task-card" v-loading="loading">
      <div class="task-head">
        <div>
          <strong>履约单 {{ task.fulfillmentOrderId.slice(0, 8) }}</strong>
          <span class="muted"> · 待拣 {{ pendingLines(task).length }} 行</span>
        </div>
        <el-tag :type="task.status === 'PICKED' ? 'success' : task.status === 'CANCELLED' ? 'danger' : 'warning'">
          {{ pickTaskStatusLabel(task.status) }}
        </el-tag>
      </div>

      <div v-for="line in task.lines" :key="line.id" class="pick-line">
        <div class="pick-product">
          <strong>{{ line.title || '商品' }}</strong>
          <span class="muted">数量 {{ line.quantity }}</span>
        </div>
        <div class="pick-batch">
          <span>建议批次：{{ line.suggestedBatchNo || '—' }}</span>
          <span class="muted">效期：{{ line.suggestedExpiryDate || '无' }}</span>
        </div>
        <div class="pick-action">
          <el-tag v-if="line.pickedQuantity >= line.quantity" type="success">已拣货</el-tag>
          <el-button
            v-else
            type="primary"
            :disabled="task.status === 'CANCELLED'"
            @click="confirmPick(task, line)"
          >
            确认拣货
          </el-button>
        </div>
      </div>
    </div>
  </div>

  <div class="panel">
    <div class="panel-head">
      <div>
        <h2>自提核销</h2>
        <p class="muted">先输入提货码拉取顾客和商品信息，与顾客核对后再核销。多次核销会显示剩余次数。</p>
      </div>
    </div>

    <div class="pickup-search">
      <el-input
        v-model="pickupCode"
        placeholder="输入或扫描提货码"
        maxlength="16"
        style="max-width: 260px"
        @keyup.enter="loadPickup"
      />
      <el-button type="primary" :loading="pickupLoading" @click="loadPickup">拉取信息</el-button>
      <span v-if="appMode === 'demo'" class="demo-hint">演示提货码：654321（只读展示，不执行真实核销）</span>
    </div>

    <div v-if="pickupDetail" class="pickup-card">
      <div class="pickup-head">
        <div>
          <span class="muted">提货码</span>
          <strong class="pickup-code">{{ pickupDetail.pickupCode }}</strong>
        </div>
        <el-tag :type="pickupDetail.status === 'COMPLETED' ? 'success' : pickupDetail.status === 'READY' ? 'warning' : 'info'">
          {{ pickupStatusLabel(pickupDetail.status) }}
        </el-tag>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="顾客">{{ pickupDetail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ pickupDetail.customerPhone }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ pickupDetail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="自提门店">{{ pickupDetail.storeName }}</el-descriptions-item>
        <el-descriptions-item label="核销次数">{{ remainingLabel(pickupDetail) }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">
          ¥{{ (pickupDetail.total.amount / 100).toFixed(2) }}
        </el-descriptions-item>
      </el-descriptions>

      <h3>商品明细</h3>
      <el-table :data="pickupDetail.items" size="small">
        <el-table-column prop="title" label="商品" />
        <el-table-column prop="quantity" label="数量" width="100" />
      </el-table>

      <div class="verify-actions">
        <el-button
          type="primary"
          :disabled="appMode === 'demo' || pickupDetail.remainingUses === 0"
          @click="verifyPickup('PICKUP_CODE')"
        >
          按提货码核销
        </el-button>
        <el-button
          :disabled="appMode === 'demo' || pickupDetail.remainingUses === 0"
          @click="verifyPickup('PHONE')"
        >
          按手机号核销
        </el-button>
        <span class="muted">请先与顾客核对姓名、手机号和商品明细。</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.panel-head h2 {
  margin: 0;
}

.panel-head p {
  margin: 4px 0 0;
}

.store-picker {
  display: flex;
  gap: 10px;
  align-items: center;
}

.live-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #6b716c;
  font-size: 12px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d99a2b;
}

.dot.connected {
  background: #1f7a4d;
}

.demo-hint {
  color: #a33232;
  font-size: 12px;
}

.task-card {
  padding: 18px;
  border: 1px solid #dedbd3;
  border-radius: 14px;
  background: #fbfaf7;
}

.task-card + .task-card {
  margin-top: 12px;
}

.task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.pick-line {
  display: grid;
  grid-template-columns: minmax(180px, 1.2fr) minmax(180px, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 12px 0;
  border-top: 1px dashed #dedbd3;
}

.pick-product,
.pick-batch {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pick-action {
  display: flex;
  justify-content: flex-end;
}

.pickup-search {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.pickup-card {
  padding: 18px;
  border: 1px solid #dedbd3;
  border-radius: 14px;
  background: #fbfaf7;
}

.pickup-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.pickup-head > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pickup-code {
  color: #c46a2b;
  font-size: 24px;
  letter-spacing: 0.12em;
}

.pickup-card h3 {
  margin: 20px 0 10px;
  font-size: 15px;
}

.verify-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}

.mb-16 {
  margin-bottom: 16px;
}

@media (max-width: 760px) {
  .panel-head,
  .store-picker,
  .pickup-search {
    flex-direction: column;
  }

  .pick-line {
    grid-template-columns: 1fr;
  }

  .pick-action {
    justify-content: flex-start;
  }
}
</style>