<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { roleLabel, type StaffUser } from '@fullmall/api-client'
import { api } from '../api'

const staff = ref<StaffUser[]>([])
const loading = ref(false)
const error = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    staff.value = await api.get<StaffUser[]>('/admin/v1/staff')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '员工加载失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>员工与权限</h2>
        <p class="muted">当前按角色控制后台菜单；新增员工、角色权限矩阵和数据权限将在下一阶段接入。</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-table :data="staff" stripe>
      <el-table-column prop="username" label="账号" width="160" />
      <el-table-column prop="displayName" label="姓名" width="160" />
      <el-table-column label="角色" width="160">
        <template #default="{ row }">{{ roleLabel(row.role) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}
</style>