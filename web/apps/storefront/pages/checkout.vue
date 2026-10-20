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
const otpCode = ref('')
const otpSending = ref(false)
const devOtpCode = ref('')
const memberToken = ref<string>(process.client ? (localStorage.getItem('fullmall.memberToken') ?? '') : '')
const addressDetail = ref('上海市静安区演示路 1 号')
const legal = ref<LegalDocument | null>(null)
const legalAccepted = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const demoResult = ref<Record<string, unknown> | null>(null)
const idempotencyKey = ref('')
const referral = ref<{ staffId: string; mode: string } | null>(null)
const coords = ref<{ latitude: number; longitude: number } | null>(null)
const locationStatus = ref('正在获取位置…')
const groupBuyId = ref('')
const groupBuyCode = ref('')

onMounted(async () => {
  idempotencyKey.value = crypto.randomUUID()
  groupBuyId.value = localStorage.getItem('fullmall.groupBuyId') ?? ''
  groupBuyCode.value = localStorage.getItem('fullmall.groupBuyCode') ?? ''
  const rawReferral = localStorage.getItem('fullmall.referral')
  if (rawReferral) {
    try {
      referral.value = JSON.parse(rawReferral) as { staffId: string; mode: string }
    } catch {
      localStorage.removeItem('fullmall.referral')
      throw new Error('推广绑定数据损坏，已清除本地推广信息')
    }
  }
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

async function requestOtp() {
  if (!contactPhone.value.trim()) {
    errorMessage.value = '请先填写手机号'
    return
  }
  otpSending.value = true
  errorMessage.value = ''
  try {
    const issued = await $fetch<{ phone: string; debugCode?: string }>(
      `${config.public.apiBase}/api/v1/member/auth/otp`,
      { method: 'POST', body: { phone: contactPhone.value.trim() } })
    // 开发/演示环境会回传验证码，直接填入，方便体验；生产不会返回
    if (issued.debugCode) {
      devOtpCode.value = issued.debugCode
      otpCode.value = issued.debugCode
    }
    ElMessage.success('验证码已发送')
  } catch (cause) {
    errorMessage.value = cause instanceof Error ? cause.message : '获取验证码失败'
  } finally {
    otpSending.value = false
  }
}

/** 访客下单：用手机号 + 验证码换取会员 token（订单会绑定该会员）。 */
async function ensureMemberToken(): Promise<string> {
  if (memberToken.value) {
    return memberToken.value
  }
  if (!otpCode.value.trim()) {
    throw new Error('请输入短信验证码')
  }
  const session = await $fetch<{ token: string }>(
    `${config.public.apiBase}/api/v1/member/auth/verify`,
    { method: 'POST', body: { phone: contactPhone.value.trim(), code: otpCode.value.trim() } })
  memberToken.value = session.token
  if (process.client) {
    localStorage.setItem('fullmall.memberToken', session.token)
  }
  return session.token
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
  if (groupBuyId.value && (cart.value.length !== 1 || cart.value[0]?.quantity !== 1)) {
    errorMessage.value = '拼团订单只能包含 1 件拼团商品，请清空购物车后重新从拼团页进入'
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
    const token = await ensureMemberToken()
    // 游客订单会返回访问令牌（响应头），保存后用于查看订单/履约
    let guestToken = ''
    const order = await $fetch<Order>(`${config.public.apiBase}/api/v1/checkout`, {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey.value, Authorization: `Bearer ${token}` },
      onResponse: ({ response }) => {
        guestToken = response.headers.get('x-order-token') ?? ''
      },
      body: {
        items: cart.value.map(line => ({ skuId: line.skuId, quantity: line.quantity })),
        fulfillmentMode: mode.value,
        storeId: mode.value === 'PICKUP' ? selectedStoreId.value : undefined,
        contact: { name: contactName.value, phone: contactPhone.value },
        address: mode.value === 'SHIP' ? { detail: addressDetail.value } : undefined,
        legalAcceptanceIds: [acceptance.id],
        referrerId: referral.value?.staffId,
        referralMode: referral.value?.mode,
        groupBuyId: groupBuyId.value || undefined
      }
    })
    await $fetch(`${config.public.apiBase}/api/v1/payments/mock/${order.id}/pay`, { method: 'POST' })
    if (guestToken) {
      localStorage.setItem(`fullmall.orderToken.${order.id}`, guestToken)
    }
    remember(order.id)
    clear()
    localStorage.removeItem('fullmall.groupBuyId')
    localStorage.removeItem('fullmall.groupBuyCode')
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
          <input v-model="contactPhone" type="tel" placeholder="请输入手机号">
        </div>
        <div class="field">
          <label>短信验证码</label>
          <div class="otp-row">
            <input v-model="otpCode" type="text" placeholder="请输入验证码">
            <button type="button" class="location-button" :disabled="otpSending" @click="requestOtp">
              {{ otpSending ? '发送中…' : '获取验证码' }}
            </button>
          </div>
          <span v-if="devOtpCode" class="muted">演示环境自动填入：{{ devOtpCode }}</span>
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
        <div v-if="groupBuyId" class="group-buy-note">
          <strong>拼团订单</strong>
          <span>团号 {{ groupBuyCode || groupBuyId }}，成团前请邀请好友参团；未成团将自动退款。</span>
        </div>
        <p class="summary-hint">秒杀、买赠与拼团价格由服务端结算时校验，最终应付以订单为准。</p>
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

<style scoped>
.group-buy-note {
  display: grid;
  gap: 4px;
  margin: 12px 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff5e0;
  color: #7a5209;
  font-size: 13px;
}

.summary-hint {
  margin: 8px 0 0;
  color: #7b857d;
  font-size: 12px;
}
</style>
