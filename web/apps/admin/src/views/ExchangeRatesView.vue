<script setup lang="ts">
/**
 * 汇率（阶段 1）：手工维护 + 精确换算。
 *
 * <p>这里**不会改商品价或订单价**：订单仍按市场定价，历史订单不回算。
 * 换算用于报价/对账/客服解释，结果按目标币种小数位四舍五入（后端 BigDecimal）。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatAmount, formatDateTime } from '@fullmall/api-client'
import { api } from '../api'

interface RateRow {
  id: string
  baseCurrency: string
  quoteCurrency: string
  rate: number
  source: string
  effectiveAt: string
  createdBy: string
  createdAt: string
}

interface Conversion {
  amount: number
  fromCurrency: string
  convertedAmount: number
  toCurrency: string
  rate: number
  effectiveAt: string
  inverse: boolean
}

const rows = ref<RateRow[]>([])
const loading = ref(false)
const error = ref('')
const dialog = ref(false)
const saving = ref(false)

// 币种候选用浏览器内置 ISO 清单（与后端 CurrencyPrecision 同源），不手填
const currencies = computed(() => {
  const intl = Intl as typeof Intl & { supportedValuesOf?: (key: string) => string[] }
  return intl.supportedValuesOf?.('currency') ?? ['CNY', 'USD', 'HKD', 'JPY', 'EUR', 'KWD']
})

const form = reactive({
  baseCurrency: 'USD',
  quoteCurrency: 'CNY',
  rate: 7.2,
  source: 'MANUAL',
  effectiveAt: undefined as Date | undefined
})

const convertForm = reactive({
  amount: 10000,
  from: 'USD',
  to: 'CNY'
})
const conversion = ref<Conversion | null>(null)
const converting = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await api.get<RateRow[]>('/admin/v1/exchange-rates', { limit: 200 })
  } catch (cause) {
    error.value = '汇率加载失败：' + (cause instanceof Error ? cause.message : String(cause))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.baseCurrency = 'USD'
  form.quoteCurrency = 'CNY'
  form.rate = 7.2
  form.source = 'MANUAL'
  form.effectiveAt = new Date()
  dialog.value = true
}

async function save() {
  if (!form.baseCurrency || !form.quoteCurrency) {
    ElMessage.error('请选择源币种与目标币种')
    return
  }
  if (form.baseCurrency === form.quoteCurrency) {
    ElMessage.error('源币种与目标币种不能相同')
    return
  }
  if (!(form.rate > 0)) {
    ElMessage.error('汇率必须大于 0')
    return
  }
  saving.value = true
  try {
    await api.put('/admin/v1/exchange-rates', {
      baseCurrency: form.baseCurrency,
      quoteCurrency: form.quoteCurrency,
      rate: form.rate,
      source: form.source.trim(),
      effectiveAt: form.effectiveAt?.toISOString()
    })
    ElMessage.success('汇率已保存')
    dialog.value = false
    await load()
  } catch (cause) {
    ElMessage.error('保存失败：' + (cause instanceof Error ? cause.message : String(cause)))
  } finally {
    saving.value = false
  }
}

async function remove(row: RateRow) {
  try {
    await ElMessageBox.confirm(
      `删除 ${row.baseCurrency} → ${row.quoteCurrency} 在 ${formatDateTime(row.effectiveAt)} 的汇率？` +
      '删除后该时刻的换算会失效（历史订单不受影响）。',
      '确认删除汇率', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
  } catch {
    return
  }
  try {
    await api.delete(`/admin/v1/exchange-rates/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch (cause) {
    ElMessage.error('删除失败：' + (cause instanceof Error ? cause.message : String(cause)))
  }
}

async function convert() {
  converting.value = true
  conversion.value = null
  try {
    conversion.value = await api.get<Conversion>('/admin/v1/exchange-rates/convert', {
      amount: convertForm.amount,
      from: convertForm.from,
      to: convertForm.to
    })
  } catch (cause) {
    ElMessage.error('换算失败：' + (cause instanceof Error ? cause.message : String(cause)))
  } finally {
    converting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>汇率</h2>
        <p class="muted">
          阶段 1：手工维护 + 精确换算。不会自动改商品价或订单价，历史订单不回算。
        </p>
      </div>
      <div class="actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增汇率</el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <el-table :data="rows" stripe empty-text="暂无汇率">
      <el-table-column label="币种对" width="160">
        <template #default="{ row }">{{ row.baseCurrency }} → {{ row.quoteCurrency }}</template>
      </el-table-column>
      <el-table-column prop="rate" label="汇率" width="180" />
      <el-table-column prop="source" label="来源" width="140" />
      <el-table-column label="生效时间" min-width="200">
        <template #default="{ row }">{{ formatDateTime(row.effectiveAt) }}</template>
      </el-table-column>
      <el-table-column prop="createdBy" label="录入人" width="120" />
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <section class="convert">
      <h3>精确换算</h3>
      <p class="muted">
        金额按最小单位输入（CNY 分 / JPY 元 / KWD 千分之一第纳尔），结果按目标币种小数位四舍五入。
      </p>
      <div class="convert-row">
        <el-input-number v-model="convertForm.amount" :min="0" :step="100" />
        <el-select v-model="convertForm.from" filterable style="width: 120px">
          <el-option v-for="code in currencies" :key="code" :label="code" :value="code" />
        </el-select>
        <span>→</span>
        <el-select v-model="convertForm.to" filterable style="width: 120px">
          <el-option v-for="code in currencies" :key="code" :label="code" :value="code" />
        </el-select>
        <el-button type="primary" :loading="converting" @click="convert">换算</el-button>
      </div>
      <el-descriptions v-if="conversion" :column="3" border class="mt-16">
        <el-descriptions-item label="原金额">
          {{ formatAmount(conversion.amount, conversion.fromCurrency) }}
        </el-descriptions-item>
        <el-descriptions-item label="换算结果">
          <strong>{{ formatAmount(conversion.convertedAmount, conversion.toCurrency) }}</strong>
        </el-descriptions-item>
        <el-descriptions-item label="使用汇率">
          {{ conversion.rate }}{{ conversion.inverse ? '（反向推导）' : '' }}
        </el-descriptions-item>
      </el-descriptions>
    </section>

    <el-dialog v-model="dialog" title="新增汇率" width="520px">
      <el-form label-width="110px">
        <el-form-item label="源币种">
          <el-select v-model="form.baseCurrency" filterable style="width: 100%">
            <el-option v-for="code in currencies" :key="code" :label="code" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标币种">
          <el-select v-model="form.quoteCurrency" filterable style="width: 100%">
            <el-option v-for="code in currencies" :key="code" :label="code" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="汇率">
          <el-input-number v-model="form.rate" :min="0.000000001" :precision="10" :step="0.01"
                           style="width: 100%" />
          <div class="muted">1 源币种 = 汇率 目标币种</div>
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" maxlength="32" placeholder="例如 MANUAL / 中国银行现汇" />
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="form.effectiveAt" type="datetime" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.actions { display: flex; gap: 8px; }
.convert { margin-top: 24px; }
.convert-row { display: flex; align-items: center; gap: 10px; }
.mb-16 { margin-bottom: 16px; }
.mt-16 { margin-top: 16px; }
</style>