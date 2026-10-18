<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

interface Money {
  amount: number
  currency: string
}

interface DashboardSummary {
  todayOrders: number
  todayPaidOrders: number
  todaySales: Money
  pendingPayment: number
  pendingFulfillment: number
  pendingPickup: number
  lowStock: number
  nearExpiry: number
  activeProducts: number
  customers: number
  todos: Array<{ code: string; label: string; count: number; path: string }>
}

const router = useRouter()
const summary = ref<DashboardSummary | null>(null)
const error = ref('')
const loading = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    summary.value = await api.get<DashboardSummary>('/admin/v1/dashboard')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function money(value: Money | undefined) {
  if (!value) return '-'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: value.currency }).format(value.amount / 100)
}
</script>

<template>
  <div v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <div class="metric-grid">
      <div class="metric-card">
        <span>今日订单</span>
        <strong>{{ summary?.todayOrders ?? 0 }}</strong>
      </div>
      <div class="metric-card">
        <span>今日销售额</span>
        <strong>{{ money(summary?.todaySales) }}</strong>
      </div>
      <div class="metric-card">
        <span>今日已支付</span>
        <strong>{{ summary?.todayPaidOrders ?? 0 }}</strong>
      </div>
      <div class="metric-card">
        <span>待付款</span>
        <strong>{{ summary?.pendingPayment ?? 0 }}</strong>
      </div>
      <div class="metric-card">
        <span>待发货/出库</span>
        <strong>{{ summary?.pendingFulfillment ?? 0 }}</strong>
      </div>
      <div class="metric-card">
        <span>待自提</span>
        <strong>{{ summary?.pendingPickup ?? 0 }}</strong>
      </div>
      <div class="metric-card">
        <span>低库存批次</span>
        <strong>{{ summary?.lowStock ?? 0 }}</strong>
      </div>
      <div class="metric-card">
        <span>临期批次</span>
        <strong>{{ summary?.nearExpiry ?? 0 }}</strong>
      </div>
      <div class="metric-card">
        <span>在售商品</span>
        <strong>{{ summary?.activeProducts ?? 0 }}</strong>
      </div>
      <div class="metric-card">
        <span>客户数</span>
        <strong>{{ summary?.customers ?? 0 }}</strong>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2>待办事项</h2>
          <p class="muted">点击进入对应处理页面。</p>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>
      <div class="todo-grid">
        <button
          v-for="todo in summary?.todos ?? []"
          :key="todo.code"
          type="button"
          class="todo-card"
          :class="{ empty: todo.count === 0 }"
          @click="router.push(todo.path)"
        >
          <span>{{ todo.label }}</span>
          <strong>{{ todo.count }}</strong>
          <small>{{ todo.count ? '去处理 →' : '暂无待办' }}</small>
        </button>
      </div>
    </div>

    <div class="panel notice-panel">
      <div class="panel-header">
        <h2>运营提示</h2>
      </div>
      <p class="muted">
        当前看板由订单、支付、履约、批次库存和自提数据实时汇总。后续接入趋势图、商品排行、门店/供应商维度与业务事件明细。
      </p>
      <el-space wrap>
        <el-tag type="success">批次/效期</el-tag>
        <el-tag type="info">多仓/门店</el-tag>
        <el-tag type="warning">供应商代发</el-tag>
        <el-tag>自提</el-tag>
        <el-tag>Mock 支付</el-tag>
      </el-space>
    </div>
  </div>
</template>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.panel-header p {
  margin: 4px 0 0;
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.todo-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 16px;
  border: 1px solid #dedbd3;
  border-radius: 12px;
  background: #fff;
  color: #1f2421;
  text-align: left;
  cursor: pointer;
}

.todo-card:hover {
  border-color: #e1251b;
}

.todo-card strong {
  font-size: 28px;
}

.todo-card small {
  color: #e1251b;
  font-size: 11px;
}

.todo-card.empty strong {
  color: #6b716c;
}

.todo-card.empty small {
  color: #9a9f9b;
}

.notice-panel {
  margin-top: 18px;
}
</style>