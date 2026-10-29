<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { apiErrorMessage } from '../composables/useAsyncState'

/**
 * 规格矩阵模板（不是"某个商品的规格"，而是可复用模板）。
 *
 * 设计要点（2026-10-28 设计审查）：
 * 1. 维度与取值用「标签 + 输入框」，组合用「勾选框」——不再让运营手写 "颜色:黑,白;容量:64G,128G"；
 * 2. 模板**不含价格**：未绑定商品时价格没有意义，应用模板到商品时才定价；
 * 3. 模板可独立创建、可被多个商品复用；被引用时删除会被拒绝。
 */

interface TemplateView {
  id: string
  code: string
  name: string
  note: string | null
  dimensions: Record<string, string[]>
  disabled: Array<Record<string, string>>
  combinationCount: number
  enabledCombinationCount: number
  usedByProducts: number
}

interface DimensionDraft {
  name: string
  values: string[]
}

const templates = ref<TemplateView[]>([])
const loading = ref(false)
const error = ref('')
const dialog = ref(false)
const saving = ref(false)
const dialogError = ref('')
const editingId = ref('')

const form = reactive({
  code: '',
  name: '',
  note: '',
  dimensions: [] as DimensionDraft[]
})

const valueInputs = ref<Record<number, string>>({})
const disabledKeys = ref<Set<string>>(new Set())

function emptyDimension(): DimensionDraft {
  return { name: '', values: [] }
}

function openCreate() {
  editingId.value = ''
  dialogError.value = ''
  form.code = ''
  form.name = ''
  form.note = ''
  form.dimensions = [emptyDimension()]
  valueInputs.value = {}
  disabledKeys.value = new Set()
  dialog.value = true
}

function openEdit(row: TemplateView) {
  editingId.value = row.id
  dialogError.value = ''
  form.code = row.code
  form.name = row.name
  form.note = row.note ?? ''
  form.dimensions = Object.entries(row.dimensions).map(([name, values]) => ({ name, values: [...values] }))
  valueInputs.value = {}
  disabledKeys.value = new Set(row.disabled.map(combination => combinationKey(combination)))
  dialog.value = true
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    templates.value = await api.get<TemplateView[]>('/admin/v1/sku-matrix-templates')
  } catch (cause) {
    templates.value = []
    error.value = apiErrorMessage(cause, '规格矩阵模板加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

// ---- 维度与取值：标签式编辑 -------------------------------------------------

function addDimension() {
  if (form.dimensions.length >= 4) {
    ElMessage.warning('维度最多 4 个')
    return
  }
  form.dimensions.push(emptyDimension())
}

function removeDimension(index: number) {
  form.dimensions.splice(index, 1)
  pruneDisabled()
}

function addValue(index: number) {
  const raw = (valueInputs.value[index] ?? '').trim()
  if (!raw) return
  const dimension = form.dimensions[index]
  if (!dimension) return
  if (dimension.values.includes(raw)) {
    ElMessage.warning('取值重复：' + raw)
    return
  }
  dimension.values.push(raw)
  valueInputs.value[index] = ''
}

function removeValue(index: number, value: string) {
  const dimension = form.dimensions[index]
  if (!dimension) return
  dimension.values = dimension.values.filter(item => item !== value)
  pruneDisabled()
}

// ---- 组合：勾选框 -----------------------------------------------------------

// 组合键规则（按维度名排序后拼 name=value）必须与后端一致：它是“禁用组合”的唯一标识。
// 排序是必需的——JSON 对象字段顺序不保证稳定，不排序同一组合会得到两个键，
// 编辑已有模板时勾选状态就会整片错位（看起来“全都启用了”）。
function combinationKey(combination: Record<string, string>): string {
  return Object.entries(combination)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([name, value]) => name + '=' + value)
    .join('|')
}

const combinations = computed<Array<{ key: string; label: string; combination: Record<string, string> }>>(() => {
  const dimensions = form.dimensions.filter(dimension => dimension.name.trim() && dimension.values.length > 0)
  if (dimensions.length === 0) return []
  let result: Array<{ key: string; label: string; combination: Record<string, string> }> = [
    { key: '', label: '', combination: {} }
  ]
  for (const dimension of dimensions) {
    const next: typeof result = []
    for (const item of result) {
      for (const value of dimension.values) {
        const combination = { ...item.combination, [dimension.name.trim()]: value }
        next.push({
          key: combinationKey(combination),
          label: Object.values(combination).join(' / '),
          combination
        })
      }
    }
    result = next
  }
  // 硬上限 500：勾选框一次全渲染，再多会明显卡顿。副作用是超出部分既不显示也无法单独禁用，
  // 属于静默截断——组合很多的模板要留意。
  return result.slice(0, 500)
})

const enabledCount = computed(() =>
  combinations.value.filter(item => !disabledKeys.value.has(item.key)).length)

function toggleCombination(key: string, enabled: boolean) {
  const next = new Set(disabledKeys.value)
  if (enabled) next.delete(key)
  else next.add(key)
  disabledKeys.value = next
}

// 禁用键必须与当前组合求交集：删维度/取值后不清理，保存时会提交“不存在的组合”，
// 后端要么拒绝要么写入脏数据。
function pruneDisabled() {
  const keys = new Set(combinations.value.map(item => item.key))
  disabledKeys.value = new Set([...disabledKeys.value].filter(key => keys.has(key)))
}

async function save() {
  dialogError.value = ''
  if (!form.code.trim() || !form.name.trim()) {
    dialogError.value = '模板编码与名称必填'
    return
  }
  const dimensions: Record<string, string[]> = {}
  for (const dimension of form.dimensions) {
    if (!dimension.name.trim()) {
      dialogError.value = '维度名称不能为空'
      return
    }
    if (dimension.values.length === 0) {
      dialogError.value = '维度「' + dimension.name + '」至少需要一个取值'
      return
    }
    dimensions[dimension.name.trim()] = dimension.values
  }
  if (Object.keys(dimensions).length === 0) {
    dialogError.value = '至少需要一个维度'
    return
  }
  const disabled = combinations.value
    .filter(item => disabledKeys.value.has(item.key))
    .map(item => item.combination)

  saving.value = true
  try {
    const payload = {
      code: form.code.trim(),
      name: form.name.trim(),
      note: form.note.trim() || null,
      dimensions,
      disabled
    }
    if (editingId.value) {
      await api.put('/admin/v1/sku-matrix-templates/' + editingId.value, payload)
      ElMessage.success('模板已更新')
    } else {
      await api.post('/admin/v1/sku-matrix-templates', payload)
      ElMessage.success('模板已创建')
    }
    dialog.value = false
    await load()
  } catch (cause) {
    dialogError.value = apiErrorMessage(cause, '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: TemplateView) {
  try {
    await ElMessageBox.confirm(
      row.usedByProducts > 0
        ? '该模板正被 ' + row.usedByProducts + ' 个商品使用，删除前请先解绑。仍要尝试删除吗？'
        : '删除后不可恢复，确定删除模板「' + row.name + '」？',
      '删除规格矩阵模板', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
  } catch {
    return
  }
  try {
    await api.delete('/admin/v1/sku-matrix-templates/' + row.id)
    ElMessage.success('已删除')
    await load()
  } catch (cause) {
    error.value = apiErrorMessage(cause, '删除失败')
  }
}

function dimensionSummary(row: TemplateView) {
  return Object.entries(row.dimensions)
    .map(([name, values]) => name + '：' + values.join(' / '))
    .join('；')
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>规格矩阵模板</h2>
        <p class="muted">
          模板只描述「维度 + 取值 + 禁用组合」，不含价格；商品编辑页选择模板后输入价格即可生成 SKU。
        </p>
      </div>
      <el-button type="primary" @click="openCreate">新建模板</el-button>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <el-table :data="templates" empty-text="还没有模板，点击右上角新建">
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="code" label="编码" width="180" />
      <el-table-column label="维度与取值" min-width="260">
        <template #default="{ row }">{{ dimensionSummary(row) }}</template>
      </el-table-column>
      <el-table-column label="组合" width="140">
        <template #default="{ row }">
          {{ row.enabledCombinationCount }} / {{ row.combinationCount }} 可用
        </template>
      </el-table-column>
      <el-table-column label="被引用" width="110">
        <template #default="{ row }">{{ row.usedByProducts }} 个商品</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editingId ? '编辑规格矩阵模板' : '新建规格矩阵模板'" width="720px">
      <el-alert v-if="dialogError" :title="dialogError" type="error" show-icon :closable="false" class="mb-16" />
      <el-form label-position="top">
        <div class="policy-grid">
          <el-form-item label="模板编码">
            <el-input v-model="form.code" placeholder="例如 PHONE-COLOR-MEM（大写字母/数字/-/_）" />
          </el-form-item>
          <el-form-item label="模板名称">
            <el-input v-model="form.name" placeholder="例如 手机颜色×容量" />
          </el-form-item>
        </div>
        <el-form-item label="备注（可选）">
          <el-input v-model="form.note" placeholder="说明适用品类或注意事项" />
        </el-form-item>

        <section class="form-section">
          <h3>维度与取值</h3>
          <p>每个维度一个标签组：输入取值后回车或点「添加」变成标签，点标签上的 × 删除。模板不含价格。</p>
          <div v-for="(dimension, index) in form.dimensions" :key="index" class="dimension-row">
            <el-input v-model="dimension.name" class="dimension-name" placeholder="维度名，例如 颜色" />
            <div class="tag-box">
              <el-tag v-for="value in dimension.values" :key="value" closable
                      @close="removeValue(index, value)">{{ value }}</el-tag>
              <el-input v-model="valueInputs[index]" class="value-input" placeholder="输入取值后回车"
                        @keyup.enter="addValue(index)" />
              <el-button class="value-add" @click="addValue(index)">添加</el-button>
            </div>
            <el-button text type="danger" class="dimension-remove" @click="removeDimension(index)">
              删除维度
            </el-button>
          </div>
          <el-button @click="addDimension">+ 添加维度</el-button>
        </section>

        <section class="form-section">
          <h3>可用组合（勾选表示启用）</h3>
          <p v-if="combinations.length === 0" class="muted">先添加维度与取值。</p>
          <template v-else>
            <p class="muted">共 {{ combinations.length }} 个组合，已启用 {{ enabledCount }} 个；取消勾选即写入禁用组合。</p>
            <div class="combo-grid">
              <el-checkbox
                v-for="item in combinations"
                :key="item.key"
                :model-value="!disabledKeys.has(item.key)"
                @change="(value: boolean) => toggleCombination(item.key, Boolean(value))"
              >{{ item.label }}</el-checkbox>
            </div>
          </template>
        </section>
      </el-form>

      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存模板</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

/* 一行里三种控件必须同一基线、同一高度：
   维度名输入框、取值输入框、添加按钮都用 Element Plus 默认尺寸（32px），
   取值容器用 min-height: 32px 与之等高；标签多时容器向下长，行内其它控件仍顶部对齐。 */
.dimension-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.dimension-row .dimension-name {
  width: 200px;
  flex: none;
}

.dimension-row .value-input {
  width: 150px;
  flex: none;
}

.dimension-row .dimension-remove {
  height: 32px;
  flex: none;
}

.tag-box {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-height: 32px;
  padding: 0 8px;
  border-radius: 8px;
  /* 用 inset 阴影代替 1px 边框：边框会让容器比左侧输入框高 2px（32 → 34），
     这正是"维度名和取值上下不齐"的来源；阴影不占布局空间。 */
  background: #faf9f6;
  box-shadow: inset 0 0 0 1px #e6e3dc;
}

/* 标签换行时按 8px 行距排布；单行时容器正好 32px，与左侧输入框严格等高 */
.tag-box {
  align-content: center;
  row-gap: 8px;
}

.combo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 6px 12px;
}
</style>
