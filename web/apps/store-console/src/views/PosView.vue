<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import {
  posSaleStatusLabel,
  posShiftStatusLabel,
  posTenderStatusLabel,
  posTenderTypeLabel,
  type PosMemberView,
  type PosProductView,
  type PosProviderView,
  type PosSale,
  type PosSaleLine,
  type PosSaleView,
  type PosShiftView,
  type Store
} from '@fullmall/api-client'

interface CartLine {
  skuId: string
  title: string
  skuCode: string
  price: number
  quantity: number
  availableQuantity: number
}

const appMode = import.meta.env.VITE_APP_MODE || 'dev'
const isDemo = appMode === 'demo'

const stores = ref<Store[]>([])
const storeId = ref('')
const cashierName = ref(localStorage.getItem('fullmall.pos.cashier') || '演示收银员')
const openingFloat = ref(200)
const shiftView = ref<PosShiftView | null>(null)
const shiftLoading = ref(false)
const closeDialog = ref(false)
const closeForm = reactive({ actualCash: 0, reason: '' })
const cashDialog = ref(false)
const cashForm = reactive({ amount: 0, cashTendered: 0 })
const cashMovementDialog = ref(false)
const cashMovementForm = reactive({ type: 'CASH_IN', amount: 0, reason: '' })

const keyword = ref('')
const products = ref<PosProductView[]>([])
const productLoading = ref(false)
const cart = ref<CartLine[]>([])
const saleView = ref<PosSaleView | null>(null)
const shiftSales = ref<PosSaleView[]>([])
const providers = ref<PosProviderView[]>([])

const memberQuery = ref('')
const memberView = ref<PosMemberView | null>(null)
const selectedCoupon = ref('')
const pointsInput = ref(0)
const tenderAmountYuan = ref(0)

const busy = ref(false)
const refundDialog = ref(false)
const refundSale = ref<PosSale | null>(null)
const refundReason = ref('')
const refundQuantities = reactive<Record<string, number>>({})
const managerUsername = ref('admin')
const managerPin = ref('')
const locked = ref(false)
const lockForm = reactive({ username: 'admin', pin: '' })
const lockError = ref('')
const error = ref('')
const success = ref('')

const isDraft = computed(() => saleView.value?.sale.status === 'DRAFT')
const remaining = computed(() => saleView.value?.remainingAmount.amount ?? 0)
const payable = computed(() => saleView.value?.sale.payable.amount ?? 0)
const subtotal = computed(() => saleView.value?.sale.subtotal.amount ?? 0)
const canOpenShift = computed(() => !!storeId.value && !shiftView.value && !isDemo)
const cartTotal = computed(() => cart.value.reduce((sum, line) => sum + line.price * line.quantity, 0))

onMounted(async () => {
  await loadStores()
  await loadProviders()
  await loadProducts()
  if (isDemo) {
    await loadDemo()
  } else {
    await loadOpenShift()
  }
})

async function loadStores() {
  try {
    stores.value = await api.get<Store[]>('/store/v1/my-stores')
    storeId.value = stores.value[0]?.id ?? ''
  } catch (cause) {
    error.value = messageOf(cause, '门店加载失败')
  }
}

async function loadProviders() {
  try {
    providers.value = await api.get<PosProviderView[]>('/store/v1/pos/providers')
  } catch (cause) {
    error.value = messageOf(cause, '收款方式加载失败')
  }
}

async function loadProducts() {
  if (!storeId.value) return
  productLoading.value = true
  try {
    products.value = await api.get<PosProductView[]>('/store/v1/pos/products', {
      storeId: storeId.value,
      keyword: keyword.value
    })
  } catch (cause) {
    error.value = messageOf(cause, '商品加载失败')
  } finally {
    productLoading.value = false
  }
}

async function loadOpenShift() {
  if (!storeId.value || !cashierName.value.trim()) {
    shiftView.value = null
    return
  }
  shiftLoading.value = true
  try {
    const view = await api.get<PosShiftView | undefined>('/store/v1/pos/shifts/open', {
      storeId: storeId.value,
      cashierName: cashierName.value.trim()
    })
    shiftView.value = view ?? null
    if (shiftView.value) {
      await loadShiftSales()
    }
  } catch (cause) {
    error.value = messageOf(cause, '班次加载失败')
  } finally {
    shiftLoading.value = false
  }
}

async function loadDemo() {
  try {
    shiftView.value = await api.get<PosShiftView>('/mock/pos/shift')
    saleView.value = await api.get<PosSaleView>('/mock/pos/sale')
    shiftSales.value = [saleView.value]
    memberView.value = {
      member: saleView.value.member!,
      coupons: []
    }
  } catch (cause) {
    error.value = messageOf(cause, '演示收银数据加载失败')
  }
}

async function loadShiftSales() {
  if (!shiftView.value) return
  try {
    shiftSales.value = await api.get<PosSaleView[]>(`/store/v1/pos/shifts/${shiftView.value.shift.id}/sales`)
  } catch (cause) {
    error.value = messageOf(cause, '班次销售记录加载失败')
  }
}

async function onStoreChange() {
  saleView.value = null
  memberView.value = null
  cart.value = []
  shiftSales.value = []
  await loadProducts()
  if (isDemo) {
    await loadDemo()
  } else {
    await loadOpenShift()
  }
}

function persistCashier() {
  localStorage.setItem('fullmall.pos.cashier', cashierName.value.trim())
}

async function openShift() {
  if (!storeId.value) {
    ElMessage.warning('请先选择门店')
    return
  }
  if (!cashierName.value.trim()) {
    ElMessage.warning('请输入收银员姓名')
    return
  }
  busy.value = true
  clearFeedback()
  try {
    persistCashier()
    shiftView.value = await api.post<PosShiftView>('/store/v1/pos/shifts', {
      storeId: storeId.value,
      cashierName: cashierName.value.trim(),
      openingFloat: yuanToFen(openingFloat.value),
      currency: 'CNY'
    })
    success.value = `班次已开启，备用金 ${money(shiftView.value.shift.openingFloat)}`
    shiftSales.value = []
  } catch (cause) {
    error.value = messageOf(cause, '开班失败')
  } finally {
    busy.value = false
  }
}

function openCloseDialog() {
  if (!shiftView.value) return
  closeForm.actualCash = shiftView.value.expectedCash.amount / 100
  closeForm.reason = ''
  closeDialog.value = true
}

async function submitCloseShift() {
  if (!shiftView.value) return
  busy.value = true
  clearFeedback()
  try {
    const closed = await api.post<PosShiftView>(
      `/store/v1/pos/shifts/${shiftView.value.shift.id}/close`,
      { actualCash: yuanToFen(closeForm.actualCash), reason: closeForm.reason.trim() || undefined }
    )
    shiftView.value = closed
    closeDialog.value = false
    success.value = `关班完成，现金差异 ${money(closed.shift.variance)}`
  } catch (cause) {
    error.value = messageOf(cause, '关班失败')
  } finally {
    busy.value = false
  }
}

async function submitCashMovement() {
  if (!shiftView.value) return
  busy.value = true
  clearFeedback()
  try {
    shiftView.value = await api.post<PosShiftView>(
      `/store/v1/pos/shifts/${shiftView.value.shift.id}/cash-movements`,
      {
        type: cashMovementForm.type,
        amount: yuanToFen(cashMovementForm.amount),
        reason: cashMovementForm.reason,
        operatorName: cashierName.value.trim()
      }
    )
    cashMovementDialog.value = false
    success.value = cashMovementForm.type === 'CASH_IN' ? '现金收入已登记' : '现金支出已登记'
  } catch (cause) {
    error.value = messageOf(cause, '现金登记失败')
  } finally {
    busy.value = false
  }
}

function addToCart(product: PosProductView) {
  if (saleView.value) {
    ElMessage.warning('当前已有销售单，请先完成或作废后再开新单')
    return
  }
  if (product.availableQuantity <= 0) {
    ElMessage.warning('该商品在门店无可售库存')
    return
  }
  const existing = cart.value.find(line => line.skuId === product.skuId)
  if (existing) {
    if (existing.quantity >= product.availableQuantity) {
      ElMessage.warning('已达到门店可售库存上限')
      return
    }
    existing.quantity += 1
    return
  }
  cart.value.push({
    skuId: product.skuId,
    title: product.title,
    skuCode: product.skuCode,
    price: product.price.amount,
    quantity: 1,
    availableQuantity: product.availableQuantity
  })
}

function changeQuantity(line: CartLine, delta: number) {
  const next = line.quantity + delta
  if (next <= 0) {
    cart.value = cart.value.filter(item => item.skuId !== line.skuId)
    return
  }
  if (next > line.availableQuantity) {
    ElMessage.warning('已达到门店可售库存上限')
    return
  }
  line.quantity = next
}

async function createSale() {
  if (!shiftView.value) {
    ElMessage.warning('请先开班')
    return
  }
  if (!cart.value.length) {
    ElMessage.warning('请先选择商品')
    return
  }
  busy.value = true
  clearFeedback()
  try {
    saleView.value = await api.post<PosSaleView>(
      '/store/v1/pos/sales',
      {
        storeId: storeId.value,
        shiftId: shiftView.value.shift.id,
        cashierName: cashierName.value.trim(),
        items: cart.value.map(line => ({ skuId: line.skuId, quantity: line.quantity }))
      },
      { 'Idempotency-Key': crypto.randomUUID() }
    )
    cart.value = []
    resetBenefits()
    syncTenderAmount()
    success.value = `销售单 ${saleView.value.sale.saleNo} 已创建`
  } catch (cause) {
    error.value = messageOf(cause, '开单失败')
  } finally {
    busy.value = false
  }
}

async function lookupMember() {
  if (!memberQuery.value.trim()) {
    ElMessage.warning('请输入会员手机号或会员编号')
    return
  }
  clearFeedback()
  try {
    memberView.value = await api.get<PosMemberView>('/store/v1/pos/members', {
      query: memberQuery.value.trim()
    })
    selectedCoupon.value = ''
    success.value = `已识别会员 ${memberView.value.member.name}`
    if (saleView.value && isDraft.value) {
      saleView.value = await api.post<PosSaleView>(
        `/store/v1/pos/sales/${saleView.value.sale.id}/member`,
        { memberId: memberView.value.member.id }
      )
    }
  } catch (cause) {
    memberView.value = null
    error.value = messageOf(cause, '会员识别失败')
  }
}

async function applyCoupon() {
  if (!saleView.value || !selectedCoupon.value) {
    ElMessage.warning('请选择优惠券')
    return
  }
  busy.value = true
  clearFeedback()
  try {
    saleView.value = await api.post<PosSaleView>(
      `/store/v1/pos/sales/${saleView.value.sale.id}/coupon`,
      { code: selectedCoupon.value }
    )
    syncTenderAmount()
    success.value = `优惠券已核销，优惠 ${money(saleView.value.sale.couponDiscount)}`
  } catch (cause) {
    error.value = messageOf(cause, '优惠券核销失败')
  } finally {
    busy.value = false
  }
}

async function redeemPoints() {
  if (!saleView.value) return
  if (pointsInput.value <= 0) {
    ElMessage.warning('请输入要抵扣的积分')
    return
  }
  busy.value = true
  clearFeedback()
  try {
    saleView.value = await api.post<PosSaleView>(
      `/store/v1/pos/sales/${saleView.value.sale.id}/points`,
      { points: Math.floor(pointsInput.value) }
    )
    syncTenderAmount()
    success.value = `积分抵扣 ${money(saleView.value.sale.pointsDiscount)}`
  } catch (cause) {
    error.value = messageOf(cause, '积分抵扣失败')
  } finally {
    busy.value = false
  }
}

function syncTenderAmount() {
  tenderAmountYuan.value = remaining.value / 100
}

function startCashTender() {
  const amount = Math.round(tenderAmountYuan.value * 100)
  if (amount <= 0 || amount > remaining.value) {
    ElMessage.warning('本次收款金额必须大于 0 且不超过待收金额')
    return
  }
  cashForm.amount = amount / 100
  cashForm.cashTendered = amount / 100
  cashDialog.value = true
}

async function submitTender(type: string) {
  if (!saleView.value) return
  let amount = Math.round(tenderAmountYuan.value * 100)
  if (type === 'CASH') {
    amount = yuanToFen(cashForm.amount)
  }
  if (amount <= 0 || amount > remaining.value) {
    ElMessage.warning('本次收款金额必须大于 0 且不超过待收金额')
    return
  }
  busy.value = true
  clearFeedback()
  try {
    const body: Record<string, unknown> = {
      type,
      amount,
      idempotencyKey: crypto.randomUUID(),
      operatorName: cashierName.value.trim()
    }
    if (type === 'CASH') {
      body.cashTendered = yuanToFen(cashForm.cashTendered)
    }
    saleView.value = await api.post<PosSaleView>(
      `/store/v1/pos/sales/${saleView.value.sale.id}/tenders`,
      body
    )
    cashDialog.value = false
    syncTenderAmount()
    success.value = `${posTenderTypeLabel(type)}收款成功`
  } catch (cause) {
    error.value = messageOf(cause, '收款失败')
  } finally {
    busy.value = false
  }
}

async function completeSale() {
  if (!saleView.value) return
  busy.value = true
  clearFeedback()
  try {
    saleView.value = await api.post<PosSaleView>(`/store/v1/pos/sales/${saleView.value.sale.id}/complete`)
    success.value = `销售完成，找零 ${money(saleView.value.sale.change)}`
    await loadShiftSales()
    await loadProducts()
  } catch (cause) {
    error.value = messageOf(cause, '完成销售失败')
  } finally {
    busy.value = false
  }
}

async function voidSale() {
  if (!saleView.value) return
  try {
    await ElMessageBox.confirm('作废后将释放库存并撤销已收款项，是否继续？', '作废销售单', {
      confirmButtonText: '确认作废',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  busy.value = true
  clearFeedback()
  try {
    saleView.value = await api.post<PosSaleView>(`/store/v1/pos/sales/${saleView.value.sale.id}/void`)
    success.value = '销售单已作废'
    await loadShiftSales()
    await loadProducts()
  } catch (cause) {
    error.value = messageOf(cause, '作废失败')
  } finally {
    busy.value = false
  }
}

async function unlock() {
  lockError.value = ''
  try {
    await api.post('/store/v1/pos/auth/manager', {
      username: lockForm.username.trim(),
      pin: lockForm.pin
    })
    locked.value = false
    lockForm.pin = ''
  } catch (cause) {
    lockError.value = messageOf(cause, '解锁失败')
  }
}

function openRefundDialog(sale: PosSale) {
  if (isDemo) {
    ElMessage.warning('演示模式只读，不能发起退款')
    return
  }
  refundSale.value = sale
  refundReason.value = ''
  Object.keys(refundQuantities).forEach(key => delete refundQuantities[key])
  for (const line of sale.lines) {
    refundQuantities[line.id] = 0
  }
  refundDialog.value = true
}

function remainingQuantity(line: PosSaleLine) {
  return Math.max(line.quantity - line.refundedQuantity, 0)
}

function selectAllRefundLines() {
  if (!refundSale.value) return
  for (const line of refundSale.value.lines) {
    refundQuantities[line.id] = remainingQuantity(line)
  }
}

function estimatedLineRefund(line: PosSaleLine) {
  const sale = refundSale.value
  const quantity = refundQuantities[line.id] ?? 0
  if (!sale || quantity <= 0) return 0
  const subtotal = sale.subtotal.amount
  const payable = sale.payable.amount
  const lineNet = subtotal === 0 ? 0 : Math.round((line.lineTotal.amount * payable) / subtotal)
  const newQuantity = line.refundedQuantity + quantity
  const target = newQuantity >= line.quantity
    ? lineNet
    : Math.round((lineNet * newQuantity) / line.quantity)
  return Math.max(target - line.refundedAmount.amount, 0)
}

function estimatedRefundTotal() {
  if (!refundSale.value) return 0
  return refundSale.value.lines.reduce((sum, line) => sum + estimatedLineRefund(line), 0)
}

async function submitPartialRefund() {
  if (!refundSale.value) return
  const items = Object.entries(refundQuantities)
    .filter(([, quantity]) => quantity > 0)
    .map(([saleLineId, quantity]) => ({ saleLineId, quantity }))
  if (!items.length) {
    ElMessage.warning('请选择至少一个退款商品和数量')
    return
  }
  if (!refundReason.value.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  busy.value = true
  clearFeedback()
  try {
    const updated = await api.post<PosSaleView>(
      `/store/v1/pos/sales/${refundSale.value.id}/refund-lines`,
      {
        items,
        reason: refundReason.value.trim(),
        operatorName: cashierName.value.trim(),
        managerUsername: managerUsername.value.trim(),
        managerPin: managerPin.value
      }
    )
    if (saleView.value?.sale.id === updated.sale.id) {
      saleView.value = updated
    }
    success.value = updated.sale.status === 'REFUNDED'
      ? `销售单 ${updated.sale.saleNo} 已全部退款`
      : `销售单 ${updated.sale.saleNo} 已部分退款 ${money(updated.sale.refunded)}`
    refundDialog.value = false
    await loadShiftSales()
    await loadProducts()
  } catch (cause) {
    error.value = messageOf(cause, '退款失败')
  } finally {
    busy.value = false
  }
}

function startNewSale() {
  saleView.value = null
  memberView.value = null
  memberQuery.value = ''
  resetBenefits()
  cart.value = []
  syncTenderAmount()
}

function resetBenefits() {
  selectedCoupon.value = ''
  pointsInput.value = 0
}

function clearFeedback() {
  error.value = ''
  success.value = ''
}

function money(value?: { amount: number; currency: string }) {
  if (!value) return '-'
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: value.currency
  }).format(value.amount / 100)
}

function yuanToFen(value: number) {
  return Math.round(Number(value || 0) * 100)
}

function messageOf(cause: unknown, fallback: string) {
  return cause instanceof Error ? cause.message : fallback
}
</script>

<template>
  <div class="pos">
    <el-alert
      v-if="isDemo"
      type="info"
      show-icon
      :closable="false"
      title="演示模式只读"
      description="公开演示站只允许 GET 与 Mock 数据。以下为示例班次和销售单，写操作按钮已禁用。"
      class="mb-16"
    />

    <div class="pos-toolbar">
      <div class="toolbar-fields">
        <el-select v-model="storeId" placeholder="选择门店" style="width: 220px" @change="onStoreChange">
          <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
        </el-select>
        <el-input v-model="cashierName" placeholder="收银员姓名" style="width: 160px" :disabled="!!shiftView && !isDemo" />
        <el-input-number v-model="openingFloat" :min="0" :precision="2" :step="50" :disabled="!!shiftView || isDemo" />
        <el-button type="primary" :disabled="!canOpenShift" :loading="busy" @click="openShift">开班</el-button>
        <el-button v-if="shiftView && shiftView.shift.status === 'OPEN' && !isDemo" @click="openCloseDialog">关班</el-button>
        <el-button v-if="shiftView && shiftView.shift.status === 'OPEN' && !isDemo" @click="cashMovementDialog = true">
          现金收付
        </el-button>
        <el-button v-if="!isDemo" @click="locked = true">锁屏</el-button>
      </div>
      <div v-if="shiftView" class="shift-summary">
        <el-tag :type="shiftView.shift.status === 'OPEN' ? 'success' : 'info'">
          {{ posShiftStatusLabel(shiftView.shift.status) }}
        </el-tag>
        <span>备用金 {{ money(shiftView.shift.openingFloat) }}</span>
        <span>现金销售 {{ money(shiftView.cashSales) }}</span>
        <span>现金退款 {{ money(shiftView.cashRefunds) }}</span>
        <strong>应有现金 {{ money(shiftView.expectedCash) }}</strong>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-alert v-if="success" :title="success" type="success" show-icon :closable="false" class="mb-16" />

    <div class="pos-grid">
      <section class="panel pos-products">
        <div class="panel-head">
          <div>
            <h2>商品与购物车</h2>
            <p class="muted">扫码枪可直接输入条码/商品编码，或输入名称搜索。点击商品加入本单。</p>
          </div>
          <div class="search-row">
            <el-input
              v-model="keyword"
              placeholder="条码 / 商品编码 / 名称"
              clearable
              style="width: 260px"
              @keyup.enter="loadProducts"
              @clear="loadProducts"
            />
            <el-button @click="loadProducts">搜索</el-button>
          </div>
        </div>

        <div v-loading="productLoading" class="product-strip">
          <button
            v-for="product in products"
            :key="product.skuId"
            type="button"
            class="product-chip"
            :disabled="saleView !== null || product.availableQuantity <= 0"
            @click="addToCart(product)"
          >
            <strong>{{ product.title }}</strong>
            <span class="muted">{{ product.skuCode }} · 门店可售 {{ product.availableQuantity }}</span>
            <span class="price">{{ money(product.price) }}</span>
          </button>
          <el-empty v-if="!products.length" description="没有匹配商品" />
        </div>

        <div v-if="!saleView" class="cart-panel">
          <el-table :data="cart" empty-text="尚未选择商品">
            <el-table-column prop="title" label="商品" min-width="160" />
            <el-table-column prop="skuCode" label="编码" width="130" />
            <el-table-column label="单价" width="110">
              <template #default="{ row }">{{ money({ amount: row.price, currency: 'CNY' }) }}</template>
            </el-table-column>
            <el-table-column label="数量" width="150">
              <template #default="{ row }">
                <el-button size="small" @click="changeQuantity(row, -1)">－</el-button>
                <span class="qty">{{ row.quantity }}</span>
                <el-button size="small" @click="changeQuantity(row, 1)">＋</el-button>
              </template>
            </el-table-column>
            <el-table-column label="小计" width="110">
              <template #default="{ row }">
                {{ money({ amount: row.price * row.quantity, currency: 'CNY' }) }}
              </template>
            </el-table-column>
          </el-table>
          <div class="cart-footer">
            <strong>合计 {{ money({ amount: cartTotal, currency: 'CNY' }) }}</strong>
            <el-button type="primary" size="large" :disabled="!cart.length || !shiftView" :loading="busy" @click="createSale">
              开单
            </el-button>
          </div>
        </div>

        <div v-else class="sale-lines">
          <div class="sale-title">
            <div>
              <strong>{{ saleView.sale.saleNo }}</strong>
              <span class="muted"> · {{ saleView.storeName }} · {{ saleView.sale.operatorName }}</span>
            </div>
            <el-tag :type="saleView.sale.status === 'COMPLETED' ? 'success' : saleView.sale.status === 'DRAFT' ? 'warning' : 'info'">
              {{ posSaleStatusLabel(saleView.sale.status) }}
            </el-tag>
          </div>
          <el-table :data="saleView.sale.lines">
            <el-table-column prop="title" label="商品" min-width="150" />
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column label="单价" width="110">
              <template #default="{ row }">{{ money(row.unitPrice) }}</template>
            </el-table-column>
            <el-table-column label="小计" width="110">
              <template #default="{ row }">{{ money(row.lineTotal) }}</template>
            </el-table-column>
            <el-table-column prop="refundedQuantity" label="已退" width="70" />
            <el-table-column prop="batchSummary" label="出货批次" min-width="160" />
          </el-table>
          <div class="sale-actions">
            <el-button v-if="saleView.sale.status === 'DRAFT' && !isDemo" @click="voidSale">作废本单</el-button>
            <el-button @click="startNewSale">开始新单</el-button>
          </div>
        </div>
      </section>

      <aside class="panel pos-checkout">
        <h2>结算</h2>
        <div class="totals">
          <div><span>商品小计</span><strong>{{ money(saleView?.sale.subtotal ?? { amount: 0, currency: 'CNY' }) }}</strong></div>
          <div><span>优惠券</span><strong>-{{ money(saleView?.sale.couponDiscount ?? { amount: 0, currency: 'CNY' }) }}</strong></div>
          <div><span>积分抵扣</span><strong>-{{ money(saleView?.sale.pointsDiscount ?? { amount: 0, currency: 'CNY' }) }}</strong></div>
          <div class="payable"><span>应收</span><strong>{{ money(saleView?.sale.payable ?? { amount: 0, currency: 'CNY' }) }}</strong></div>
          <div><span>已收</span><strong>{{ money(saleView?.paidAmount ?? { amount: 0, currency: 'CNY' }) }}</strong></div>
          <div><span>待收</span><strong class="remaining">{{ money(saleView?.remainingAmount ?? { amount: 0, currency: 'CNY' }) }}</strong></div>
          <div><span>找零</span><strong>{{ money(saleView?.sale.change ?? { amount: 0, currency: 'CNY' }) }}</strong></div>
        </div>

        <div class="member-box">
          <div class="search-row">
            <el-input
              v-model="memberQuery"
              placeholder="会员手机号 / 会员编号"
              :disabled="!isDraft || isDemo"
              @keyup.enter="lookupMember"
            />
            <el-button :disabled="!isDraft || isDemo" @click="lookupMember">识别会员</el-button>
          </div>
          <div v-if="memberView" class="member-card">
            <div>
              <strong>{{ memberView.member.name }}</strong>
              <span class="muted"> · {{ memberView.member.tier }} · {{ memberView.member.phone }}</span>
            </div>
            <div class="member-metrics">
              <span>积分 {{ memberView.member.points }}</span>
              <span>储值 {{ money(memberView.member.storedValue) }}</span>
            </div>
          </div>
          <el-empty v-else description="未识别会员" :image-size="48" />
        </div>

        <div class="benefit-box">
          <el-select v-model="selectedCoupon" placeholder="选择优惠券" clearable :disabled="!isDraft || isDemo" style="width: 100%">
            <el-option
              v-for="coupon in memberView?.coupons ?? []"
              :key="coupon.id"
              :label="`${coupon.code} · ${coupon.name}`"
              :value="coupon.code"
            />
          </el-select>
          <el-button :disabled="!selectedCoupon || !isDraft || isDemo" @click="applyCoupon">核销优惠券</el-button>
          <div class="points-row">
            <el-input-number
              :key="`points-${isDraft}-${!!memberView}`"
              v-model="pointsInput"
              :min="0"
              :step="100"
              :disabled="!isDraft || !memberView || isDemo"
            />
            <el-button :disabled="!memberView || !isDraft || isDemo" @click="redeemPoints">积分抵扣</el-button>
          </div>
          <small class="muted">1 积分 = 1 分钱；积分与优惠券均在完成销售时正式核销。</small>
        </div>

        <div class="tender-box">
          <div class="tender-amount">
            <span>本次收款（元）</span>
            <el-input-number
              :key="`tender-${isDraft}`"
              v-model="tenderAmountYuan"
              :min="0"
              :precision="2"
              :step="10"
              :disabled="!isDraft || isDemo"
            />
          </div>
          <div class="tender-buttons">
            <el-button
              v-for="provider in providers"
              :key="provider.code"
              :disabled="!isDraft || remaining <= 0 || isDemo || (provider.requiresMember && !memberView)"
              :type="provider.code === 'CASH' ? 'primary' : 'default'"
              @click="provider.code === 'CASH' ? startCashTender() : submitTender(provider.code)"
            >
              {{ provider.label }}
              <el-tag v-if="provider.mock" size="small" type="info" class="mock-tag">Mock</el-tag>
            </el-button>
          </div>
        </div>

        <div v-if="saleView?.sale.tenders.length" class="tender-list">
          <div v-for="tender in saleView.sale.tenders" :key="tender.id" class="tender-row">
            <span>{{ posTenderTypeLabel(tender.type) }}</span>
            <span>{{ money(tender.amount) }}</span>
            <el-tag size="small" :type="tender.status === 'SUCCESS' ? 'success' : 'info'">
              {{ posTenderStatusLabel(tender.status) }}
            </el-tag>
          </div>
        </div>

        <div class="complete-row">
          <el-button
            type="success"
            size="large"
            :disabled="!isDraft || remaining !== 0 || isDemo"
            :loading="busy"
            @click="completeSale"
          >
            完成销售
          </el-button>
          <span v-if="isDraft && remaining > 0" class="muted">还需收款 {{ money({ amount: remaining, currency: 'CNY' }) }}</span>
        </div>
      </aside>
    </div>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>本班次销售记录</h2>
          <p class="muted">退款为整单原路退回，批次库存同步回补；已关班次需走门店财务流程。</p>
        </div>
        <el-button @click="loadShiftSales">刷新</el-button>
      </div>
      <el-table :data="shiftSales" empty-text="本班次暂无销售记录">
        <el-table-column prop="sale.saleNo" label="销售单号" min-width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.sale.status === 'COMPLETED' ? 'success' : row.sale.status === 'REFUNDED' || row.sale.status === 'PARTIALLY_REFUNDED' ? 'warning' : 'info'">
              {{ posSaleStatusLabel(row.sale.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="应收" width="120">
          <template #default="{ row }">{{ money(row.sale.payable) }}</template>
        </el-table-column>
        <el-table-column label="已收" width="120">
          <template #default="{ row }">{{ money(row.paidAmount) }}</template>
        </el-table-column>
        <el-table-column label="找零" width="100">
          <template #default="{ row }">{{ money(row.sale.change) }}</template>
        </el-table-column>
        <el-table-column label="已退款" width="110">
          <template #default="{ row }">{{ money(row.sale.refunded) }}</template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ new Date(row.sale.createdAt).toLocaleString('zh-CN') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button
              v-if="row.sale.status === 'COMPLETED' || row.sale.status === 'PARTIALLY_REFUNDED'"
              size="small"
              type="danger"
              plain
              :disabled="isDemo"
              @click="openRefundDialog(row.sale)"
            >
              退款
            </el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>
    </section>


    <el-dialog v-model="refundDialog" title="按商品退款" width="720px">
      <div v-if="refundSale">
        <div class="refund-head">
          <div>
            <strong>{{ refundSale.saleNo }}</strong>
            <span class="muted"> · 应收 {{ money(refundSale.payable) }} · 已退 {{ money(refundSale.refunded) }}</span>
          </div>
          <el-button size="small" @click="selectAllRefundLines">全部可退</el-button>
        </div>
        <el-table :data="refundSale.lines">
          <el-table-column prop="title" label="商品" min-width="150" />
          <el-table-column prop="quantity" label="购买" width="70" />
          <el-table-column prop="refundedQuantity" label="已退" width="70" />
          <el-table-column label="可退" width="70">
            <template #default="{ row }">{{ remainingQuantity(row) }}</template>
          </el-table-column>
          <el-table-column label="本次退款" width="140">
            <template #default="{ row }">
              <el-input-number
                v-model="refundQuantities[row.id]"
                :min="0"
                :max="remainingQuantity(row)"
                :disabled="remainingQuantity(row) === 0"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="预计金额" width="120">
            <template #default="{ row }">
              {{ money({ amount: estimatedLineRefund(row), currency: refundSale?.currency || 'CNY' }) }}
            </template>
          </el-table-column>
        </el-table>
        <el-form label-position="top" class="mt-12">
          <el-form-item label="店长授权（正式环境退款必填）">
            <div class="manager-row">
              <el-input v-model="managerUsername" placeholder="店长/运营账号" />
              <el-input v-model="managerPin" type="password" show-password placeholder="PIN" />
            </div>
          </el-form-item>
          <el-form-item label="退款原因">
            <el-input v-model="refundReason" type="textarea" :rows="2" placeholder="例如：顾客部分退货、商品破损" />
          </el-form-item>
        </el-form>
        <el-alert
          :title="`预计退款 ${money({ amount: estimatedRefundTotal(), currency: refundSale?.currency || 'CNY' })}`"
          type="warning"
          :closable="false"
        />
      </div>
      <template #footer>
        <el-button @click="refundDialog = false">取消</el-button>
        <el-button type="danger" :loading="busy" @click="submitPartialRefund">确认退款</el-button>
      </template>
    </el-dialog>

    <div v-if="locked" class="lock-overlay">
      <div class="lock-card">
        <h2>收银台已锁屏</h2>
        <p class="muted">请输入店长/运营账号 PIN 解锁。锁屏期间不能开单或收款。</p>
        <el-input v-model="lockForm.username" placeholder="账号" />
        <el-input v-model="lockForm.pin" type="password" show-password placeholder="PIN" @keyup.enter="unlock" />
        <el-alert v-if="lockError" :title="lockError" type="error" show-icon :closable="false" />
        <el-button type="primary" :loading="busy" @click="unlock">解锁</el-button>
      </div>
    </div>

    <el-dialog v-model="cashDialog" title="现金收款" width="420px">
      <el-form label-position="top">
        <el-form-item label="本次计入应收（元）">
          <el-input-number v-model="cashForm.amount" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实收现金（元）">
          <el-input-number v-model="cashForm.cashTendered" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-alert
          :title="`找零 ${money({ amount: Math.max(yuanToFen(cashForm.cashTendered) - yuanToFen(cashForm.amount), 0), currency: 'CNY' })}`"
          type="info"
          :closable="false"
        />
      </el-form>
      <template #footer>
        <el-button @click="cashDialog = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="submitTender('CASH')">确认收款</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="closeDialog" title="关班与现金小结" width="480px">
      <el-form label-position="top">
        <el-form-item label="应有现金">
          <el-input :model-value="money(shiftView?.expectedCash)" disabled />
        </el-form-item>
        <el-form-item label="实盘现金（元）">
          <el-input-number v-model="closeForm.actualCash" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="差异原因">
          <el-input v-model="closeForm.reason" type="textarea" :rows="3" placeholder="有差异时必须填写" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="submitCloseShift">确认关班</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cashMovementDialog" title="现金收付登记" width="440px">
      <el-form label-position="top">
        <el-form-item label="类型">
          <el-radio-group v-model="cashMovementForm.type">
            <el-radio-button label="CASH_IN">现金收入</el-radio-button>
            <el-radio-button label="CASH_OUT">现金支出</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额（元）">
          <el-input-number v-model="cashMovementForm.amount" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="cashMovementForm.reason" placeholder="例如：备用金补充、采购支出" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cashMovementDialog = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="submitCashMovement">登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pos {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pos-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e7e3da;
  border-radius: 14px;
}

.toolbar-fields,
.search-row,
.shift-summary,
.tender-buttons,
.sale-actions,
.complete-row,
.points-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.shift-summary {
  color: #5c574e;
  font-size: 13px;
}

.shift-summary strong {
  color: #1f2b22;
}

.pos-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 1fr);
  gap: 16px;
  align-items: start;
}

.panel-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h2,
.pos-checkout h2 {
  margin: 0;
}

.product-strip {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 10px;
  max-height: 300px;
  overflow: auto;
  margin-bottom: 16px;
}

.product-chip {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 14px;
  border: 1px solid #e2ded4;
  border-radius: 12px;
  background: #fbfaf6;
  text-align: left;
  cursor: pointer;
}

.product-chip:hover:not(:disabled) {
  border-color: #2f6b4f;
  background: #f2f8f4;
}

.product-chip:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.product-chip .price {
  color: #b0451f;
  font-weight: 700;
}

.cart-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
}

.qty {
  display: inline-block;
  min-width: 32px;
  text-align: center;
}

.sale-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.sale-actions {
  margin-top: 12px;
}

.totals {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 0 16px;
  border-bottom: 1px dashed #dedbd3;
}

.totals > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.totals .payable {
  font-size: 18px;
  color: #1f2b22;
}

.totals .remaining {
  color: #b0451f;
}

.member-box,
.benefit-box,
.tender-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 0;
  border-bottom: 1px dashed #dedbd3;
}

.member-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border-radius: 10px;
  background: #f4f8f5;
}

.member-metrics {
  display: flex;
  gap: 16px;
  color: #2f6b4f;
  font-size: 13px;
}

.tender-amount {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.tender-buttons {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.mock-tag {
  margin-left: 4px;
}

.tender-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 0;
  border-bottom: 1px dashed #dedbd3;
}

.tender-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.complete-row {
  padding-top: 16px;
  justify-content: space-between;
}

.mb-16 {
  margin-bottom: 16px;
}

.mt-12 {
  margin-top: 12px;
}

.manager-row {
  display: flex;
  gap: 8px;
}

.lock-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(18, 22, 19, 0.72);
}

.lock-card {
  display: flex;
  width: min(420px, 100%);
  flex-direction: column;
  gap: 12px;
  padding: 26px;
  border-radius: 18px;
  background: #fff;
}

.lock-card h2 {
  margin: 0;
}

.refund-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

@media (max-width: 1000px) {
  .pos-grid {
    grid-template-columns: 1fr;
  }
}
</style>