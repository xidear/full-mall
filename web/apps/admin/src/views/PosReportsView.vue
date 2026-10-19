<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { posSaleStatusLabel, posShiftStatusLabel } from '@fullmall/api-client'
import type {
  PosDailyReport,
  PosReconciliationReport,
  PosSaleView,
  Store
} from '@fullmall/api-client'
import { api } from '../api'

const stores = ref<Store[]>([])
const storeId = ref('')
const date = ref(new Date().toISOString().slice(0, 10))
const report = ref<PosDailyReport | null>(null)
const reconciliation = ref<PosReconciliationReport | null>(null)
const sales = ref<PosSaleView[]>([])
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  await loadStores()
  await load()
})

async function loadStores() {
  try {
    stores.value = await api.get<Store[]>('/api/v1/stores')
    storeId.value = stores.value[0]?.id ?? ''
  } catch (cause) {
    error.value = messageOf(cause, '门店加载失败')
  }
}

async function load() {
  if (!storeId.value) return
  loading.value = true
  error.value = ''
  try {
    const [daily, recon, recent] = await Promise.all([
      api.get<PosDailyReport>('/admin/v1/pos/reports/daily', { storeId: storeId.value, date: date.value }),
      api.get<PosReconciliationReport>('/admin/v1/pos/reconciliation', { storeId: storeId.value, date: date.value }),
      api.get<PosSaleView[]>('/admin/v1/pos/sales', { storeId: storeId.value, date: date.value })
    ])
    report.value = daily
    reconciliation.value = recon
    sales.value = recent
  } catch (cause) {
    error.value = messageOf(cause, '收银报表加载失败')
  } finally {
    loading.value = false
  }
}

function money(value?: { amount: number; currency: string }) {
  if (!value) return '-'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: value.currency }).format(value.amount / 100)
}

function tenderLabel(type: string) {
  const labels: Record<string, string> = {
    CASH: '现金',
    WECHAT: '微信扫码',
    ALIPAY: '支付宝',
    CARD: '银行卡',
    STORED_VALUE: '会员储值',
    OTHER: '其他收款'
  }
  return labels[type] ?? type
}

function messageOf(cause: unknown, fallback: string) {
  return cause instanceof Error ? cause.message : fallback
}
</script>

<template>
  <div v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2>门店收银日结</h2>
          <p class="muted">按门店与营业日期汇总销售、退款、优惠、积分、券、现金差异与支付方式。</p>
        </div>
        <div class="filters">
          <el-select v-model="storeId" placeholder="选择门店" style="width: 210px" @change="load">
            <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
          </el-select>
          <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" @change="load" />
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <div class="metric-grid">
        <div class="metric-card">
          <span>净销售额</span>
          <strong>{{ money(report?.netSales) }}</strong>
          <small>{{ report?.salesCount ?? 0 }} 笔销售 · {{ report?.shiftCount ?? 0 }} 个班次</small>
        </div>
        <div class="metric-card">
          <span>优惠合计</span>
          <strong>{{ money(report?.discountTotal) }}</strong>
          <small>券 {{ money(report?.couponDiscount) }} · 积分 {{ money(report?.pointsDiscount) }}</small>
        </div>
        <div class="metric-card">
          <span>退款</span>
          <strong>{{ money(report?.refundTotal) }}</strong>
          <small>{{ report?.refundCount ?? 0 }} 笔整单退款</small>
        </div>
        <div class="metric-card">
          <span>现金收入</span>
          <strong>{{ money(report?.cashTotal) }}</strong>
          <small>现金差异 {{ money(report?.cashVariance) }}</small>
        </div>
        <div class="metric-card">
          <span>积分</span>
          <strong>{{ report?.pointsRedeemed ?? 0 }}</strong>
          <small>抵扣 · 发放 {{ report?.pointsEarned ?? 0 }}</small>
        </div>
        <div class="metric-card">
          <span>优惠券核销</span>
          <strong>{{ report?.couponsUsed ?? 0 }}</strong>
          <small>张</small>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2>支付方式汇总</h2>
          <p class="muted">Mock 渠道的应收与实收在演示环境中一致；接入真实收单后由对账任务写入实收差异。</p>
        </div>
      </div>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-table :data="report?.tenderMix ?? []" empty-text="当日暂无收款">
            <el-table-column label="支付方式">
              <template #default="{ row }">{{ tenderLabel(row.tenderType) }}</template>
            </el-table-column>
            <el-table-column label="收款金额" align="right">
              <template #default="{ row }">{{ money(row.amount) }}</template>
            </el-table-column>
          </el-table>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-table :data="reconciliation?.rows ?? []" empty-text="当日暂无对账数据">
            <el-table-column label="支付方式">
              <template #default="{ row }">{{ tenderLabel(row.tenderType) }}</template>
            </el-table-column>
            <el-table-column label="应到账" align="right">
              <template #default="{ row }">{{ money(row.expectedAmount) }}</template>
            </el-table-column>
            <el-table-column label="差异" align="right" width="110">
              <template #default="{ row }">{{ money(row.variance) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag type="success">{{ row.status === 'MATCHED' ? '已核对' : row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-col>
      </el-row>
    </div>

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2>班次与现金小结</h2>
          <p class="muted">应有现金 = 备用金 + 现金销售 - 现金退款 + 现金收入 - 现金支出。</p>
        </div>
      </div>
      <el-table :data="report?.shifts ?? []" empty-text="当日暂无班次">
        <el-table-column prop="shift.cashierName" label="收银员" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.shift.status === 'OPEN' ? 'success' : 'info'">
              {{ posShiftStatusLabel(row.shift.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备用金" width="110">
          <template #default="{ row }">{{ money(row.shift.openingFloat) }}</template>
        </el-table-column>
        <el-table-column label="现金销售" width="110">
          <template #default="{ row }">{{ money(row.cashSales) }}</template>
        </el-table-column>
        <el-table-column label="现金退款" width="110">
          <template #default="{ row }">{{ money(row.cashRefunds) }}</template>
        </el-table-column>
        <el-table-column label="应有现金" width="120">
          <template #default="{ row }">{{ money(row.expectedCash) }}</template>
        </el-table-column>
        <el-table-column label="实盘" width="110">
          <template #default="{ row }">{{ money(row.shift.actualCash) }}</template>
        </el-table-column>
        <el-table-column label="差异" width="110">
          <template #default="{ row }">{{ money(row.shift.variance) }}</template>
        </el-table-column>
        <el-table-column prop="shift.varianceReason" label="差异原因" min-width="160" />
        <el-table-column label="销售笔数" width="100" prop="saleCount" />
      </el-table>
    </div>

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2>当日销售单</h2>
          <p class="muted">包含组合支付、积分、优惠券与退款状态。</p>
        </div>
      </div>
      <el-table :data="sales" empty-text="当日暂无销售">
        <el-table-column prop="sale.saleNo" label="销售单号" min-width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.sale.status === 'COMPLETED' ? 'success' : row.sale.status === 'REFUNDED' || row.sale.status === 'PARTIALLY_REFUNDED' ? 'warning' : 'info'">
              {{ posSaleStatusLabel(row.sale.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sale.operatorName" label="收银员" width="120" />
        <el-table-column label="应收" width="110">
          <template #default="{ row }">{{ money(row.sale.payable) }}</template>
        </el-table-column>
        <el-table-column label="已收" width="110">
          <template #default="{ row }">{{ money(row.paidAmount) }}</template>
        </el-table-column>
        <el-table-column label="积分抵扣" width="110">
          <template #default="{ row }">{{ money(row.sale.pointsDiscount) }}</template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ new Date(row.sale.createdAt).toLocaleString('zh-CN') }}</template>
        </el-table-column>
        <el-table-column label="支付明细" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="tender in row.sale.tenders" :key="tender.id" size="small" class="tender-tag">
              {{ tenderLabel(tender.type) }} {{ money(tender.amount) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.tender-tag {
  margin: 2px 4px 2px 0;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>