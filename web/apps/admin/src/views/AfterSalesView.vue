<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AfterSaleView } from '@fullmall/api-client'
import { api } from '../api'

interface Location {
  id: string
  name: string
  type: string
}

const afterSales = ref<AfterSaleView[]>([])
const locations = ref<Location[]>([])
const loading = ref(false)
const error = ref('')
const statusFilter = ref('')
const detail = ref<AfterSaleView | null>(null)
const detailDialog = ref(false)
const receiveDialog = ref(false)
const receiveTarget = ref<AfterSaleView | null>(null)
const receiveForm = reactive({ locationId: '' })

const statusLabels: Record<string, string> = {
  REQUESTED: '待审核',
  APPROVED: '已同意',
  REJECTED: '已拒绝',
  RECEIVED: '已退货入库',
  REFUNDED: '已退款',
  CANCELLED: '已取消'
}

const typeLabels: Record<string, string> = {
  RETURN: '退货退款',
  EXCHANGE: '换货'
}

onMounted(async () => {
  await load()
  try {
    locations.value = await api.get<Location[]>('/admin/v1/warehouse/locations')
  } catch {
    locations.value = []
  }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    afterSales.value = await api.get<AfterSaleView[]>('/admin/v1/after-sales', {
      status: statusFilter.value || undefined
    })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '售后单加载失败'
  } finally {
    loading.value = false
  }
}

function openDetail(afterSale: AfterSaleView) {
  detail.value = afterSale
  detailDialog.value = true
}

async function approve(afterSale: AfterSaleView) {
  loading.value = true
  try {
    await api.post(`/admin/v1/after-sales/${afterSale.id}/approve`, { adminNote: '审核通过' })
    ElMessage.success('售后已同意')
    await load()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '审核失败'
  } finally {
    loading.value = false
  }
}

async function reject(afterSale: AfterSaleView) {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝售后', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '拒绝原因不能为空'
    })
    loading.value = true
    await api.post(`/admin/v1/after-sales/${afterSale.id}/reject`, { reason: value })
    ElMessage.success('售后已拒绝')
    await load()
  } catch (cause) {
    if (cause === 'cancel' || cause === 'close') return
    error.value = cause instanceof Error ? cause.message : '拒绝失败'
  } finally {
    loading.value = false
  }
}

function openReceive(afterSale: AfterSaleView) {
  receiveTarget.value = afterSale
  receiveForm.locationId = locations.value[0]?.id ?? ''
  receiveDialog.value = true
}

async function receive() {
  if (!receiveTarget.value || !receiveForm.locationId) {
    ElMessage.warning('请选择退货入库地点')
    return
  }
  const location = locations.value.find(item => item.id === receiveForm.locationId)
  if (!location) return
  loading.value = true
  try {
    await api.post(`/admin/v1/after-sales/${receiveTarget.value.id}/receive`, {
      locationType: location.type,
      locationId: location.id
    })
    receiveDialog.value = false
    ElMessage.success('退货已入库，批次状态为隔离')
    await load()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '退货入库失败'
  } finally {
    loading.value = false
  }
}

async function refund(afterSale: AfterSaleView) {
  try {
    await ElMessageBox.confirm(
      `确认退款 ${money(afterSale.refundAmount)}？款项将原路退回（Mock 渠道）。`,
      '确认退款',
      { confirmButtonText: '确认退款', cancelButtonText: '取消', type: 'warning' }
    )
    loading.value = true
    await api.post(`/admin/v1/after-sales/${afterSale.id}/refund`)
    ElMessage.success('退款完成')
    await load()
  } catch (cause) {
    if (cause === 'cancel' || cause === 'close') return
    error.value = cause instanceof Error ? cause.message : '退款失败'
  } finally {
    loading.value = false
  }
}

function money(value?: { amount: number; currency: string }) {
  if (!value) return '-'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: value.currency }).format(value.amount / 100)
}
</script>

<template>
  <div v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2>售后处理</h2>
          <p class="muted">退货申请 → 审核 → 退货入库（隔离批次）→ Mock 原路退款；每一步都有事件记录。</p>
        </div>
        <div class="filters">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 160px" @change="load">
            <el-option v-for="(label, value) in statusLabels" :key="value" :label="label" :value="value" />
          </el-select>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="afterSales" empty-text="暂无售后单">
        <el-table-column prop="afterSaleNo" label="售后单号" min-width="180" />
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ typeLabels[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'REFUNDED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退款金额" width="120">
          <template #default="{ row }">{{ money(row.refundAmount) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="160" />
        <el-table-column label="申请时间" width="180">
          <template #default="{ row }">{{ new Date(row.createdAt).toLocaleString('zh-CN') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'REQUESTED'" size="small" type="primary" @click="approve(row)">同意</el-button>
            <el-button v-if="row.status === 'REQUESTED' || row.status === 'APPROVED'" size="small" type="danger" plain @click="reject(row)">拒绝</el-button>
            <el-button v-if="row.status === 'APPROVED'" size="small" type="warning" @click="openReceive(row)">退货入库</el-button>
            <el-button v-if="row.status === 'RECEIVED'" size="small" type="success" @click="refund(row)">退款</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailDialog" title="售后详情" width="680px">
      <div v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="售后单号">{{ detail.afterSaleNo }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ typeLabels[detail.type] || detail.type }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabels[detail.status] || detail.status }}</el-descriptions-item>
          <el-descriptions-item label="退款金额">{{ money(detail.refundAmount) }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="原因" :span="2">{{ detail.reason }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detail.description || '—' }}</el-descriptions-item>
          <el-descriptions-item label="后台备注" :span="2">{{ detail.adminNote || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h3 class="section-title">退货商品</h3>
        <el-table :data="detail.lines" size="small">
          <el-table-c
          <el-table-column prop="title" label="商品" min-width="180" />
          <el-table-column prop="skuCode" label="SKU" width="150" />
          <el-table-column prop="quantity" label="数量" width="80" />
        </el-table>
        <h3 class="section-title">处理事件</h3>
        <el-timeline>
          <el-timeline-item v-for="event in detail.events" :key="event.id" :timestamp="new Date(event.createdAt).toLocaleString('zh-CN')">
            {{ event.eventType }} · {{ event.note || '—' }} · {{ event.operatorName || '—' }}
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-dialog>

    <el-dialog v-model="receiveDialog" title="退货入库" width="460px">
      <el-form label-position="top">
        <el-form-item label="入库地点">
          <el-select v-model="receiveForm.locationId" style="width: 100%">
            <el-option
              v-for="location in locations"
              :key="location.id"
              :label="`${location.name}（${location.type === 'STORE' ? '门店' : '仓库'}）`"
              :value="location.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert title="退货商品将创建 QUARANTINE 隔离批次，质检后再决定上架或报损。" type="info" :closable="false" />
      <template #footer>
        <el-button @click="receiveDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="receive">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filters {
  display: flex;
  gap: 10px;
}

.section-title {
  margin: 18px 0 10px;
  font-size: 15px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>
