<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Job {
  jobId: string
  resource: string
  status: string
}

const resources = [
  { value: 'member', label: '会员列表' },
  { value: 'pos_sale_line', label: 'POS 销售明细' },
  { value: 'sku_cost', label: 'SKU 成本（ERP）' }
]
const resource = ref('member')
const jobs = ref<Job[]>([])
const busy = ref(false)

async function createExport() {
  if (busy.value) return
  busy.value = true
  try {
    const created = await api.post<{ jobId: string }>('/admin/v1/export-jobs', {
      resource: resource.value, projection: 'default', params: {}
    })
    jobs.value.unshift({ jobId: created.jobId, resource: resource.value, status: 'PENDING' })
    ElMessage.success('已提交，正在生成')
    await poll(created.jobId)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '提交失败')
  } finally {
    busy.value = false
  }
}

async function poll(jobId: string) {
  for (let i = 0; i < 40; i += 1) {
    await new Promise(resolve => setTimeout(resolve, 500))
    const job = await api.get<{ status: string }>(`/admin/v1/export-jobs/${jobId}`)
    const target = jobs.value.find(item => item.jobId === jobId)
    if (target) target.status = job.status
    if (job.status === 'SUCCEEDED' || job.status === 'FAILED') return
  }
}

async function download(job: Job) {
  try {
    const response = await fetch(
      `${import.meta.env.VITE_API_BASE || 'http://localhost:8080'}/admin/v1/export-jobs/${job.jobId}/download`,
      { headers: { Authorization: `Bearer ${localStorage.getItem('fullmall-admin-token') ?? ''}` } })
    if (!response.ok) {
      throw new Error(`下载失败：${response.status}`)
    }
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${job.resource}-${job.jobId.slice(0, 8)}.csv`
    link.click()
    URL.revokeObjectURL(url)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '下载失败')
  }
}

onMounted(() => {})
</script>

<template>
  <div>
    <h1>导出中心</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="导出是异步任务：提交后轮询状态，成功即可下载"
              description="文件有保留期；超过最大行数会明确失败，不会静默截断。" />

    <div class="header-actions mb-16">
      <el-select v-model="resource" style="width: 240px">
        <el-option v-for="item in resources" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" :loading="busy" @click="createExport">生成导出</el-button>
    </div>

    <el-table :data="jobs" empty-text="暂无导出任务">
      <el-table-column prop="resource" label="资源" width="180" />
      <el-table-column prop="status" label="状态" width="140" />
      <el-table-column prop="jobId" label="任务 ID" min-width="280" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="row.status !== 'SUCCEEDED'"
                     @click="download(row)">下载</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.header-actions { display: flex; gap: 12px; }
.mb-16 { margin-bottom: 16px; }
</style>
