<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  fulfillmentModeLabel,
  fulfillmentStatusLabel,
  fulfillmentTypeLabel,
  orderStatusLabel,
  type Fulfillment,
  type Order,
  type OrderSummary
} from '@fullmall/api-client'
import { api } from '../api'

const orders = ref<OrderSummary[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const status = ref('')
const selected = ref<Order | null>(null)
const fulfillments = ref<Fulfillment[]>([])
const detailLoading = ref(false)

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

async function load() {
  loading.value = true
  error.value = ''
  try {
    const page = await api.get<{ items: OrderSummary[]; total: number }>('/admin/v1/orders', {
      status: status.value || undefined,
      page: 0,
      size: 100
    })
    orders.value = page.items
    total.value = page.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '订单加载失败'
  } finally {
    loading.value = false
  }
}

async function viewOrder(order: OrderSummary) {
  detailLoading.value = true
  error.value = ''
  try {
    selected.value = await api.get<Order>(`/api/v1/orders/${order.id}`)
    fulfillments.value = await api.get<Fulfillment[]>(`/api/v1/orders/${order.id}/fulfillments`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '订单详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

function money(amount: number, currency: string) {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency }).format(amount / 100)
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
        <el-select v-model="status" style="width: 160px" @change="load">
          <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-table :data="orders" stripe>
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
      <el-table-column label="金额" width="130">
        <template #default="{ row }">{{ money(row.total.amount, row.total.currency) }}</template>
      </el-table-column>
      <el-table-column label="下单时间" width="180">
        <template #default="{ row }">{{ new Date(row.createdAt).toLocaleString('zh-CN') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewOrder(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <p class="muted">共 {{ total }} 条订单</p>
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
        <el-descriptions-item label="下单时间">{{ new Date(selected.createdAt).toLocaleString('zh-CN') }}</el-descriptions-item>
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
    </div>
  </div>
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