<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { roleLabel, type StaffUser, type Store } from '@fullmall/api-client'
import { api } from '../api'

const staff = ref<StaffUser[]>([])
const stores = ref<Store[]>([])
const bindings = ref<Record<string, string[]>>({})
const loading = ref(false)
const error = ref('')

const pinDialog = ref(false)
const pinStaff = ref<StaffUser | null>(null)
const pin = ref('')
const pinSaving = ref(false)

const storeDialog = ref(false)
const storeStaff = ref<StaffUser | null>(null)
const selectedStoreIds = ref<string[]>([])
const storeSaving = ref(false)
const originalStoreIds = ref<string[]>([])

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [staffList, storeList] = await Promise.all([
      api.get<StaffUser[]>('/admin/v1/staff'),
      api.get<Store[]>('/admin/v1/stores')
    ])
    staff.value = staffList
    stores.value = storeList
    const entries = await Promise.all(staffList.map(async user => {
      const ids = await api.get<string[]>(`/admin/v1/staff/${user.id}/stores`)
      return [user.id, ids] as const
    }))
    bindings.value = Object.fromEntries(entries)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '员工加载失败'
  } finally {
    loading.value = false
  }
}

function storeNames(ids: string[]) {
  return ids.map(id => stores.value.find(store => store.id === id)?.name ?? id)
}

function openPin(user: StaffUser) {
  pinStaff.value = user
  pin.value = ''
  pinDialog.value = true
}

async function savePin() {
  if (!pinStaff.value || pin.value.trim().length < 4) {
    ElMessage.error('PIN 至少 4 位')
    return
  }
  pinSaving.value = true
  try {
    await api.post<StaffUser>(`/admin/v1/staff/${pinStaff.value.id}/pin`, { pin: pin.value.trim() })
    ElMessage.success('PIN 已更新，可用于收银锁屏解锁与店长授权')
    pinDialog.value = false
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : 'PIN 设置失败')
  } finally {
    pinSaving.value = false
  }
}

function openStores(user: StaffUser) {
  storeStaff.value = user
  originalStoreIds.value = [...(bindings.value[user.id] ?? [])]
  selectedStoreIds.value = [...originalStoreIds.value]
  storeDialog.value = true
}

async function saveStores() {
  if (!storeStaff.value) return
  storeSaving.value = true
  try {
    const staffId = storeStaff.value.id
    const toAdd = selectedStoreIds.value.filter(id => !originalStoreIds.value.includes(id))
    const toRemove = originalStoreIds.value.filter(id => !selectedStoreIds.value.includes(id))
    for (const storeId of toAdd) {
      await api.post<string[]>(`/admin/v1/staff/${staffId}/stores`, { storeId })
    }
    for (const storeId of toRemove) {
      await api.post<string[]>(`/admin/v1/staff/${staffId}/stores/unbind`, { storeId })
    }
    bindings.value = { ...bindings.value, [staffId]: [...selectedStoreIds.value] }
    ElMessage.success('门店数据权限已更新')
    storeDialog.value = false
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '门店绑定失败')
  } finally {
    storeSaving.value = false
  }
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>员工与数据权限</h2>
        <p class="muted">
          门店账号只能查看绑定门店的收银、拣货和自提数据；ADMIN/OPERATOR 可查看全部门店。
        </p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-table :data="staff" stripe>
      <el-table-column prop="username" label="账号" width="160" />
      <el-table-column prop="displayName" label="姓名" width="160" />
      <el-table-column label="角色" width="140">
        <template #default="{ row }">{{ roleLabel(row.role) }}</template>
      </el-table-column>
      <el-table-column label="绑定门店" min-width="240">
        <template #default="{ row }">
          <div v-if="bindings[row.id]?.length" class="store-tags">
            <el-tag v-for="name in storeNames(bindings[row.id])" :key="name" effect="plain">{{ name }}</el-tag>
          </div>
          <span v-else class="muted">{{ row.role === 'STORE' ? '未绑定（无法访问门店数据）' : '不限制门店' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210">
        <template #default="{ row }">
          <el-button link type="primary" @click="openStores(row)">绑定门店</el-button>
          <el-button link @click="openPin(row)">设置 PIN</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="pinDialog" title="设置员工 PIN" width="420px">
      <p class="muted">
        为 {{ pinStaff?.displayName }}（{{ pinStaff?.username }}）设置 PIN，用于收银台锁屏解锁与退款店长授权。
      </p>
      <el-input v-model="pin" type="password" show-password maxlength="12" placeholder="至少 4 位数字或字符" />
      <template #footer>
        <el-button @click="pinDialog = false">取消</el-button>
        <el-button type="primary" :loading="pinSaving" @click="savePin">保存 PIN</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="storeDialog" title="绑定门店数据范围" width="520px">
      <p class="muted">仅 STORE 角色受门店范围限制；ADMIN/OPERATOR 始终可以访问全部门店。</p>
      <el-select v-model="selectedStoreIds" multiple filterable placeholder="选择门店" style="width: 100%">
        <el-option v-for="store in stores" :key="store.id" :label="`${store.name} · ${store.address}`" :value="store.id" />
      </el-select>
      <template #footer>
        <el-button @click="storeDialog = false">取消</el-button>
        <el-button type="primary" :loading="storeSaving" @click="saveStores">保存绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.store-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>