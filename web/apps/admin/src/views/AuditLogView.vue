<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { formatDateTime } from '@fullmall/api-client'
import { api } from '../api'

interface AuditEvent {
  eventType: string
  actor: string
  resource: string
  resourceId: string
  detail: string
  requestId: string
  createdAt: string
}

const items = ref<AuditEvent[]>([])
const loading = ref(false)
const error = ref('')
const eventType = ref('')
const actor = ref('')
const range = ref<Date[] | null>(null)
const page = ref(0)
const size = ref(20)
const total = ref(0)

// 实体快照：只有注册了审计类型的实体可查（当前仅资产）
const snapshotType = ref('asset')
const snapshotId = ref('')
const snapshots = ref<Array<Record<string, unknown>>>([])
const snapshotLoading = ref(false)
const snapshotError = ref('')

async function loadSnapshots() {
  if (!snapshotType.value.trim() || !snapshotId.value.trim() || snapshotLoading.value) return
  snapshotLoading.value = true
  snapshotError.value = ''
  snapshots.value = []
  try {
    snapshots.value = await api.get<Array<Record<string, unknown>>>('/admin/v1/audit/history', {
      entityType: snapshotType.value.trim(),
      entityId: snapshotId.value.trim()
    })
  } catch (cause) {
    snapshotError.value = cause instanceof Error ? cause.message : '快照加载失败'
  } finally {
    snapshotLoading.value = false
  }
}

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const query: Record<string, string | number | undefined> = {
      eventType: eventType.value || undefined,
      actor: actor.value.trim() || undefined,
      page: page.value,
      size: size.value
    }
    if (range.value && range.value.length === 2) {
      query.from = new Date(range.value[0]).toISOString()
      query.to = new Date(range.value[1]).toISOString()
    }
    const response = await api.get<{ items: AuditEvent[]; total: number }>(
      '/admin/v1/audit/outbox/events', query)
    items.value = response.items
    total.value = response.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '审计日志加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 0
  load()
}

function changePage(next: number) {
  page.value = next - 1
  load()
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>操作审计</h2>
        <p class="muted">记录支付、退款、提现、权限、库存等关键操作；只读，不可修改或删除。</p>
      </div>
      <div class="header-actions">
        <el-input v-model="eventType" placeholder="事件类型，如 PAYMENT_REFUNDED"
                  clearable style="width: 220px" @keyup.enter="search" />
        <el-input v-model="actor" placeholder="操作人（模糊）" clearable
                  style="width: 180px" @keyup.enter="search" />
        <el-date-picker v-model="range" type="datetimerange" style="width: 340px" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>
    <el-tabs>
      <el-tab-pane label="操作事件">
        <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <el-table :data="items" stripe>
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="eventType" label="事件" width="220" show-overflow-tooltip />
      <el-table-column prop="actor" label="操作人" width="180" show-overflow-tooltip />
      <el-table-column label="对象" width="200">
        <template #default="{ row }">{{ row.resource }} / {{ row.resourceId }}</template>
      </el-table-column>
      <el-table-column prop="detail" label="详情" min-width="240" show-overflow-tooltip />
    </el-table>

    <div class="table-footer">
      <span class="muted">共 {{ total }} 条</span>
      <el-pagination layout="prev, pager, next" :page-size="size" :total="total"
                     :current-page="page + 1" @current-change="changePage" />
    </div>

        <el-empty v-if="!loading && !items.length" description="没有匹配的审计记录" />
      </el-tab-pane>

      <el-tab-pane label="实体快照">
        <el-alert type="info" :closable="false" show-icon class="mb-16"
                  title="只有注册了审计类型的实体可查（当前仅 asset）"
                  description="用于核对某条记录的历史版本；普通业务变更请看「操作事件」。" />
        <div class="snapshot-actions mb-16">
          <el-input v-model="snapshotType" placeholder="实体类型，如 asset" style="width: 200px" />
          <el-input v-model="snapshotId" aria-label="按实体 ID 查看历史快照" placeholder="调试：实体 ID（UUID）" style="width: 320px" />
          <el-button type="primary" :loading="snapshotLoading" @click="loadSnapshots">查询</el-button>
        </div>
        <el-alert v-if="snapshotError" :title="snapshotError" type="error" show-icon
                  :closable="false" class="mb-16" />
        <el-table :data="snapshots" v-loading="snapshotLoading" empty-text="暂无快照">
          <el-table-column prop="commitId" label="版本" min-width="240" />
          <el-table-column prop="author" label="操作人" width="160" />
          <el-table-column prop="committedAt" label="提交时间" width="180" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.snapshot-actions { display: flex; gap: 12px; }
</style>
