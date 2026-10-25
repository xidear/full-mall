<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface CostView {
  skuId: string
  unknown: boolean
  quantity?: number
  amount?: number
  avgCost?: number
  stale?: boolean
  latestBatch?: {
    batchNo: string
    quantity: number
    unitCost: number
    currency: string
    receivedAt: string
  } | null
}

const skuId = ref('')
const productId = ref('')
const selectedProduct = ref<{ id: string; title: string; skus?: Array<{ id: string; skuCode: string }> } | null>(null)
const loading = ref(false)
const result = ref<CostView | null>(null)

async function query() {
  if (!skuId.value.trim() || loading.value) return
  loading.value = true
  result.value = null
  try {
    result.value = await api.get<CostView>(
      `/admin/v1/products/skus/${skuId.value.trim()}/cost-view`)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '查询失败')
  } finally {
    loading.value = false
  }
}

function onProductSelect(product: { id: string; title: string; skus?: Array<{ id: string; skuCode: string }> } | null) {
  selectedProduct.value = product
  skuId.value = ''
}

function yuan(fen?: number) {
  return fen === undefined ? '—' : (fen / 100).toFixed(2)
}
</script>

<template>
  <div>
    <h1>成本查询</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="成本由采购收货自动产生（移动加权平均），不可手填"
              description="无成本记录时显示「成本未知」而不是 0；成本字段仅 ADMIN 可见。" />

    <div class="query-row mb-16">
      <ProductPicker v-model="productId" width="260px"
                     @select="onProductSelect" />
      <SkuPicker v-model="skuId" :product="selectedProduct" />
      <el-button type="primary" :loading="loading" @click="query">查询</el-button>
    </div>

    <el-result v-if="result?.unknown" icon="warning" title="成本未知"
               sub-title="该 SKU 还没有入库成本记录，毛利无法核算（不会被当成 0）" />

    <el-descriptions v-else-if="result" :column="2" border>
      <el-descriptions-item label="SKU">{{ result.skuId }}</el-descriptions-item>
      <el-descriptions-item label="在库数量">{{ result.quantity }}</el-descriptions-item>
      <el-descriptions-item label="在库金额">{{ yuan(result.amount) }} 元</el-descriptions-item>
      <el-descriptions-item label="加权平均成本">
        <strong>{{ yuan(result.avgCost) }} 元</strong>
      </el-descriptions-item>
      <el-descriptions-item label="成本状态">
        {{ result.stale ? '卖完后保留的最后成本' : '有效' }}
      </el-descriptions-item>
      <el-descriptions-item label="最近批次">
        <template v-if="result.latestBatch">
          {{ result.latestBatch.batchNo }} ·
          {{ yuan(result.latestBatch.unitCost) }} 元 ×
          {{ result.latestBatch.quantity }}
        </template>
        <template v-else>无批次记录</template>
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<style scoped>
.query-row { display: flex; gap: 12px; }
.mb-16 { margin-bottom: 16px; }
</style>
