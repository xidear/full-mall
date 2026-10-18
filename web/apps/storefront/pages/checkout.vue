<script setup lang="ts">
import type { LegalDocument, Order, Store } from '~/types'

const config = useRuntimeConfig()
const base = import.meta.client ? config.public.apiBase : config.apiInternalBase
const { cart, total, clear } = useCart()
const { remember } = useOrders()
const mode = ref<'SHIP' | 'PICKUP'>('SHIP')
const stores = ref<Store[]>([])
const selectedStoreId = ref('')
const contactName = ref('演示用户')
const contactPhone = ref('13800000000')
const addressDetail = ref('上海市静安区演示路 1 号')
const legal = ref<LegalDocument | null>(null)
const legalAccepted = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const demoResult = ref<Record<string, unknown> | null>(null)
const idempotencyKey = ref('')
const coords = ref<{ latitude: number; longitude: number } | null>(null)
const locationStatus = ref('正在获取位置…')

onMounted(async () => {
  idempotencyKey.value = crypto.randomUUID()
  await loadStores()
  legal.value = await $fetch<LegalDocument>(`${config.public.apiBase}/api/v1/legal/documents`, {
    query: { type: 'TERMS', locale: 'zh-CN', market: 'CN' }
  })
  requestLocation()
})

async function loadStores() {
  const storesResponse = await $fetch<Store[]>(`${config.public.apiBase}/api/v1/stores`, {
    query: {
      skuId: cart.value[0]?.skuId,
      latitude: coords.value?.latitude,
      longitude: coords.value?.longitude
    }
  })
  stores.value = storesResponse
  if (!selectedStoreId.value || !storesResponse.some(store => store.id === selectedStoreId.value)) {
    selectedStoreId.value = storesResponse[0]?.id ?? ''
  }
}

function requestLocation() {
  if (!navigator.geolocation) {
    locationStatus.value = '浏览器不支持定位，显示全部门店'
    return
  }
  navigator.geolocation.getCurrentPosition(
    position => {
      coords.value = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude
      }
      locationStatus.value = '已按当前位置排序'
      loadStores()
    },
    () => {
      locationStatus.value = '未授权定位，显示全部门店'
    },
    { timeout: 8000, maximumAge: 300000 }
  )
}

async function submit() {
  errorMessage.value = ''
  if (!cart.value.length) {
    errorMessage.value = '购物车为空'
    return
  }
  if (!legalAccepted.value) {
    errorMessage.value = '请先阅读并同意服务条款'
    return
  }
  if (mode.value === 'PICKUP' && !selectedStoreId.value) {
    errorMessage.value = '请选择自提门店'
    return
  }
  submitting.value = true
  try {
    if (config.public.appMode === 'demo') {
      demoResult.value = await $fetch<Record<string, unknown>>(`${config.public.apiBase}/mock/checkout`, {
        query: { scenario: 'paid' }
      })
      return
    }

    const acceptance = await $fetch<{ id: string }>(`${config.public.apiBase}/api/v1/legal/acceptances`, {
      method: 'POST',
      body: {
        documentVersionId: legal.value?.id,
        locale: 'zh-CN',
        method: 'CLICKWRAP'
      }
    })
    const order = await $fetch<Order>(`${config.public.apiBase}/api/v1/checkout`, {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey.value },
      body: {
        items: cart.value.map(line => ({ skuId: line.skuId, quantity: line.quantity })),
        fulfillmentMode: mode.value,
        storeId: mode.value === 'PICKUP' ? selectedStoreId.value : undefined,
        contact: { name: contactName.value, phone: contactPhone.value },
        address: mode.value === 'SHIP' ? { detail: addressDetail.value } : undefined,
        legalAcceptanceIds: [acceptance.id]
      }
    })
    await $fetch(`${config.public.apiBase}/api/v1/payments/mock/${order.id}/pay`, { method: 'POST' })
    remember(order.id)
    clear()
    await navigateTo(`/orders/${order.id}`)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '下单失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="checkout-page">
    <div class="section-heading">
      <h1>结算</h1>
      <span>{{ cart.length }} 个商品</span>
    </div>

    <div class="checkout-grid">
      <div class="panel">
        <h2>履约方式</h2>
        <div class="segmented">
          <button type="button" :class="{ active: mode === 'SHIP' }" @click="mode = 'SHIP'">快递配送</button>
          <button type="button" :class="{ active: mode === 'PICKUP' }" @click="mode = 'PICKUP'">门店自提</button>
        </div>

        <div v-if="mode === 'PICKUP'" class="field">
          <label>自提门店 <span class="muted">（{{ locationStatus }}）</span></label>
          <select v-model="selectedStoreId">
            <option v-for="store in stores" :key="store.id" :value="store.id">
              {{ store.name }} · {{ store.address }}
              <template v-if="store.distanceMeters"> · 距您 {{ (store.distanceMeters / 1000).toFixed(1) }}km</template>
            </option>
          </select>
          <button type="button" class="location-button" @click="requestLocation">使用我的位置</button>
        </div>
        <div v-else class="field">
          <label>收货地址</label>
          <input v-model="addressDetail" type="text" placeholder="请输入详细地址">
        </div>

        <div class="field">
          <label>联系人</label>
          <input v-model="contactName" type="text">
        </div>
        <div class="field">
          <label>手机号</label>
          <input v-model="contactPhone" type="tel">
        </div>
      </div>

      <aside class="panel summary-panel">
        <h2>订单摘要</h2>
        <ul class="summary-list">
          <li v-for="line in cart" :key="line.skuId">
            <span>{{ line.title }} × {{ line.quantity }}</span>
            <strong>{{ formatMoney({ amount: line.price.amount * line.quantity, currency: line.price.currency }) }}</strong>
          </li>
        </ul>
        <div class="summary-total">
          <span>合计</span>
          <strong>{{ formatMoney(total) }}</strong>
        </div>

        <label class="checkbox-row">
          <input v-model="legalAccepted" type="checkbox">
          <span>
            我已阅读并同意
            <NuxtLink to="/legal/TERMS" target="_blank">《服务条款》</NuxtLink>
            和
            <NuxtLink to="/legal/PRIVACY" target="_blank">《隐私政策》</NuxtLink>
          </span>
        </label>

        <button type="button" class="button full-width" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中…' : config.public.appMode === 'demo' ? '查看演示结果' : 'Mock 支付并下单' }}
        </button>
        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        <div v-if="demoResult" class="demo-result-card">
          <strong>演示模式：流程已模拟</strong>
          <p>订单状态：已支付；履约方式：门店自提；提货码：654321。</p>
          <p>公开演示站不会创建真实订单、扣减库存或发送真实消息。</p>
        </div>
      </aside>
    </div>
  </section>
</template>