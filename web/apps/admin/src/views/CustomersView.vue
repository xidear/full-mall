<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api'

interface Customer {
  phone: string
  name: string
  orderCount: number
  totalSpent: { amount: number; currency: string }
  lastOrderAt: string
}

const customers = ref<Customer[]>([])
const loading = ref(false)
const error = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    customers.value = await api.get<Customer[]>('/admin/v1/customers', { limit: 200 })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '客户加载失败'
  } finally {
    loading.value = false
  }
}

function money(value: Customer['totalSpent']) {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: value.currency }).format(value.amount / 100)
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>客户管理</h2>
        <p class="muted">按手机号聚合订单客户；客户账号、会员等级和标签将在下一阶段接入。</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-table :data="customers" stripe>
      <el-table-column prop="name" label="客户" width="140" />
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column prop="orderCount" label="订单数" width="100" />
      <el-table-column label="累计消费" width="140">
        <template #default="{ row }">{{ money(row.totalSpent) }}</template>
      </el-table-column>
      <el-table-column label="最近下单" min-width="180">
        <template #default="{ row }">{{ new Date(row.lastOrderAt).toLocaleString('zh-CN') }}</template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !customers.length" description="暂无客户订单" />
  </div>
</template>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}
</style>