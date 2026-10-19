<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import {
  fulfillmentStatusLabel,
  fulfillmentTypeLabel,
  orderStatusLabel
} from '@fullmall/api-client'
import type { AfterSale, Fulfillment, Order } from '~/types'

const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const { data: order, error, refresh: refreshOrder } = await useFetch<Order>(
  `${base}/api/v1/orders/${route.params.id}`
)
const { data: fulfillments } = await useFetch<Fulfillment[]>(
  `${base}/api/v1/orders/${route.params.id}/fulfillments`
)

if (error.value || !order.value) {
  throw createError({ statusCode: 404, statusMessage: '订单不存在' })
}

const { data: afterSales, refresh: refreshAfterSales } = await useFetch<AfterSale[]>(
  `${base}/api/v1/orders/${route.params.id}/after-sales`,
  { query: { phone: order.value.contactPhone } }
)

const afterSaleForm = reactive({
  type: 'RETURN',
  reason: '',
  description: ''
})
const quantities = reactive<Record<string, number>>({})
const submitting = ref(false)
const message = ref('')
const formError = ref('')

const canApply = computed(() => ['PAID', 'ALLOCATED', 'PARTIALLY_FULFILLED', 'FULFILLED']
  .includes(order.value?.status ?? ''))

const statusLabels: Record<string, string> = {
  REQUESTED: '待审核',
  APPROVED: '已同意',
  REJECTED: '已拒绝',
  RECEIVED: '已退货入库',
  REFUNDED: '已退款',
  CANCELLED: '已取消'
}

async function submitAfterSale() {
  formError.value = ''
  message.value = ''
  const items = (order.value?.lines ?? [])
    .filter(line => (quantities[line.id] ?? 0) > 0)
    .map(line => ({ orderLineId: line.id, quantity: Math.trunc(quantities[line.id]) }))
  if (!afterSaleForm.reason.trim()) {
    formError.value = '请填写售后原因'
    return
  }
  if (!items.length) {
    formError.value = '请选择至少一个商品和数量'
    return
  }
  submitting.value = true
  try {
    await $fetch(`${config.public.apiBase}/api/v1/orders/${order.value?.id}/after-sales`, {
      method: 'POST',
      body: {
        type: afterSaleForm.type,
        reason: afterSaleForm.reason.trim(),
        description: afterSaleForm.description.trim() || undefined,
        contactPhone: order.value?.contactPhone,
        items
      }
    })
    message.value = '售后申请已提交，运营审核后会联系您。'
    afterSaleForm.reason = ''
    afterSaleForm.description = ''
    Object.keys(quantities).forEach(key => delete quantities[key])
    await Promise.all([refreshOrder(), refreshAfterSales()])
  } catch (cause) {
    formError.value = cause instanceof Error ? cause.message : '售后申请提交失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section v-if="order" class="order-page">
    <div class="section-heading">
      <h1>订单 {{ order.orderNo }}</h1>
      <span class="status-badge">{{ orderStatusLabel(order.status) }}</span>
    </div>

    <div class="panel">
      <h2>商品</h2>
      <ul class="summary-list">
        <li v-for="line in order.lines" :key="line.id">
          <span>{{ line.title }} × {{ line.quantity }} · {{ line.fulfillmentType }}</span>
          <strong>{{ formatMoney({ amount: line.unitPrice.amount * line.quantity, currency: line.unitPrice.currency }) }}</strong>
        </li>
      </ul>
      <div class="summary-total">
        <span>合计</span>
        <strong>{{ formatMoney(order.total) }}</strong>
      </div>
    </div>

    <div class="panel">
      <h2>履约</h2>
      <div v-for="fulfillment in fulfillments ?? []" :key="fulfillment.id" class="fulfillment-row">
        <div>
          <strong>{{ fulfillmentTypeLabel(fulfillment.type) }}</strong>
          <span class="status-badge">{{ fulfillmentStatusLabel(fulfillment.status) }}</span>
        </div>
        <p v-if="fulfillment.pickupCode">
          提货码：<strong class="pickup-code">{{ fulfillment.pickupCode }}</strong>
        </p>
        <p v-if="fulfillment.trackingNo">物流单号：{{ fulfillment.trackingNo }}</p>
      </div>
    </div>

    <div v-if="canApply" class="panel after-sale-form">
      <h2>申请售后</h2>
      <p class="muted">支持退货退款或换货。退货商品入库后进入隔离批次，质检完成再退款。</p>
      <div class="form-grid">
        <label>
          <span>售后类型</span>
          <select v-model="afterSaleForm.type">
            <option value="RETURN">退货退款</option>
            <option value="EXCHANGE">换货</option>
          </select>
        </label>
        <label>
          <span>原因</span>
          <input v-model="afterSaleForm.reason" placeholder="例如：商品破损、不喜欢、少件" />
        </label>
      </div>
      <label class="full-row">
        <span>补充说明</span>
        <textarea v-model="afterSaleForm.description" rows="2" placeholder="可选：描述问题、期望处理方式" />
      </label>
      <div class="after-sale-lines">
        <div v-for="line in order.lines" :key="line.id" class="after-sale-line">
          <span>{{ line.title }} <small>（购买 {{ line.quantity }} 件）</small></span>
          <input v-model.number="quantities[line.id]" type="number" min="0" :max="line.quantity" />
        </div>
      </div>
      <p v-if="formError" class="form-error">{{ formError }}</p>
      <p v-if="message" class="form-success">{{ message }}</p>
      <button type="button" class="button" :disabled="submitting" @click="submitAfterSale">
        {{ submitting ? '提交中…' : '提交售后申请' }}
      </button>
    </div>

    <div v-if="afterSales?.length" class="panel">
      <h2>售后记录</h2>
      <div v-for="afterSale in afterSales" :key="afterSale.id" class="after-sale-record">
        <div>
          <strong>{{ afterSale.afterSaleNo }}</strong>
          <span class="status-badge">{{ statusLabels[afterSale.status] || afterSale.status }}</span>
        </div>
        <p>{{ afterSale.type === 'RETURN' ? '退货退款' : '换货' }} · {{ afterSale.reason }} · 退款 {{ formatMoney(afterSale.refundAmount) }}</p>
        <p v-if="afterSale.adminNote" class="muted">处理备注：{{ afterSale.adminNote }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.after-sale-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.after-sale-form label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #5c574e;
}

.after-sale-form input,
.after-sale-form select,
.after-sale-form textarea {
  padding: 9px 10px;
  border: 1px solid #d8d4ca;
  border-radius: 8px;
  font: inherit;
}

.after-sale-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
  background: #f7f5ef;
}

.after-sale-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.after-sale-line input {
  width: 90px;
}

.form-error {
  margin: 0;
  color: #a51f18;
  font-size: 13px;
}

.form-success {
  margin: 0;
  color: #22643d;
  font-size: 13px;
}

.after-sale-record {
  padding: 12px 0;
  border-top: 1px dashed #dedbd3;
}

.after-sale-record:first-of-type {
  border-top: 0;
}
</style>