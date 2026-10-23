<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'

interface Draft {
  id: string
  actionType: string
  status: string
  requestedBy: string
  approvedBy1: string
  approvedBy2: string
  createdAt: string
}

const rows = ref<Draft[]>([])
const loading = ref(false)
const acting = ref('')

async function load() {
  loading.value = true
  try {
    rows.value = await api.get<Draft[]>('/admin/v1/ai/actions/pending?limit=100')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function act(draft: Draft, action: 'approve' | 'reject' | 'execute') {
  if (acting.value) return
  if (action === 'reject') {
    try {
      const reason = await ElMessageBox.prompt('驳回原因（会记录给发起人）', '驳回', {
        inputPattern: /\S+/, inputErrorMessage: '必须填写原因'
      })
      acting.value = draft.id
      await api.post(`/admin/v1/ai/actions/${draft.id}/reject`, { reason: reason.value })
      ElMessage.success('已驳回')
    } catch (cause) {
      if (cause instanceof Error) ElMessage.error(cause.message)
    } finally {
      acting.value = ''
      await load()
    }
    return
  }
  acting.value = draft.id
  try {
    await api.post(`/admin/v1/ai/actions/${draft.id}/${action}`, {})
    ElMessage.success(action === 'approve' ? '已签' : '已执行')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
  } finally {
    acting.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>AI 变更审批</h1>
    <el-alert type="warning" :closable="false" show-icon class="mb-16"
              title="AI 只能提草稿：必须两个不同管理员签署后才能执行"
              description="同一人不能签两次；执行会真实改动数据，请先核对载荷。" />

    <div class="mb-16">
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" empty-text="没有待处理的 AI 变更">
      <el-table-column prop="actionType" label="动作" width="170" />
      <el-table-column prop="status" label="状态" width="150" />
      <el-table-column prop="requestedBy" label="发起" width="140" />
      <el-table-column label="签署" width="200">
        <template #default="{ row }">
          {{ row.approvedBy1 || '—' }} / {{ row.approvedBy2 || '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="190" />
      <el-table-column label="操作" width="230">
        <template #default="{ row }">
          <el-button link type="primary" :loading="acting === row.id"
                     :disabled="row.status !== 'PENDING_APPROVAL'"
                     @click="act(row, 'approve')">签署</el-button>
          <el-button link type="danger" :disabled="row.status !== 'PENDING_APPROVAL'"
                     @click="act(row, 'reject')">驳回</el-button>
          <el-button link type="warning" :disabled="row.status !== 'APPROVED'"
                     :loading="acting === row.id"
                     @click="act(row, 'execute')">执行</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
</style>
