<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ProductPage } from '@fullmall/api-client'
import { api } from '../api'

interface Location {
  id: string
  name: string
  type: string
}

interface ReceiptLine {
  id: string
  skuId: string
  skuCode: string
  title: string
  batchNo: string
  productionDate?: string
  expiryDate?: string
  quantity: number
  unitCost?: { amount: number; currency: string }
}

interface Receipt {
  id: string
  receiptNo: string
  locationType: string
  locationId: string
  status: string
  note?: string
  createdAt: string
  confirmedAt?: string
  lines: ReceiptLine[]
}

interface StocktakeLine {
  id: string
  batchId: string
  skuId: string
  skuCode: string
  title: string
  batchNo: string
  expiryDate?: string
  systemQuantity: number
  countedQuantity?: number
  variance?: number
  reason?: string
}

interface Stocktake {
  id: string
  stocktakeNo: string
  locationType: string
  locationId: string
  status: string
  note?: string
  createdAt: string
  confirmedAt?: string
  lines: StocktakeLine[]
}

interface Transfer {
  id: string
  transferNo: string
  fromType: string
  fromId: string
  toType: string
  toId: string
  status: string
  note?: string
  createdAt: string
  lines: Array<{ id: string; skuId: string; skuCode: string; title: string; quantity: number }>
}

interface Suggestion {
  skuId: string
  skuCode: string
  title: string
  availableQuantity: number
  targetQuantity: number
  suggestedQuantity: number
  nearestExpiry?: string
}

const activeTab = ref('receipts')
const loading = ref(false)
const error = ref('')
const locations = ref<Location[]>([])
const products = ref<ProductPage | null>(null)
const receipts = ref<Receipt[]>([])
const stocktakes = ref<Stocktake[]>([])
const transfers = ref<Transfer[]>([])
const currentStocktake = ref<Stocktake | null>(null)
const counts = reactive<Record<string, number>>({})
const reasons = reactive<Record<string, string>>({})
const suggestions = ref<Suggestion[]>([])
const replenishmentLocation = ref('')

const receiptForm = reactive({
  locationId: '',
  skuId: '',
  batchNo: `RECV-${Date.now().toString().slice(-8)}`,
  quantity: 10,
  expiryDate: '',
  unitCost: 12
})
const stocktakeForm = reactive({ locationId: '', note: '门店例行盘点' })
const transferForm = reactive({
  fromId: '',
  toId: '',
  skuId: '',
  quantity: 1,
  note: '门店调拨'
})

const skuOptions = computed(() => (products.value?.items ?? []).flatMap(product =>
  product.skus.map(sku => ({
    value: sku.id,
    label: `${product.title} · ${sku.skuCode}`
  }))
))

const locationName = (type: string, id: string) => {
  const location = locations.value.find(item => item.id === id)
  return location ? location.name : `${type}/${id.slice(0, 8)}`
}

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  error.value = ''
  try {
    const [locationList, productPage, receiptList, stocktakeList, transferList] = await Promise.all([
      api.get<Location[]>('/admin/v1/warehouse/locations'),
      api.get<ProductPage>('/api/v1/products', { locale: 'zh-CN', size: 100 }),
      api.get<Receipt[]>('/admin/v1/warehouse/receipts'),
      api.get<Stocktake[]>('/admin/v1/warehouse/stocktakes'),
      api.get<Transfer[]>('/admin/v1/warehouse/transfers')
    ])
    locations.value = locationList
    products.value = productPage
    receipts.value = receiptList
    stocktakes.value = stocktakeList
    transfers.value = transferList
    if (!receiptForm.locationId) receiptForm.locationId = locationList[0]?.id ?? ''
    if (!stocktakeForm.locationId) stocktakeForm.locationId = locationList[0]?.id ?? ''
    if (!transferForm.fromId) transferForm.fromId = locationList[0]?.id ?? ''
    if (!transferForm.toId) transferForm.toId = locationList[1]?.id ?? ''
    if (!replenishmentLocation.value) replenishmentLocation.value = locationList[0]?.id ?? ''
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '仓储数据加载失败'
  } finally {
    loading.value = false
  }
}

async function receiveStock() {
  const location = locations.value.find(item => item.id === receiptForm.locationId)
  if (!location || !receiptForm.skuId || !receiptForm.batchNo.trim()) {
    ElMessage.warning('请选择地点、商品并填写批次号')
    return
  }
  if (receiptForm.quantity <= 0) {
    ElMessage.warning('收货数量必须大于 0')
    return
  }
  loading.value = true
  try {
    const receipt = await api.post<Receipt>('/admin/v1/warehouse/receipts', {
      locationType: location.type,
      locationId: location.id,
      note: '后台收货入库',
      lines: [{
        skuId: receiptForm.skuId,
        batchNo: receiptForm.batchNo.trim(),
        productionDate: new Date().toISOString().slice(0, 10),
        expiryDate: receiptForm.expiryDate || undefined,
        quantity: Math.trunc(receiptForm.quantity),
        unitCostAmount: Math.round(Number(receiptForm.unitCost) * 100),
        currency: 'CNY'
      }]
    })
    await api.post(`/admin/v1/warehouse/receipts/${receipt.id}/confirm`)
    ElMessage.success(`收货入库完成：${receipt.receiptNo}`)
    receiptForm.batchNo = `RECV-${Date.now().toString().slice(-8)}`
    await loadAll()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '收货失败'
  } finally {
    loading.value = false
  }
}

async function startStocktake() {
  const location = locations.value.find(item => item.id === stocktakeForm.locationId)
  if (!location) {
    ElMessage.warning('请选择盘点地点')
    return
  }
  loading.value = true
  try {
    currentStocktake.value = await api.post<Stocktake>('/admin/v1/warehouse/stocktakes', {
      locationType: location.type,
      locationId: location.id,
      note: stocktakeForm.note
    })
    Object.keys(counts).forEach(key => delete counts[key])
    Object.keys(reasons).forEach(key => delete reasons[key])
    for (const line of currentStocktake.value.lines) {
      counts[line.id] = line.systemQuantity
    }
    ElMessage.success(`盘点单 ${currentStocktake.value.stocktakeNo} 已生成`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '盘点创建失败'
  } finally {
    loading.value = false
  }
}

async function confirmStocktake() {
  if (!currentStocktake.value) return
  loading.value = true
  try {
    const result = await api.post<Stocktake>(
      `/admin/v1/warehouse/stocktakes/${currentStocktake.value.id}/confirm`,
      {
        lines: currentStocktake.value.lines.map(line => ({
          lineId: line.id,
          countedQuantity: Math.max(0, Math.trunc(counts[line.id] ?? line.systemQuantity)),
          reason: reasons[line.id] || undefined
        }))
      }
    )
    currentStocktake.value = result
    ElMessage.success(`盘点完成：${result.stocktakeNo}`)
    await loadAll()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '盘点确认失败'
  } finally {
    loading.value = false
  }
}

async function createTransfer() {
  if (!transferForm.fromId || !transferForm.toId || transferForm.fromId === transferForm.toId) {
    ElMessage.warning('调出与调入门店/仓库不能相同')
    return
  }
  if (!transferForm.skuId || transferForm.quantity <= 0) {
    ElMessage.warning('请选择商品并填写数量')
    return
  }
  const from = locations.value.find(item => item.id === transferForm.fromId)
  const to = locations.value.find(item => item.id === transferForm.toId)
  if (!from || !to) return
  loading.value = true
  try {
    const transfer = await api.post<Transfer>('/admin/v1/warehouse/transfers', {
      fromType: from.type,
      fromId: from.id,
      toType: to.type,
      toId: to.id,
      note: transferForm.note,
      lines: [{ skuId: transferForm.skuId, quantity: Math.trunc(transferForm.quantity) }]
    })
    await api.post(`/admin/v1/warehouse/transfers/${transfer.id}/ship`)
    ElMessage.success(`调拨单 ${transfer.transferNo} 已出库，等待收货`)
    await loadAll()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '调拨失败'
  } finally {
    loading.value = false
  }
}

async function receiveTransfer(transfer: Transfer) {
  loading.value = true
  try {
    await api.post(`/admin/v1/warehouse/transfers/${transfer.id}/receive`)
    ElMessage.success(`调拨单 ${transfer.transferNo} 已收货`)
    await loadAll()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '调拨收货失败'
  } finally {
    loading.value = false
  }
}

async function loadReplenishment() {
  const location = locations.value.find(item => item.id === replenishmentLocation.value)
  if (!location) return
  loading.value = true
  try {
    suggestions.value = await api.get<Suggestion[]>('/admin/v1/warehouse/replenishment', {
      locationType: location.type,
      locationId: location.id
    })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '补货建议加载失败'
  } finally {
    loading.value = false
  }
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    CONFIRMED: '已确认',
    IN_TRANSIT: '在途',
    RECEIVED: '已收货',
    CANCELLED: '已取消'
  }
  return labels[status] ?? status
}
</script>

<template>
  <div v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <div class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="收货入库" name="receipts">
          <el-form label-position="top" class="inline-form">
            <el-form-item label="收货地点">
              <el-select v-model="receiptForm.locationId" style="width: 220px">
                <el-option
                  v-for="location in locations"
                  :key="location.id"
                  :label="`${location.name}（${location.type === 'STORE' ? '门店' : '仓库'}）`"
                  :value="location.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="商品">
              <el-select v-model="receiptForm.skuId" filterable style="width: 280px">
                <el-option v-for="sku in skuOptions" :key="sku.value" :label="sku.label" :value="sku.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="批次号">
              <el-input v-model="receiptForm.batchNo" style="width: 190px" />
            </el-form-item>
            <el-form-item label="数量">
              <el-input-number v-model="receiptForm.quantity" :min="1" />
            </el-form-item>
            <el-form-item label="到期日">
              <el-date-picker v-model="receiptForm.expiryDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="成本单价（元）">
              <el-input-number v-model="receiptForm.unitCost" :min="0" :precision="2" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="receiveStock">收货入库</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="receipts" empty-text="暂无收货单">
            <el-table-column prop="receiptNo" label="收货单号" min-width="180" />
            <el-table-column label="地点" min-width="150">
              <template #default="{ row }">{{ locationName(row.locationType, row.locationId) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'CONFIRMED' ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="明细" min-width="300">
              <template #default="{ row }">
                <div v-for="line in row.lines" :key="line.id" class="line-note">
                  {{ line.title }} · 批次 {{ line.batchNo }} · × {{ line.quantity }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="收货时间" width="180">
              <template #default="{ row }">{{ new Date(row.confirmedAt || row.createdAt).toLocaleString('zh-CN') }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="盘点" name="stocktakes">
          <div class="toolbar-row">
            <el-select v-model="stocktakeForm.locationId" style="width: 240px">
              <el-option
                v-for="location in locations"
                :key="location.id"
                :label="location.name"
                :value="location.id"
              />
            </el-select>
            <el-input v-model="stocktakeForm.note" placeholder="盘点说明" style="width: 240px" />
            <el-button type="primary" @click="startStocktake">生成盘点单</el-button>
          </div>

          <div v-if="currentStocktake" class="stocktake-box">
            <div class="stocktake-head">
              <div>
                <strong>{{ currentStocktake.stocktakeNo }}</strong>
                <span class="muted"> · {{ locationName(currentStocktake.locationType, currentStocktake.locationId) }}</span>
              </div>
              <el-button
                type="success"
                :disabled="currentStocktake.status !== 'DRAFT'"
                @click="confirmStocktake"
              >
                确认盘点
              </el-button>
            </div>
            <el-table :data="currentStocktake.lines">
              <el-table-column prop="title" label="商品" min-width="150" />
              <el-table-column prop="batchNo" label="批次" min-width="150" />
              <el-table-column prop="systemQuantity" label="系统数量" width="100" />
              <el-table-column label="实盘数量" width="150">
                <template #default="{ row }">
                  <el-input-number
                    v-model="counts[row.id]"
                    :min="0"
                    :disabled="currentStocktake?.status !== 'DRAFT'"
                    size="small"
                  />
                </template>
              </el-table-column>
              <el-table-column label="差异原因" min-width="180">
                <template #default="{ row }">
                  <el-input
                    v-model="reasons[row.id]"
                    :disabled="currentStocktake?.status !== 'DRAFT'"
                    placeholder="有差异时填写"
                  />
                </template>
              </el-table-column>
            </el-table>
          </div>

          <el-table :data="stocktakes" empty-text="暂无盘点单" class="mt-16">
            <el-table-column prop="stocktakeNo" label="盘点单号" min-width="180" />
            <el-table-column label="地点" min-width="150">
              <template #default="{ row }">{{ locationName(row.locationType, row.locationId) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'CONFIRMED' ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lines.length" label="批次数" width="100" />
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">{{ new Date(row.createdAt).toLocaleString('zh-CN') }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="调拨" name="transfers">
          <el-form label-position="top" class="inline-form">
            <el-form-item label="调出">
              <el-select v-model="transferForm.fromId" style="width: 210px">
                <el-option v-for="location in locations" :key="location.id" :label="location.name" :value="location.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="调入">
              <el-select v-model="transferForm.toId" style="width: 210px">
                <el-option v-for="location in locations" :key="location.id" :label="location.name" :value="location.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="商品">
              <el-select v-model="transferForm.skuId" filterable style="width: 280px">
                <el-option v-for="sku in skuOptions" :key="sku.value" :label="sku.label" :value="sku.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="数量">
              <el-input-number v-model="transferForm.quantity" :min="1" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="createTransfer">创建并出库</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="transfers" empty-text="暂无调拨单">
            <el-table-column prop="transferNo" label="调拨单号" min-width="180" />
            <el-table-column label="调出 → 调入" min-width="220">
              <template #default="{ row }">
                {{ locationName(row.fromType, row.fromId) }} → {{ locationName(row.toType, row.toId) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'RECEIVED' ? 'success' : row.status === 'IN_TRANSIT' ? 'warning' : 'info'">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="商品" min-width="180">
              <template #default="{ row }">
                <div v-for="line in row.lines" :key="line.id" class="line-note">
                  {{ line.title }} × {{ line.quantity }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'IN_TRANSIT'"
                  size="small"
                  type="primary"
                  @click="receiveTransfer(row)"
                >
                  确认收货
                </el-button>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="补货建议" name="replenishment">
          <div class="toolbar-row">
            <el-select v-model="replenishmentLocation" style="width: 240px">
              <el-option v-for="location in locations" :key="location.id" :label="location.name" :value="location.id" />
            </el-select>
            <el-button type="primary" @click="loadReplenishment">生成建议</el-button>
          </div>
          <el-table :data="suggestions" empty-text="当前地点库存充足">
            <el-table-column prop="title" label="商品" min-width="180" />
            <el-table-column prop="skuCode" label="SKU" width="150" />
            <el-table-column prop="availableQuantity" label="可用库存" width="110" />
            <el-table-column prop="targetQuantity" label="目标库存" width="110" />
            <el-table-column prop="suggestedQuantity" label="建议补货" width="120">
              <template #default="{ row }">
                <el-tag type="warning">{{ row.suggestedQuantity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最近效期" width="130">
              <template #default="{ row }">{{ row.nearestExpiry || '—' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style scoped>
.inline-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}

.inline-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.toolbar-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.line-note {
  font-size: 12px;
  color: #5c574e;
}

.stocktake-box {
  padding: 16px;
  border: 1px solid #e3dfd6;
  border-radius: 12px;
  background: #fbfaf7;
}

.stocktake-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.mt-16 {
  margin-top: 16px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>