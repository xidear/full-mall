<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useMediaQuery } from '@vueuse/core'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'

/**
 * 风险告警：FATAL/HIGH 告警的处理面板。
 *
 * <p>三种状态必须能分清（这也是升级机制的前提）：未确认 → 已在处理（确认）→ 已解决（恢复）。
 * 站内信是告警的默认通道，所以「已推送通道」里永远有 IN_APP，外发通道配置得对不对一眼就能看出来。
 */
interface RiskAlert {
  id: string
  ruleCode: string
  severity: 'FATAL' | 'HIGH' | 'MEDIUM' | 'LOW'
  subject: string
  detail?: string
  createdAt: string
  alerted: boolean
  alertedAt?: string
  alertChannel?: string
  alertError?: string
  recoveredAt?: string
  acknowledgedAt?: string
  acknowledgedBy?: string
  escalationLevel: number
  escalatedAt?: string
}

const rows = ref<RiskAlert[]>([])
const loading = ref(false)
const busyId = ref('')
// 手机尺寸下表格 9 列挤不下、右侧固定列会吃掉半屏：改用卡片列表，操作按钮跟着卡片走
const narrow = useMediaQuery('(max-width: 640px)')

async function load() {
  loading.value = true
  try {
    rows.value = await api.get<RiskAlert[]>('/admin/v1/risk-alerts?limit=100')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function severityTone(severity: RiskAlert['severity']) {
  if (severity === 'FATAL') return 'danger'
  if (severity === 'HIGH') return 'warning'
  if (severity === 'MEDIUM') return 'primary'
  return 'info'
}

function state(row: RiskAlert) {
  if (row.recoveredAt) return { label: '已恢复', tone: 'success' as const }
  if (row.acknowledgedAt) return { label: '处理中', tone: 'primary' as const }
  return { label: '待处理', tone: 'danger' as const }
}

const pendingCount = computed(() => rows.value.filter(row => !row.recoveredAt && !row.acknowledgedAt).length)
const escalatedCount = computed(() => rows.value.filter(row => row.escalationLevel > 0).length)

async function acknowledge(row: RiskAlert) {
  busyId.value = row.id
  try {
    await api.post(`/admin/v1/risk-alerts/${row.id}/ack`, {})
    ElMessage.success('已确认：这条告警不会再被升级')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '确认失败')
  } finally {
    busyId.value = ''
  }
}

async function recover(row: RiskAlert) {
  try {
    await ElMessageBox.confirm(
      `标记恢复后这条告警会关闭，并按通道路由发一条恢复通知。${row.ruleCode} · ${row.subject}`,
      '标记恢复',
      { confirmButtonText: '标记恢复', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  busyId.value = row.id
  try {
    await api.post(`/admin/v1/risk-alerts/${row.id}/recover`, {})
    ElMessage.success('已标记恢复')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '标记恢复失败')
  } finally {
    busyId.value = ''
  }
}

function formatTime(value?: string) {
  if (!value) return '—'
  const date = new Date(value)
  // 表格里只到分钟：秒没有决策价值，省下的宽度留给「已推送通道」
  return Number.isNaN(date.getTime())
    ? '—'
    : date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false })
}

onMounted(load)
</script>

<template>
  <div>
    <h1>风险告警</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="站内信是默认通道，外部通道按「第三方服务 → 通道路由」配置"
              description="告警会先写进通知中心（站内信，无需配置），再按用途尝试群机器人/短信/邮件。FATAL 15 分钟、HIGH 1 小时没人确认会自动升级，最多 3 级；确认处理即停止升级。" />

    <div class="mb-16">
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
      <span class="muted">共 {{ rows.length }} 条，待处理 {{ pendingCount }} 条，已升级 {{ escalatedCount }} 条</span>
    </div>

    <el-table v-if="!narrow" :data="rows" v-loading="loading" empty-text="暂无告警" class="alert-table">
      <el-table-column label="级别" width="96">
        <template #default="{ row }">
          <el-tag :type="severityTone(row.severity)">{{ row.severity }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="state(row).tone" effect="plain">{{ state(row).label }}</el-tag>
          <div v-if="row.acknowledgedBy" class="muted-inline cell-sub">{{ row.acknowledgedBy }}</div>
        </template>
      </el-table-column>
      <el-table-column label="升级" width="78">
        <template #default="{ row }">
          <el-tag v-if="row.escalationLevel > 0" type="warning" effect="plain">L{{ row.escalationLevel }}</el-tag>
          <span v-else class="muted-inline">—</span>
        </template>
      </el-table-column>
      <!-- 站内信是默认通道：这一列必须在不横向滚动的位置，否则「到底发出去没有」看不见 -->
      <el-table-column label="已推送通道" width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.alerted">{{ row.alertChannel || '—' }}</span>
          <span v-else class="danger-text">未推送：{{ row.alertError || '无可用通道' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="ruleCode" label="规则" min-width="170" show-overflow-tooltip />
      <el-table-column prop="subject" label="对象" min-width="140" show-overflow-tooltip />
      <el-table-column label="详情" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="muted-inline">{{ row.detail || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="130">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="196" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain
                     :disabled="Boolean(row.acknowledgedAt) || Boolean(row.recoveredAt)"
                     :loading="busyId === row.id"
                     @click="acknowledge(row)">确认处理</el-button>
          <el-button size="small" type="success" plain
                     :disabled="Boolean(row.recoveredAt)"
                     :loading="busyId === row.id"
                     @click="recover(row)">标记恢复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 手机尺寸：卡片列表代替宽表格，关键信息（级别/状态/升级/通道）置顶，按钮不必横向滚动 -->
    <div v-else v-loading="loading" class="alert-cards">
      <el-empty v-if="!rows.length" description="暂无告警" />
      <el-card v-for="row in rows" :key="row.id" shadow="never" class="alert-card">
        <div class="alert-card-head">
          <el-tag :type="severityTone(row.severity)">{{ row.severity }}</el-tag>
          <el-tag :type="state(row).tone" effect="plain">{{ state(row).label }}</el-tag>
          <el-tag v-if="row.escalationLevel > 0" type="warning" effect="plain">升级 L{{ row.escalationLevel }}</el-tag>
        </div>
        <div class="alert-card-title">{{ row.ruleCode }}</div>
        <div class="alert-card-line">{{ row.subject }}</div>
        <div class="alert-card-line">{{ row.detail || '—' }}</div>
        <div class="alert-card-line">
          通道：<span v-if="row.alerted">{{ row.alertChannel || '—' }}</span>
          <span v-else class="danger-text">未推送：{{ row.alertError || '无可用通道' }}</span>
        </div>
        <div class="alert-card-line">创建：{{ formatTime(row.createdAt) }}<span v-if="row.acknowledgedBy"> · 确认人 {{ row.acknowledgedBy }}</span></div>
        <div class="alert-card-actions">
          <el-button size="small" type="primary" plain
                     :disabled="Boolean(row.acknowledgedAt) || Boolean(row.recoveredAt)"
                     :loading="busyId === row.id"
                     @click="acknowledge(row)">确认处理</el-button>
          <el-button size="small" type="success" plain
                     :disabled="Boolean(row.recoveredAt)"
                     :loading="busyId === row.id"
                     @click="recover(row)">标记恢复</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.muted { margin-left: 12px; }
.muted-inline { color: #909399; }
.danger-text { color: #f56c6c; }
.alert-cards { display: flex; flex-direction: column; gap: 12px; }
.cell-sub { font-size: 12px; margin-top: 2px; }
.alert-card-head { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.alert-card-title { font-weight: 600; margin-bottom: 4px; word-break: break-all; }
.alert-card-line { color: #606266; font-size: 13px; line-height: 1.6; word-break: break-all; }
.alert-card-actions { display: flex; gap: 8px; margin-top: 12px; }
</style>
