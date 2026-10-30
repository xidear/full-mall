<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import { PERMISSION_DOCS, PERMISSION_RELATIONS, type PermissionMatrixView, type PermissionView } from '@fullmall/api-client'
import { api } from '../api'

const matrix = ref<PermissionMatrixView | null>(null)
const selectedRole = ref('OPERATOR')
const selected = ref<string[]>([])
/** 当前角色是否可编辑：由 role.editable 决定（ADMIN/SUPPLIER 为 false），不再靠字符串判断。 */
const currentRoleEditable = computed(() =>
  roles.value.find(item => item.role === selectedRole.value)?.editable ?? false)
const loading = ref(false)
const error = ref('')

interface RoleOption {
  role: string
  label: string
  editable: boolean
}

// 角色候选来自后端 role 表（ADR-0015），这里只维护"不可编辑角色"的展示后缀
const roles = ref<RoleOption[]>([])
const roleLabels = computed<Record<string, string>>(() =>
  Object.fromEntries(roles.value.map(item => [item.role, item.editable ? item.label : item.label + '（全部权限）'])))

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
    const [loaded, roleList] = await Promise.all([
      api.get<PermissionMatrixView>('/admin/v1/permissions'),
      api.get<RoleOption[]>('/admin/v1/permissions/roles')
    ])
    matrix.value = loaded
    roles.value = roleList
    if (!roles.value.some(item => item.role === selectedRole.value)) {
      selectedRole.value = roles.value.find(item => item.editable)?.role ?? roles.value[0]?.role ?? 'OPERATOR'
    }
    const role = matrix.value.roles.find(item => item.role === selectedRole.value)
    selected.value = role ? [...role.permissions] : []
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '权限矩阵加载失败'
  } finally {
    loading.value = false
  }
}

function toggle(code: string) {
  if (!currentRoleEditable.value) return
  selected.value = selected.value.includes(code)
    ? selected.value.filter(item => item !== code)
    : [...selected.value, code]
}

async function save() {
  if (!currentRoleEditable.value) return
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
  return !currentRoleEditable.value || selected.value.includes(code)
}

/** 说明来自生成产物（docs/permissions.catalog.json + PermissionService 路径映射），页面不手写文案。 */
function docOf(code: string) {
  return PERMISSION_DOCS[code]
}

function endpointsText(code: string): string {
  const doc = docOf(code)
  if (!doc) return '—'
  if (doc.endpoints.length > 0) return doc.endpoints.join('、')
  return doc.fallback ? '兜底：所有未登记路径' : '—'
}

function relationText(code: string): string {
  const doc = docOf(code)
  // PERMISSION_RELATIONS 是 as const（字面量类型），这里显式标成 string[] 才能 push 别的文案
  const lines: string[] = [PERMISSION_RELATIONS.inheritance]
  if (doc?.fallback) lines.push(PERMISSION_RELATIONS.fallback)
  lines.push(PERMISSION_RELATIONS.menu)
  return lines.join(' ')
}

/** 原生 title：hover 之外，读屏/alt 也能拿到完整说明（悬停提示只是好看，不能是唯一出口）。 */
function hoverText(permission: PermissionView): string {
  const doc = docOf(permission.code)
  if (!doc) {
    return permission.code + '：尚无说明，请补 docs/permissions.catalog.json 后重新生成'
  }
  return [
    doc.summary,
    '勾选后可做：' + (doc.grants.length ? doc.grants.join('；') : '—'),
    '取消后：' + (doc.revokes.length ? doc.revokes.join('；') : '—'),
    '关联接口：' + endpointsText(permission.code),
    '关系：无父级、无继承（逐个授予）；ADMIN 始终拥有'
  ].join('\n')
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
            每项右侧 ⓘ 悬停可看：勾选/取消分别影响哪些操作、拦截哪些接口、有无继承关系。
          </p>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>

      <el-radio-group v-model="selectedRole" class="role-tabs">
        <el-radio-button v-for="role in roles.map(item => item.role)" :key="role" :label="role">
          {{ roleLabels[role] }}
        </el-radio-button>
      </el-radio-group>

      <div v-if="!currentRoleEditable" class="admin-note">
        ADMIN 拥有全部权限，无需配置。
      </div>

      <div class="permission-groups">
        <section v-for="[group, permissions] in groups" :key="group" class="permission-group">
          <h3>{{ group }}</h3>
          <label v-for="permission in permissions" :key="permission.code"
                 class="permission-row" :title="hoverText(permission)">
            <el-checkbox
              :model-value="checked(permission.code)"
              :disabled="!currentRoleEditable"
              @change="toggle(permission.code)"
            />
            <span class="permission-text">
              <strong>{{ permission.label }}</strong>
              <code>{{ permission.code }}</code>
            </span>
            <el-tooltip placement="right" effect="light" :show-after="120" popper-class="perm-tooltip">
              <template #content>
                <div class="perm-tip">
                  <p class="perm-tip-title">{{ permission.label }} · {{ permission.code }}</p>
                  <p v-if="docOf(permission.code)" class="perm-tip-summary">{{ docOf(permission.code)?.summary }}</p>
                  <p v-else class="perm-tip-missing">
                    该权限还没有说明：请在 docs/permissions.catalog.json 补一条后重新生成
                  </p>
                  <template v-if="docOf(permission.code)">
                    <p class="perm-tip-label">勾选后可做</p>
                    <ul>
                      <li v-for="item in docOf(permission.code)?.grants ?? []" :key="item">{{ item }}</li>
                    </ul>
                    <p class="perm-tip-label">取消后</p>
                    <ul>
                      <li v-for="item in docOf(permission.code)?.revokes ?? []" :key="item">{{ item }}</li>
                    </ul>
                    <p class="perm-tip-label">关联接口</p>
                    <p class="perm-tip-mono">{{ endpointsText(permission.code) }}</p>
                    <p class="perm-tip-label">关系</p>
                    <p class="perm-tip-rel">{{ relationText(permission.code) }}</p>
                    <p v-if="docOf(permission.code)?.note" class="perm-tip-note">
                      注意：{{ docOf(permission.code)?.note }}
                    </p>
                  </template>
                </div>
              </template>
              <el-icon class="perm-help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </label>
        </section>
      </div>

      <div class="save-row">
        <el-button type="primary" :disabled="!currentRoleEditable" :loading="loading" @click="save">
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

.permission-text {
  flex: 1;
}

.perm-help {
  margin-left: auto;
  color: #a8b0b8;
  cursor: help;
}

.perm-help:hover {
  color: var(--el-color-primary, #c4322b);
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

<!-- Element Plus 的 tooltip 渲染在 body 上，scoped 选择器打不到，这段必须保持非 scoped -->
<style>
.perm-tooltip {
  max-width: 380px;
}

.perm-tooltip .perm-tip {
  color: #40453f;
  font-size: 12px;
  line-height: 1.55;
}

.perm-tooltip .perm-tip-title {
  margin: 0 0 4px;
  color: #1f2421;
  font-weight: 700;
}

.perm-tooltip .perm-tip-summary {
  margin: 0 0 8px;
}

.perm-tooltip .perm-tip-label {
  margin: 8px 0 2px;
  color: #6b5316;
  font-weight: 600;
}

.perm-tooltip .perm-tip ul {
  margin: 0;
  padding-left: 16px;
}

.perm-tooltip .perm-tip li {
  margin: 1px 0;
}

.perm-tooltip .perm-tip-mono {
  margin: 0;
  color: #5c574e;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  word-break: break-all;
}

.perm-tooltip .perm-tip-rel {
  margin: 0;
  color: #6b716c;
}

.perm-tooltip .perm-tip-note,
.perm-tooltip .perm-tip-missing {
  margin: 8px 0 0;
  color: #b23b2e;
}
</style>
