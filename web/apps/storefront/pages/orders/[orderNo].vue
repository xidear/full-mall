<script setup lang="ts">
import { formatMemberDate, formatMemberDateTime } from '~/utils/datetime'
// 会员时区优先，未登录时用站点默认时区（避免 SSR 与浏览器显示不一致）。
const memberTimezone = useDisplayTimeZone()
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
// 顾客看到的订单地址使用业务订单号，而不是数据库 UUID。
const orderNo = String(route.params.orderNo || '')
// 会员/访客下单后都需要带 token 才能查看自己的订单（服务端按归属校验）
// 服务端渲染读 cookie、客户端也读 cookie（localStorage 在 SSR 取不到，曾导致硬刷新 404）
const memberToken = useCookie<string | null>('fullmall_member_token').value ?? ''
// 游客令牌优先取 cookie（SSR 也能读），再退回 localStorage（老会话只存过 localStorage）。
const guestTokenCookie = useCookie<string | null>(`fullmall_ot_${orderNo}`).value ?? ''
const guestToken = guestTokenCookie
  || (process.client ? (localStorage.getItem(`fullmall.orderToken.${orderNo}`) ?? '') : '')
const authHeaders: Record<string, string> = {
  ...(memberToken ? { Authorization: `Bearer ${memberToken}` } : {}),
  ...(guestToken ? { 'X-Order-Token': guestToken } : {})
}
// 路由既能接业务订单号（顾客看到的规范地址），也能接 UUID：
// 内部系统/测试/客服发的链接两种都有，只认一种会让其中一种直接 404（曾导致跟踪用例 404）。
const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(orderNo)
const orderEndpoint = isUuid ? `${base}/api/v1/orders/${orderNo}` : `${base}/api/v1/orders/no/${orderNo}`
const { data: order, error, refresh: refreshOrder } = await useFetch<Order>(
  orderEndpoint,
  {
    key: `order-${orderNo}`, headers: authHeaders }
)
const { data: fulfillments } = await useFetch<Fulfillment[]>(
  `${base}/api/v1/orders/${order.value?.id}/fulfillments`,
  {
    key: `fulfillments-${order.value?.id}`, headers: authHeaders }
)

if (error.value || !order.value) {
  throw createError({ statusCode: 404, statusMessage: '订单不存在' })
}

interface TrackingEvent { at: string; status: string; location: string }
const hasShipped = computed(() => (fulfillments.value ?? []).some(item => item.trackingNo))
// 显式状态：不用 useAsyncData 的 pending，避免把 data 当对象调用（曾导致按钮卡在"查询中…"）
const tracking = ref<TrackingEvent[]>([])
const trackingLoading = ref(false)
const trackingError = ref('')

async function loadTracking() {
  if (!hasShipped.value || trackingLoading.value) {
    return
  }
  trackingLoading.value = true
  trackingError.value = ''
  try {
    tracking.value = await $fetch<TrackingEvent[]>(
      `${base}/api/v1/orders/${order.value?.id}/tracking`,
      { headers: authHeaders })
  } catch (cause) {
    trackingError.value = apiErrorMessage(cause, '物流查询失败，请稍后重试')
  } finally {
    trackingLoading.value = false
  }
}

const { data: afterSales, refresh: refreshAfterSales } = await useFetch<AfterSale[]>(
  `${base}/api/v1/orders/${order.value?.id}/after-sales`,
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

// 评价只能对已支付/已完成的订单发起（后端同样校验，这里只是提前隐藏入口）。
const canReview = computed(() => ['PAID', 'FULFILLED'].includes(order.value?.status ?? ''))

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
    formError.value = apiErrorMessage(cause, '售后申请提交失败，请稍后重试')
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

    <div v-if="canReview && memberToken" class="panel">
      <h2>评价商品</h2>
      <p class="muted">可以传图片或视频，上限由站方配置；评价审核通过后展示在商品页。</p>
      <ReviewForm :order-id="order.id" :lines="order.lines" :token="memberToken" />
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
        <p v-if="fulfillment.trackingNo">
          物流单号：{{ fulfillment.trackingNo }}
          <button type="button" class="link-button" :disabled="trackingLoading" @click="loadTracking">
            {{ trackingLoading ? '查询中…' : '查看物流轨迹' }}
          </button>
        </p>
      </div>
    </div>

    <!-- 已发货就渲染面板：轨迹为空时也要给用户明确反馈，而不是"点了没反应" -->
    <div v-if="hasShipped || tracking?.length || trackingError" class="panel">
      <h2>物流轨迹</h2>
      <p v-if="trackingLoading" class="muted">查询中…</p>
      <p v-else-if="!tracking?.length && !trackingError" class="muted">
        暂无物流轨迹（承运商尚未回传），请稍后再查。
      </p>
      <p v-if="trackingError" class="form-error">物流信息暂时不可用，请稍后再试。</p>
      <ul v-else class="tracking-list">
        <li v-for="(event, index) in tracking" :key="index">
          <span class="tracking-time">{{ formatMemberDateTime(event.at, memberTimezone) }}</span>
          <strong>{{ event.status }}</strong>
          <span class="muted">{{ event.location }}</span>
        </li>
      </ul>
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
          <span class="status-badge">{{ statusLabels[afterSale.status] || '处理中' }}</span>
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
  color: var(--muted);
}

.after-sale-form input,
.after-sale-form select,
.after-sale-form textarea {
  padding: 9px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  font: inherit;
}

.after-sale-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
  background: var(--surface-muted);
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
  color: var(--danger);
  font-size: 13px;
}

.form-success {
  margin: 0;
  color: var(--brand-dark);
  font-size: 13px;
}

.after-sale-record {
  padding: 12px 0;
  border-top: 1px dashed var(--line);
}

.after-sale-record:first-of-type {
  border-top: 0;
}
</style>

<style scoped>
.link-button {
  margin-left: 8px;
  padding: 0;
  border: none;
  background: none;
  color: var(--brand);
  cursor: pointer;
}

.tracking-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.tracking-list li {
  display: grid;
  grid-template-columns: 160px 120px 1fr;
  gap: 8px;
  font-size: 14px;
}

.tracking-time {
  color: var(--muted);
}
</style>
