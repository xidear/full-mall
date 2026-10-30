<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { roleLabel, VALIDATION, type StaffUser, type Store } from '@fullmall/api-client'
import { api } from '../api'
import { auth } from '../auth'
import StatusToggle from '../components/StatusToggle.vue'

const staff = ref<StaffUser[]>([])
const stores = ref<Store[]>([])
const bindings = ref<Record<string, string[]>>({})
const loading = ref(false)
// 排序状态目前只是占位：列都没加 sortable，onSortChange 实际不会触发，load() 也不接收排序参数。
const sortKey = ref('displayName')
const sortDirection = ref<'asc' | 'desc'>('asc')

function onSortChange(payload: { prop: string | null; order: 'ascending' | 'descending' | null }) {
  sortKey.value = payload.prop ?? 'displayName'
  sortDirection.value = payload.order === 'descending' ? 'desc' : 'asc'
  load()
}
const error = ref('')

// 员工档案 CRUD（角色候选来自后端 /admin/v1/staff/roles，页面不写死）
const roleOptions = ref<string[]>([])
const createDialog = ref(false)
const editDialog = ref(false)
const resetDialog = ref(false)
const saving = ref(false)
const createForm = ref({ username: '', password: '', displayName: '', role: '' })
const editForm = ref({ id: '', displayName: '', role: '' })
const resetForm = ref({ id: '', username: '', password: '' })

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
      // 排序参数后端早已支持（ListSortingTest 有覆盖），前端之前没接
      api.get<StaffUser[]>('/admin/v1/staff', { sort: sortKey.value, direction: sortDirection.value }),
      api.get<Store[]>('/admin/v1/stores')
    ])
    staff.value = staffList
    stores.value = storeList
    // N+1 请求：每个员工一次 /admin/v1/staff/{id}/stores。人数少时可接受，上百人首屏会明显变慢；
    // 根治需要后端在员工列表里直接带 storeIds。
    const entries = await Promise.all(staffList.map(async user => {
      const ids = await api.get<string[]>(`/admin/v1/staff/${user.id}/stores`)
      return [user.id, ids] as const
    }))
    bindings.value = Object.fromEntries(entries)
    // 角色候选单独取：它失败不该让员工列表整体变空（一个 Promise.all 会互相拖死）
    try {
      roleOptions.value = await api.get<string[]>('/admin/v1/staff/roles')
    } catch (cause) {
      roleOptions.value = []
      ElMessage.error('角色候选加载失败，暂时无法新建/改角色：' + (cause instanceof Error ? cause.message : ''))
    }
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
  if (!pinStaff.value
      || pin.value.trim().length < VALIDATION.pinMinLength
      || pin.value.trim().length > VALIDATION.pinMaxLength) {
    ElMessage.error(`PIN 需要 ${VALIDATION.pinMinLength}–${VALIDATION.pinMaxLength} 位`)
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

// 差异提交（逐条 bind/unbind）而不是整体替换：后端没有“设置门店集合”的接口。
// 代价是没有事务——中途某条失败时，前面已成功的绑定已生效，而本地 bindings 不更新，
// 必须刷新才能看到真实状态；串行而非并发是为了失败时能定位到具体 storeId。
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
function openCreate() {
  createForm.value = { username: '', password: '', displayName: '', role: roleOptions.value[0] ?? 'OPERATOR' }
  createDialog.value = true
}

function openEdit(user: StaffUser) {
  editForm.value = { id: user.id, displayName: user.displayName, role: user.role }
  editDialog.value = true
}

function openReset(user: StaffUser) {
  resetForm.value = { id: user.id, username: user.username, password: '' }
  resetDialog.value = true
}

/** 前端预检只镜像后端规则（VALIDATION 由 schema 生成），最终以后端判定为准。 */
function usernameProblem(username: string): string | null {
  const pattern = new RegExp(VALIDATION.staffUsernamePattern)
  if (!pattern.test(username)) {
    return '账号只能是字母数字与 . _ -，长度 ' + VALIDATION.staffUsernameMinLength
      + '-' + VALIDATION.staffUsernameMaxLength
  }
  return null
}

async function saveCreate() {
  const username = createForm.value.username.trim()
  const problem = usernameProblem(username)
  if (problem) { ElMessage.error(problem); return }
  if (createForm.value.password.length < VALIDATION.passwordMinLength) {
    ElMessage.error('密码至少 ' + VALIDATION.passwordMinLength + ' 位')
    return
  }
  saving.value = true
  try {
    await api.post('/admin/v1/staff', {
      username,
      password: createForm.value.password,
      displayName: createForm.value.displayName.trim() || username,
      role: createForm.value.role
    })
    ElMessage.success('员工已创建')
    createDialog.value = false
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '创建失败')
  } finally {
    saving.value = false
  }
}

async function saveEdit() {
  saving.value = true
  try {
    await api.patch('/admin/v1/staff/' + editForm.value.id, {
      displayName: editForm.value.displayName.trim(),
      role: editForm.value.role
    })
    ElMessage.success('已保存')
    editDialog.value = false
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '保存失败')
  } finally {
    saving.value = false
  }
}

/** 供 StatusToggle 调用：失败必须抛出，让开关保持原状态并提示原因。 */
async function toggleStatus(user: StaffUser, next: boolean) {
  const saved = await api.post<StaffUser>('/admin/v1/staff/' + user.id + '/status', { active: next })
  Object.assign(user, saved)
  if (!next) {
    ElMessage.info('已停用，该员工的登录会话已被清除')
  }
}

async function saveReset() {
  if (resetForm.value.password.length < VALIDATION.passwordMinLength) {
    ElMessage.error('密码至少 ' + VALIDATION.passwordMinLength + ' 位')
    return
  }
  saving.value = true
  try {
    await api.post('/admin/v1/staff/' + resetForm.value.id + '/password-reset', {
      password: resetForm.value.password
    })
    ElMessage.success('密码已重置，该员工所有会话已被清除')
    resetDialog.value = false
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '重置失败')
  } finally {
    saving.value = false
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
      <el-button type="primary" @click="openCreate">新建员工</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-table :data="staff" @sort-change="onSortChange" stripe>
      <el-table-column prop="username" label="账号" width="160" sortable="custom" />
      <el-table-column prop="displayName" label="姓名" width="160" sortable="custom" />
      <el-table-column prop="role" label="角色" width="140" sortable="custom">
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
      <el-table-column prop="status" label="状态" width="130" sortable="custom">
        <template #default="{ row }">
          <!-- 状态就在列表里切：操作列不再重复一个"停用/启用"按钮 -->
          <StatusToggle :active="row.status === 'ACTIVE'" active-text="启用" inactive-text="停用"
                        :disabled="row.username === auth.user?.username"
                        :confirm-text="`停用 ${row.displayName}（${row.username}）会立即注销其登录会话，确定吗？`"
                        :on-change="next => toggleStatus(row, next)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="360">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link @click="openReset(row)">重置密码</el-button>
          <el-button link @click="openStores(row)">绑定门店</el-button>
          <el-button link @click="openPin(row)">设置 PIN</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="pinDialog" title="设置员工 PIN" width="420px">
      <p class="muted">
        为 {{ pinStaff?.displayName }}（{{ pinStaff?.username }}）设置 PIN，用于收银台锁屏解锁与退款店长授权。
      </p>
      <el-input v-model="pin" type="password" show-password :maxlength="VALIDATION.pinMaxLength" :placeholder="`${VALIDATION.pinMinLength}–${VALIDATION.pinMaxLength} 位数字或字符`" />
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
    <el-dialog v-model="createDialog" title="新建员工" width="480px">
      <el-form label-width="96px">
        <el-form-item label="账号" required>
          <el-input v-model="createForm.username"
                    :maxlength="VALIDATION.staffUsernameMaxLength"
                    placeholder="字母数字与 . _ -，如 zhangsan" />
        </el-form-item>
        <el-form-item label="初始密码" required>
          <el-input v-model="createForm.password" type="password" show-password
                    :placeholder="'至少 ' + VALIDATION.passwordMinLength + ' 位'" />
          <span class="muted">创建后请让员工自行「修改密码」；管理员可随时重置</span>
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="createForm.displayName" :maxlength="VALIDATION.staffDisplayNameMaxLength"
                    placeholder="留空则用账号" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="createForm.role" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role" :label="roleLabel(role)" :value="role" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialog" title="编辑员工" width="480px">
      <el-form label-width="96px">
        <el-form-item label="显示名">
          <el-input v-model="editForm.displayName" :maxlength="VALIDATION.staffDisplayNameMaxLength" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role" :label="roleLabel(role)" :value="role" />
          </el-select>
          <span class="muted">最后一个生效管理员不能改角色或被停用（后端会拦）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetDialog" title="重置密码" width="460px">
      <p class="muted">为 {{ resetForm.username }} 设置新密码，保存后该员工所有已登录会话会被清除。</p>
      <el-input v-model="resetForm.password" type="password" show-password
                :placeholder="'至少 ' + VALIDATION.passwordMinLength + ' 位'" />
      <template #footer>
        <el-button @click="resetDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveReset">重置</el-button>
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