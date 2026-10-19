<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { PermissionMatrixView, PermissionView } from '@fullmall/api-client'
import { api } from '../api'

const matrix = ref<PermissionMatrixView | null>(null)
const selectedRole = ref('OPERATOR')
const selected = ref<string[]>([])
const loading = ref(false)
const error = ref('')

const roleLabels: Record<string, string> = {
  OPERATOR: '运营人员',
  STORE: '门店人员',
  CS: '客服人员',
  ADMIN: '超级管理员（全部权限）'
}

const groups = computed(() => {
  const result = new Map<string, PermissionView[]>()
  for (const permission of matrix.value?.permissions ?? []) {
    result.set(permission.group, [...(result.get(permission.group) ?? []), permission])
  }
  return Array.from(result.entries())
})

onMounted(load)

watch(selectedRole, () => {
  const role = matrix.value?.roles.find(item => item.role === selectedRole.value)
  selected.value = role ? [...role.permissions] : []
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    matrix.value = await api.get<PermissionMatrixView>('/admin/v1/permissions')
    const role = matrix.value.roles.find(item => item.role === selectedRole.value)
    selected.value = role ? [...role.permissions] : []
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '权限矩阵加载失败'
  } finally {
    loading.value = false
  }
}

function toggle(code: string) {
  if (selectedRole.value === 'ADMIN') return
  selected.value = selected.value.includes(code)
    ? selected.value.filter(item => item !== code)
    : [...selected.value, code]
}

async function save() {
  if (selectedRole.value === 'ADMIN') return
  loading.value = true
  error.value = ''
  try {
    await api.put(`/admin/v1/permissions/roles/${selectedRole.value}`, { codes: selected.value })
    ElMessage.success('权限已保存，下一次请求生效')
    await load()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '权限保存失败'
  } finally {
    loading.value = false
  }
}

function checked(code: string) {
  return selectedRole.value === 'ADMIN' || selected.value.includes(code)
}
</script>

<template>
  <div v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2>角色权限矩阵</h2>
          <p class="muted">
            权限在后端过滤器中强制校验，不只是隐藏菜单。ADMIN 始终拥有全部权限；门店和客服账号不能进入运营后台。
          </p>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>

      <el-radio-group v-model="selectedRole" class="role-tabs">
        <el-radio-button v-for="role in ['OPERATOR', 'STORE', 'CS', 'ADMIN']" :key="role" :label="role">
          {{ roleLabels[role] }}
        </el-radio-button>
      </el-radio-group>

      <div v-if="selectedRole === 'ADMIN'" class="admin-note">
        ADMIN 拥有全部权限，无需配置。
      </div>

      <div class="permission-groups">
        <section v-for="[group, permissions] in groups" :key="group" class="permission-group">
          <h3>{{ group }}</h3>
          <label v-for="permission in permissions" :key="permission.code" class="permission-row">
            <el-checkbox
              :model-value="checked(permission.code)"
              :disabled="selectedRole === 'ADMIN'"
              @change="toggle(permission.code)"
            />
            <span>
              <strong>{{ permission.label }}</strong>
              <code>{{ permission.code }}</code>
            </span>
          </label>
        </section>
      </div>

      <div class="save-row">
        <el-button type="primary" :disabled="selectedRole === 'ADMIN'" :loading="loading" @click="save">
          保存 {{ roleLabels[selectedRole] }} 权限
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.role-tabs {
  margin-bottom: 18px;
}

.admin-note {
  margin-bottom: 18px;
  padding: 12px;
  border-radius: 8px;
  background: #f6f4ee;
  color: #5c574e;
  font-size: 13px;
}

.permission-groups {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.permission-group {
  padding: 14px;
  border: 1px solid #e3dfd6;
  border-radius: 12px;
  background: #fbfaf7;
}

.permission-group h3 {
  margin: 0 0 10px;
  font-size: 14px;
}

.permission-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 7px 0;
  border-top: 1px dashed #e3dfd6;
  cursor: pointer;
}

.permission-row:first-of-type {
  border-top: 0;
}

.permission-row span {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.permission-row strong {
  font-size: 13px;
}

.permission-row code {
  color: #8a8f8a;
  font-size: 11px;
}

.save-row {
  margin-top: 18px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>
