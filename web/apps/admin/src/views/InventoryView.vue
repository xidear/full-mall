<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  inventoryStatusLabel,
  locationTypeLabel,
  type InventoryBatch
} from '@fullmall/api-client'
import { api } from '../api'

const filters = reactive({
  locationType: '' as '' | 'WAREHOUSE' | 'STORE' | 'SUPPLIER',
  expiringInDays: undefined as number | undefined
})
const batches = ref<InventoryBatch[]>([])
const loading = ref(false)
const error = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    batches.value = await api.get<InventoryBatch[]>('/admin/v1/inventory/batches', {
      locationType: filters.locationType || undefined,
      expiringInDays: filters.expiringInDays
    })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function isExpiringSoon(expiryDate?: string) {
  if (!expiryDate) return false
  const today = new Date()
  const expiry = new Date(expiryDate)
  const days = Math.ceil((expiry.getTime() - today.getTime()) / 86_400_000)
  return days >= 0 && days <= 7
}

function isExpired(expiryDate?: string) {
  if (!expiryDate) return false
  return new Date(expiryDate).getTime() < Date.now()
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <h2>库存批次</h2>
      <el-button @click="load">刷新</el-button>
    </div>

    <div class="toolbar">
      <el-select v-model="filters.locationType" placeholder="地点类型" clearable style="width: 160px">
        <el-option label="仓库" value="WAREHOUSE" />
        <el-option label="门店" value="STORE" />
        <el-option label="供应商" value="SUPPLIER" />
      </el-select>
      <el-input-number v-model="filters.expiringInDays" :min="0" :max="365" placeholder="N 天内到期" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <el-table :data="batches" stripe>
      <el-table-column prop="batchNo" label="批次" min-width="150" />
      <el-table-column prop="skuId" label="SKU" min-width="180" />
      <el-table-column prop="locationType" label="地点类型" width="110">
        <template #default="{ row }">{{ locationTypeLabel(row.locationType) }}</template>
      </el-table-column>
      <el-table-column prop="locationId" label="地点 ID" min-width="180" />
      <el-table-column prop="quantity" label="在手" width="80" />
      <el-table-column prop="reservedQuantity" label="预占" width="80" />
      <el-table-column label="可用" width="80">
        <template #default="{ row }">{{ row.quantity - row.reservedQuantity }}</template>
      </el-table-column>
      <el-table-column label="效期" width="130">
        <template #default="{ row }">
          <span :class="{ 'expiry-warning': isExpiringSoon(row.expiryDate) || isExpired(row.expiryDate) }">
            {{ row.expiryDate || '无' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'AVAILABLE' ? 'success' : row.status === 'NEAR_EXPIRY' ? 'warning' : 'danger'">
            {{ inventoryStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !batches.length" description="暂无批次库存" />
  </div>
</template>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}
</style>