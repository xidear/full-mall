<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  fulfillmentModeLabel,
  fulfillmentStatusLabel,
  fulfillmentTypeLabel,
  orderStatusLabel,
  formatAmount,
  toMajor,
  toMinor,
  type Fulfillment,
  type Order,
  type OrderSummary, formatDateTime, formatDate } from '@fullmall/api-client'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const orders = ref<OrderSummary[]>([])
const total = ref(0)
const page = ref(0)
// keyset 模式：游标替代页码，深页不再随页数变慢
const useKeyset = ref(true)
const cursorCreatedAt = ref<string | null>(null)
const cursorId = ref<string | null>(null)
const hasMore = ref(true)
const loadingMore = ref(false)
const size = ref(20)

const sortKey = ref('createdAt')
const sortDirection = ref<'asc' | 'desc'>('desc')

function onSortChange(payload: { prop: string | null; order: 'ascending' | 'descending' | null }) {
  sortKey.value = payload.prop ?? 'createdAt'
  sortDirection.value = payload.order === 'ascending' ? 'asc' : 'desc'
  page.value = 0
  load()
}

function reloadFromFirstPage() {
  page.value = 0
  load()
}

function changePage(next: number) {
  page.value = next - 1
  load()
}
const loading = ref(false)
const error = ref('')
const route = useRoute()
// 支持从工作台待办直达：/orders?status=PENDING_SHIPMENT
const status = ref(typeof route.query.status === 'string' ? route.query.status : '')
const selected = ref<Order | null>(null)
const fulfillments = ref<Fulfillment[]>([])
const detailLoading = ref(false)
// 退款：金额按元输入，提交换算成分；原因必填（后端强制）
const refundDialog = ref(false)
const refundYuan = ref(0)
const refundReason = ref('')
const refundSaving = ref(false)

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'PENDING_PAYMENT', label: '待付款' },
  { value: 'PAID', label: '已付款' },
  { value: 'ALLOCATED', label: '已分配履约' },
  { value: 'PARTIALLY_FULFILLED', label: '部分完成' },
  { value: 'FULFILLED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'REFUNDED', label: '已退款' }
]

onMounted(load)

async function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  loadingMore.value = true
  try {
    const response = await api.get<{
      items: OrderSummary[]
      nextCreatedAt: string | null
      nextId: string | null
    }>('/admin/v1/orders/keyset', {
      status: status.value || undefined,
      cursorCreatedAt: cursorCreatedAt.value || undefined,
      cursorId: cursorId.value || undefined,
      size: size.value
    })
    orders.value = [...orders.value, ...response.items]
    cursorCreatedAt.value = response.nextCreatedAt
    cursorId.value = response.nextId
    hasMore.value = Boolean(response.nextId)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '加载更多失败'
  } finally {
    loadingMore.value = false
  }
}

async function load() {
  loading.value = true
  error.value = ''
  if (useKeyset.value) {
    cursorCreatedAt.value = null
    cursorId.value = null
    hasMore.value = true
    orders.value = []
    loading.value = false
    await loadMore()
    return
  }
  try {
    const response = await api.get<{ items: OrderSummary[]; total: number }>('/admin/v1/orders', {
      status: status.value || undefined,
      page: page.value,
      size: size.value,
      sort: sortKey.value,
      direction: sortDirection.value
    })
    orders.value = response.items
    total.value = response.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '订单加载失败'
  } finally {
    loading.value = false
  }
}

function openRefund() {
  if (!selected.value) return
  refundYuan.value = toMajor(selected.value.total.amount, selected.value.total.currency)
  refundReason.value = ''
  refundDialog.value = true
}

async function submitRefund() {
  if (!selected.value || refundSaving.value) return
  if (!refundReason.value.trim()) {
    ElMessage.error('请填写退款原因')
    return
  }
  refundSaving.value = true
  try {
    await api.post(`/admin/v1/orders/${selected.value.id}/refunds`, {
      amount: toMinor(refundYuan.value, selected.value.total.currency),
      reason: refundReason.value.trim()
    })
    ElMessage.success('退款已提交')
    refundDialog.value = false
    await viewOrder(selected.value)
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '退款失败')
  } finally {
    refundSaving.value = false
  }
}

async function viewOrder(order: OrderSummary) {
  detailLoading.value = true
  error.value = ''
  try {
    selected.value = await api.get<Order>(`/admin/v1/orders/${order.id}`)
    fulfillments.value = await api.get<Fulfillment[]>(`/admin/v1/orders/${order.id}/fulfillments`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '订单详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

function money(amount: number, currency: string) {
  return formatAmount(amount, currency)
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>订单管理</h2>
        <p class="muted">按状态筛选订单，查看商品、履约和自提信息。</p>
      </div>
      <div class="toolbar">
        <el-select v-model="status" style="width: 160px" @change="reloadFromFirstPage">
          <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-table :data="orders" @sort-change="onSortChange" stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'FULFILLED' ? 'success' : row.status === 'CANCELLED' ? 'danger' : 'warning'">
            {{ orderStatusLabel(row.status) }}
          </el-tag>
        

</template>
      </el-table-column>
      <el-table-column label="履约方式" width="120">
        <template #default="{ row }">{{ fulfillmentModeLabel(row.fulfillmentMode) }}</template>
      </el-table-column>
      <el-table-column prop="contactName" label="联系人" width="120" />
      <el-table-column label="金额" sortable="custom" prop="totalAmount" width="130">
        <template #default="{ row }">{{ money(row.total.amount, row.total.currency) }}</template>
      </el-table-column>
      <el-table-column label="下单时间" sortable="custom" prop="createdAt" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewOrder(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-footer">
      <span class="muted">共 {{ total }} 条订单</span>
      <el-button v-if="useKeyset" data-testid="load-more" :loading="loadingMore"
                 :disabled="!hasMore" @click="loadMore">
        {{ hasMore ? '加载更多' : '没有更多了' }}
      </el-button>
      <el-pagination v-else
        layout="prev, pager, next"
        :page-size="size"
        :total="total"
        :current-page="page + 1"
        @current-change="changePage" />
    </div>
    <el-empty v-if="!loading && !orders.length" description="暂无订单" />

    <div v-if="selected" v-loading="detailLoading" class="order-detail">
      <div class="panel-header">
        <h2>订单详情 {{ selected.orderNo }}</h2>
        <el-tag>{{ orderStatusLabel(selected.status) }}</el-tag>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="联系人">{{ selected.contactName }} · {{ selected.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="履约方式">{{ fulfillmentModeLabel(selected.fulfillmentMode) }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ money(selected.total.amount, selected.total.currency) }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatDateTime(selected.createdAt) }}</el-descriptions-item>
      </el-descriptions>

      <h3>商品</h3>
      <el-table :data="selected.lines" size="small">
        <el-table-column prop="title" label="商品" min-width="220" />
        <el-table-column prop="skuCode" label="SKU" width="140" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="履约类型" width="130">
          <template #default="{ row }">{{ fulfillmentTypeLabel(row.fulfillmentType) }}</template>
        </el-table-column>
        <el-table-column label="小计" width="130">
          <template #default="{ row }">{{ money(row.unitPrice.amount * row.quantity, row.unitPrice.currency) }}</template>
        </el-table-column>
      </el-table>

      <h3>履约单</h3>
      <el-table :data="fulfillments" size="small">
        <el-table-column label="类型" width="140">
          <template #default="{ row }">{{ fulfillmentTypeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }">{{ fulfillmentStatusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="pickupCode" label="提货码" width="120" />
        <el-table-column prop="trackingNo" label="物流单号" min-width="160" />
      </el-table>
    
      <div class="header-actions" style="margin-top: 12px">
        <el-button type="danger" plain @click="openRefund">发起退款</el-button>
      </div>
</div>
  </div>
    <el-dialog v-model="refundDialog" title="发起退款" width="420px">
      <el-form label-width="90px">
        <el-form-item label="订单金额">
          <span>{{ selected ? money(selected.total.amount, selected.total.currency) : '' }}</span>
        </el-form-item>
        <el-form-item label="退款金额（元）">
          <el-input-number v-model="refundYuan" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input v-model="refundReason" placeholder="必填，会写入审计" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialog = false">取消</el-button>
        <el-button type="danger" :loading="refundSaving" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>
</template>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.order-detail {
  margin-top: 26px;
  padding-top: 22px;
  border-top: 1px solid #ece9e2;
}

.order-detail h3 {
  margin: 20px 0 10px;
  font-size: 15px;
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin: 0;
}
</style>