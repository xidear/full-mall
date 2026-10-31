<script setup lang="ts">
/**
 * 分市场价格（ADR-0013 下游）。
 *
 * 只编辑「覆盖价」：清空覆盖价 = 回到 SKU 基础价，不是把价格改成 0。
 * 币种由后端按市场强制，页面不传——否则会出现"标错币种的价"，最终以错误金额收款。
 */
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { currencyDigits, formatAmount, toMajor, toMinor } from '@fullmall/api-client'
import { api } from '../api'

const props = defineProps<{
  modelValue: boolean
  skuId: string
  skuCode: string
  baseAmount: number
  baseCurrency: string
}>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; changed: [] }>()

interface MarketView { code: string; name: string; currency: string }
interface PriceRow { marketCode: string; currency: string; amount: number; compareAtAmount: number | null }

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const markets = ref<MarketView[]>([])
const overrides = ref<Record<string, string>>({})
const loading = ref(false)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const [marketList, priceRows] = await Promise.all([
      api.get<MarketView[]>('/admin/v1/markets'),
      api.get<PriceRow[]>(`/admin/v1/skus/${props.skuId}/prices`)
    ])
    markets.value = marketList
    const next: Record<string, string> = {}
    for (const market of marketList) {
      const row = priceRows.find(price => price.marketCode === market.code)
      next[market.code] = row ? toMajor(row.amount, row.currency).toFixed(currencyDigits(row.currency)) : ''
    }
    overrides.value = next
  } catch (cause) {
    ElMessage.error('分市场价格加载失败：' + (cause as Error).message)
  } finally {
    loading.value = false
  }
}

async function saveMarket(market: MarketView) {
  const raw = (overrides.value[market.code] ?? '').trim()
  saving.value = true
  try {
    if (!raw) {
      // 空 = 清除覆盖价 → 回到基础价
      await api.delete(`/admin/v1/skus/${props.skuId}/prices/${market.code}`).catch(() => null)
      ElMessage.success(`${market.code} 已回到基础价`)
    } else {
      const amount = toMinor(Number(raw), market.currency)
      if (!Number.isFinite(amount) || amount <= 0) {
        ElMessage.error('金额必须大于 0')
        return
      }
      await api.put(`/admin/v1/skus/${props.skuId}/prices/${market.code}`, {
        currency: market.currency,
        amount
      })
      ElMessage.success(`${market.code} 已保存`)
    }
    emit('changed')
    await load()
  } catch (cause) {
    ElMessage.error('保存失败：' + (cause as Error).message)
  } finally {
    saving.value = false
  }
}

watch(() => props.modelValue, (open) => { if (open) load() })
</script>

<template>
  <el-dialog v-model="visible" :title="`分市场价格 · ${skuCode}`" width="560px" append-to-body>
    <p class="hint">
      基础价：<b>{{ formatAmount(baseAmount, baseCurrency) }}</b>。
      留空某个市场 = 该市场沿用基础价（币种必须与市场一致，否则下单会被拒绝）。
    </p>
    <el-table :data="markets" v-loading="loading" size="small">
      <el-table-column label="市场" min-width="140">
        <template #default="{ row }">{{ row.code }} · {{ row.name }}</template>
      </el-table-column>
      <el-table-column label="币种" width="80">
        <template #default="{ row }">{{ row.currency }}</template>
      </el-table-column>
      <el-table-column label="覆盖价（市场币种）" width="160">
        <template #default="{ row }">
          <el-input v-model="overrides[row.code]" :placeholder="`基础价 ${formatAmount(baseAmount, baseCurrency)}`" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" :loading="saving" @click="saveMarket(row)">保存</el-button>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.hint { color: var(--text-muted, #666); margin: 0 0 12px; line-height: 1.6; }
</style>
