<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Expiring {
  batchId: string
  batchNo: string
  skuCode: string
  locationType: string
  quantity: number
  expiryDate: string
  daysLeft: number
}

const rows = ref<Expiring[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    // limit=200 是硬上限：批次更多时后端只返回到期最近的 200 条且页面不提示“被截断”，
    // 所以下方“共 N 个批次”是已加载数，不是全量——要全量请走导出中心。
    rows.value = await api.get<Expiring[]>('/admin/v1/inventory/batches/expiring?limit=200')
  // 失败只弹 toast 并保留上一次的 rows：刷新失败时列表看起来仍正常，看不出是旧数据
  //（与 AGENTS.md §10.1“错误要能定位”有差距，页面级错误块尚未加）。
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function tone(days: number) {
  if (days < 0) return 'danger'
  if (days <= 7) return 'warning'
  return 'info'
}

onMounted(load)
</script>

<template>
  <div>
    <h1>临期商品</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="按到期日升序：负数表示已过期"
              description="过期批次已无法被拣货分配；这里用于提前促销或下架。" />

    <div class="mb-16">
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
      <span class="muted">共 {{ rows.length }} 个批次</span>
    </div>

    <el-table :data="rows" v-loading="loading" empty-text="暂无临期批次">
      <el-table-column prop="skuCode" label="SKU" min-width="180" />
      <el-table-column prop="batchNo" label="批次" min-width="180" />
      <el-table-column prop="locationType" label="地点类型" width="130" />
      <el-table-column prop="quantity" label="数量" width="100" />
      <el-table-column prop="expiryDate" label="到期日" width="140" />
      <el-table-column label="剩余天数" width="130">
        <template #default="{ row }">
          <el-tag :type="tone(row.daysLeft)">
            {{ row.daysLeft < 0 ? `已过期 ${-row.daysLeft} 天` : `${row.daysLeft} 天` }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.muted { margin-left: 12px; }
</style>
