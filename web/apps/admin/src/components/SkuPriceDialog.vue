<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { formatAmount, toMinor } from '@fullmall/api-client'
import { api } from '../api'
import { apiErrorMessage } from '../composables/useAsyncState'

/**
 * 规格改价（商品内，单个商品作用域）。
 *
 * 为什么不是独立模块：价格属于"某商品某 SKU"，按"存储=256G"跨商品统一改价
 * 会把不相干的商品一起改掉。这里的作用域永远是当前这一个商品。
 *
 * 交互（按运营的实际操作顺序）：
 * 1. 左侧勾维度（存储 / 颜色…）；
 * 2. 右侧出现该维度的取值，勾选要改的值（同维度内是"或"，跨维度是"且"）；
 * 3. 下方选 加价/减价 + 金额，右侧实时预览命中的 SKU 与原价 → 新价；
 * 4. 确定 = 整批成功或整批失败。
 */
const props = defineProps<{
  modelValue: boolean
  productId: string
  productTitle?: string
}>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; applied: [] }>()

interface Change { skuId: string; skuCode: string; before: number; after: number }

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const dimensions = ref<Record<string, string[]>>({})
const dimensionsError = ref('')
const selectedDimensions = ref<string[]>([])
const selectedValues = ref<Record<string, string[]>>({})
const direction = ref<'ADD' | 'SUB'>('ADD')
const amount = ref(10)
const preview = ref<Change[]>([])
const previewError = ref('')
const loadingPreview = ref(false)
const applying = ref(false)

function reset() {
  selectedDimensions.value = []
  selectedValues.value = {}
  direction.value = 'ADD'
  amount.value = 10
  preview.value = []
  previewError.value = ''
}

async function loadDimensions() {
  dimensionsError.value = ''
  try {
    dimensions.value = await api.get<Record<string, string[]>>(
      '/admin/v1/products/' + props.productId + '/sku-dimensions')
    if (Object.keys(dimensions.value).length === 0) {
      dimensionsError.value = '该商品还没有规格维度（先配置规格矩阵并生成 SKU）'
    }
  } catch (cause) {
    dimensions.value = {}
    dimensionsError.value = apiErrorMessage(cause, '规格维度加载失败')
  }
}

watch(() => props.modelValue, async (open) => {
  if (!open) return
  reset()
  await loadDimensions()
})

function toggleDimension(name: string, checked: boolean) {
  if (checked) {
    if (!selectedDimensions.value.includes(name)) selectedDimensions.value.push(name)
  } else {
    selectedDimensions.value = selectedDimensions.value.filter(item => item !== name)
    delete selectedValues.value[name]
  }
  schedulePreview()
}

function toggleValue(name: string, value: string, checked: boolean) {
  const current = selectedValues.value[name] ?? []
  selectedValues.value = {
    ...selectedValues.value,
    [name]: checked ? [...current, value] : current.filter(item => item !== value)
  }
  schedulePreview()
}

const match = computed(() => {
  const result: Record<string, string[]> = {}
  for (const name of selectedDimensions.value) {
    const values = selectedValues.value[name] ?? []
    if (values.length > 0) result[name] = values
  }
  return result
})
const hasSelection = computed(() => Object.keys(match.value).length > 0)
// 界面按"元"输入（输入框旁标了 元），后端一律收"分"：
// 不做这层换算的话，输入 10 元只会加 10 分（0.1 元），且预览看起来"正常"。
const amountInCents = computed(() => toMinor(amount.value || 0, 'CNY'))
const signedAmount = computed(() => (direction.value === 'SUB' ? -amountInCents.value : amountInCents.value))

let timer: ReturnType<typeof setTimeout> | undefined
function schedulePreview() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { void runPreview() }, 250)
}

async function runPreview() {
  previewError.value = ''
  preview.value = []
  if (!hasSelection.value) return
  if (!amount.value || amount.value <= 0) {
    previewError.value = '请填写大于 0 的金额'
    return
  }
  loadingPreview.value = true
  try {
    preview.value = await api.post<Change[]>(
      '/admin/v1/products/' + props.productId + '/price-rule/preview-multi',
      { match: match.value, mode: 'ADD', value: signedAmount.value })
  } catch (cause) {
    previewError.value = apiErrorMessage(cause, '预览失败')
  } finally {
    loadingPreview.value = false
  }
}

async function apply() {
  if (!hasSelection.value) { previewError.value = '请至少勾选一个规格值'; return }
  if (!amount.value || amount.value <= 0) { previewError.value = '请填写大于 0 的金额'; return }
  applying.value = true
  previewError.value = ''
  try {
    const result = await api.post<{ updated: number }>(
      '/admin/v1/products/' + props.productId + '/price-rule/apply-multi',
      { match: match.value, mode: 'ADD', value: signedAmount.value })
    ElMessage.success('已调整 ' + result.updated + ' 个规格的价格')
    emit('applied')
    visible.value = false
  } catch (cause) {
    previewError.value = apiErrorMessage(cause, '改价失败')
  } finally {
    applying.value = false
  }
}

function yuan(amountInCents: number): string {
  // 规格改价预览接口只返回最小单位金额（无币种）；当前 SKU 均为 CNY
  return formatAmount(amountInCents, 'CNY')
}
</script>

<template>
  <el-dialog v-model="visible" :title="'规格改价 · ' + (productTitle || '')" width="760px" append-to-body>
    <el-alert v-if="dimensionsError" :title="dimensionsError" type="warning" show-icon
              :closable="false" class="mb-12" />

    <div v-else class="price-grid">
      <section class="pane">
        <h4>规格（勾选维度）</h4>
        <p class="muted">先选维度，再在右侧勾取值</p>
        <el-checkbox
          v-for="(values, name) in dimensions"
          :key="name"
          :model-value="selectedDimensions.includes(name)"
          @change="(value: boolean | string | number) => toggleDimension(name, Boolean(value))"
        >{{ name }}<span class="count">（{{ values.length }} 个取值）</span></el-checkbox>
      </section>

      <section class="pane">
        <h4>取值（勾选要改的值）</h4>
        <p v-if="selectedDimensions.length === 0" class="muted">左侧勾选一个维度后，这里显示它的取值</p>
        <template v-for="name in selectedDimensions" :key="name">
          <div class="value-group">
            <div class="value-title">{{ name }}</div>
            <el-checkbox
              v-for="value in dimensions[name]"
              :key="value"
              :model-value="(selectedValues[name] ?? []).includes(value)"
              @change="(checked: boolean | string | number) => toggleValue(name, value, Boolean(checked))"
            >{{ value }}</el-checkbox>
          </div>
        </template>
      </section>
    </div>

    <div class="rule-row">
      <el-radio-group v-model="direction" @change="schedulePreview">
        <el-radio-button value="ADD">加价</el-radio-button>
        <el-radio-button value="SUB">减价</el-radio-button>
      </el-radio-group>
      <el-input-number v-model="amount" :min="0.01" :precision="2" :step="1" controls-position="right"
                       @change="schedulePreview" />
      <span class="muted">元</span>
      <el-button :loading="loadingPreview" @click="runPreview">预览</el-button>
      <span v-if="hasSelection" class="muted">命中 {{ preview.length }} 个规格</span>
    </div>

    <el-alert v-if="previewError" :title="previewError" type="error" show-icon :closable="false" class="mb-12" />

    <el-table v-if="preview.length" :data="preview" max-height="260" size="small">
      <el-table-column prop="skuCode" label="SKU" min-width="200" />
      <el-table-column label="改前" width="120">
        <template #default="{ row }">{{ yuan(row.before) }}</template>
      </el-table-column>
      <el-table-column label="改后" width="120">
        <template #default="{ row }">{{ yuan(row.after) }}</template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="applying" :disabled="!preview.length" @click="apply">
        确定改价
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.price-grid {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 16px;
  margin-bottom: 14px;
}

.pane {
  border: 1px solid #ece9e2;
  border-radius: 10px;
  padding: 12px;
  min-height: 140px;
}

.pane h4 {
  margin: 0 0 4px;
  font-size: 14px;
}

.pane .muted {
  margin: 0 0 10px;
  font-size: 12px;
}

.count {
  color: #767b81;
  font-size: 12px;
}

.value-group {
  margin-bottom: 10px;
}

.value-title {
  font-size: 12px;
  color: #5f666d;
  margin-bottom: 4px;
}

.rule-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding-top: 6px;
}

.mb-12 {
  margin-bottom: 12px;
}
</style>
