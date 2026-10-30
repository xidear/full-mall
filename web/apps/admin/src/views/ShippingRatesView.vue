<script setup lang="ts">
/**
 * 运费档位（市场 × 目的地 × 重量）。
 *
 * 币种由后端按市场取，页面不传：允许前端传币种就会出现"运费币种与订单币种不一致"的错账。
 * 目的地留空 = 该市场"其它目的地"的默认档；跨国没有档位时后端会明确报错（不默认免运费）。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface RateRow {
  id: string
  marketCode: string
  destinationCountry: string | null
  minWeightGrams: number
  maxWeightGrams: number | null
  feeAmount: number
  currency: string
  freeOverAmount: number | null
}

interface MarketView { code: string; name: string; currency: string }

const markets = ref<MarketView[]>([])
const rows = ref<RateRow[]>([])
const market = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const dialogVisible = ref(false)
const editing = reactive({
  id: '',
  destinationCountry: '',
  minWeightGrams: 0,
  maxWeightGrams: null as number | null,
  feeAmountYuan: 0,
  freeOverYuan: null as number | null
})

const currentCurrency = () => markets.value.find(m => m.code === market.value)?.currency ?? ''

async function loadMarkets() {
  markets.value = await api.get<MarketView[]>('/admin/v1/markets')
  if (!market.value && markets.value.length) market.value = markets.value[0].code
}

/**
 * 金额展示：后端 Jackson 配了 default-property-inclusion=non_null，**null 字段会被整个省略**，
 * 前端拿到的是 undefined。所以可空字段一律用 == null 判断（同时覆盖 null/undefined），
 * 金额再兜一层 Number.isFinite，避免把缺失值渲染成 NaN 这种明显错误。
 */
function formatAmount(amount: number | null | undefined, currency: string): string {
  if (amount == null || !Number.isFinite(amount)) {
    return '—'
  }
  return (amount / 100).toFixed(2) + ' ' + currency
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await api.get<RateRow[]>(`/admin/v1/shipping-rates?market=${encodeURIComponent(market.value)}`)
  } catch (cause) {
    error.value = '运费档位加载失败：' + (cause as Error).message
  } finally {
    loading.value = false
  }
}

async function onMarketChange() {
  await load()
}

function openCreate() {
  Object.assign(editing, {
    id: '', destinationCountry: '', minWeightGrams: 0, maxWeightGrams: null,
    feeAmountYuan: 0, freeOverYuan: null
  })
  dialogVisible.value = true
}

function openEdit(row: RateRow) {
  Object.assign(editing, {
    id: row.id,
    destinationCountry: row.destinationCountry ?? '',
    minWeightGrams: row.minWeightGrams,
    maxWeightGrams: row.maxWeightGrams,
    feeAmountYuan: row.feeAmount / 100,
    freeOverYuan: row.freeOverAmount == null ? null : row.freeOverAmount / 100
  })
  dialogVisible.value = true
}

async function save() {
  if (!market.value) { ElMessage.error('请先选择市场'); return }
  if (editing.maxWeightGrams !== null && editing.maxWeightGrams <= editing.minWeightGrams) {
    ElMessage.error('重量上限必须大于下限')
    return
  }
  saving.value = true
  try {
    const body = {
      marketCode: market.value,
      destinationCountry: editing.destinationCountry.trim() || null,
      minWeightGrams: editing.minWeightGrams,
      maxWeightGrams: editing.maxWeightGrams,
      // 金额按"分"提交（ADR-0004），页面用元输入再乘 100
      feeAmount: Math.round(editing.feeAmountYuan * 100),
      freeOverAmount: editing.freeOverYuan == null ? null : Math.round(editing.freeOverYuan * 100)
    }
    const path = editing.id
      ? `/admin/v1/shipping-rates/${editing.id}`
      : '/admin/v1/shipping-rates'
    rows.value = await api.put<RateRow[]>(path, body)
    ElMessage.success('已保存')
    dialogVisible.value = false
  } catch (cause) {
    ElMessage.error('保存失败：' + (cause as Error).message)
  } finally {
    saving.value = false
  }
}

async function remove(row: RateRow) {
  try {
    await api.delete(`/admin/v1/shipping-rates/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch (cause) {
    ElMessage.error('删除失败：' + (cause as Error).message)
  }
}

onMounted(async () => {
  await loadMarkets()
  await load()
})
</script>

<template>
  <section class="shipping-page">
    <div class="page-heading">
      <h1>运费档位</h1>
      <div class="actions">
        <el-select v-model="market" style="width: 160px" @change="onMarketChange">
          <el-option v-for="item in markets" :key="item.code" :label="`${item.code} · ${item.name}`" :value="item.code" />
        </el-select>
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增档位</el-button>
      </div>
    </div>

    <p class="hint">
      档位按「市场 × 目的地 × 重量」匹配，重量单位是克。目的地留空表示该市场的「其它目的地」默认档；
      <b>跨国没有档位时后端会拒绝下单</b>，不会默认免运费。币种固定为所选市场的币种（{{ currentCurrency() }}）。
    </p>

    <el-alert v-if="error" :title="error" type="error" :closable="false" />

    <el-table :data="rows" v-loading="loading" empty-text="暂无档位">
      <el-table-column label="目的地" width="120">
        <template #default="{ row }">{{ row.destinationCountry ?? '其它（默认）' }}</template>
      </el-table-column>
      <el-table-column label="重量区间（克）" min-width="160">
        <template #default="{ row }">
          {{ row.minWeightGrams }} – {{ row.maxWeightGrams ?? '不限' }}
        </template>
      </el-table-column>
      <el-table-column label="运费" width="120">
        <template #default="{ row }">{{ formatAmount(row.feeAmount, row.currency) }}</template>
      </el-table-column>
      <el-table-column label="满额免运费" width="180">
        <template #default="{ row }">
          {{ row.freeOverAmount == null ? '—' : `满 ${formatAmount(row.freeOverAmount, row.currency)} 免` }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑档位' : '新增档位'" width="520px">
      <el-form label-width="120px">
        <el-form-item label="目的地国家">
          <el-input v-model="editing.destinationCountry" placeholder="ISO 3166-1，留空=其它目的地默认档" />
        </el-form-item>
        <el-form-item label="重量下限（克）">
          <el-input-number v-model="editing.minWeightGrams" :min="0" />
        </el-form-item>
        <el-form-item label="重量上限（克）">
          <el-input-number v-model="editing.maxWeightGrams" :min="0" placeholder="留空=不限" />
        </el-form-item>
        <el-form-item :label="`运费（${currentCurrency()}）`">
          <el-input-number v-model="editing.feeAmountYuan" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="满额免运费">
          <el-input-number v-model="editing.freeOverYuan" :min="0" :precision="2" placeholder="留空=不免" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.actions { display: flex; gap: 8px; align-items: center; }
.hint { color: var(--text-muted, #666); margin: 0 0 16px; line-height: 1.6; }
</style>
