<script setup lang="ts">
import type { LegalDocument, Order, Store } from '~/types'
import { randomUuid } from '~/utils/uuid'
import { apiErrorMessage } from '~/utils/apiError'
import { VALIDATION } from '@fullmall/api-client'

const config = useRuntimeConfig()
const apiBase = import.meta.server ? config.apiInternalBase : config.public.apiBase
const route = useRoute()
const { cart, total, clear } = useCart()
const { remember } = useOrders()
const { token: memberToken } = useMember()
const { locale, t } = useLocale()
const { market, requiresAutoAccountNotice, phonePattern, identifierHint } = useRegionPolicy()
const { siteName } = useSite()
useHead(() => ({ title: `结算 · ${siteName.value}` }))

const mode = ref<'SHIP' | 'PICKUP'>('SHIP')
const stores = ref<Store[]>([])
const storesLoading = ref(false)
const storesError = ref('')
const selectedStoreId = ref('')
const contactName = ref('')
const contactPhone = ref('')
const addressDetail = ref('')
const legalAccepted = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const demoResult = ref<Record<string, unknown> | null>(null)
const idempotencyKey = ref('')
const referral = ref<{ staffId: string; mode: string } | null>(null)
const coords = ref<{ latitude: number; longitude: number } | null>(null)
const locationStatus = ref<'idle' | 'requesting' | 'granted' | 'denied' | 'unavailable' | 'unsupported'>('idle')
const groupBuyId = ref('')
const groupBuyCode = ref('')

const locationText = computed(() => ({
  idle: '按距离排序需要先授权定位',
  requesting: '正在获取位置…',
  granted: '已按当前位置排序',
  denied: '未获得定位权限，当前按默认顺序显示门店',
  unavailable: '定位失败，当前按默认顺序显示门店',
  unsupported: '当前浏览器不支持定位，按默认顺序显示门店'
}[locationStatus.value]))
const canRequestLocation = computed(() =>
  ['idle', 'denied', 'unavailable'].includes(locationStatus.value))

const { data: legalDoc, error: legalError } = await useAsyncData<LegalDocument>('checkout-terms', async () => {
  const load = (targetLocale: string) => $fetch<LegalDocument>(`${apiBase}/api/v1/legal/documents`, {
    query: { type: 'TERMS', locale: targetLocale, market: market.value }
  })
  try {
    return await load(locale.value)
  } catch (cause) {
    const statusCode = (cause as { statusCode?: number } | null)?.statusCode
    if (locale.value === 'zh-CN' || statusCode !== 404) throw cause
    // 条款只有中文时仍允许英文用户下单，但必须明确告知回退。
    return await load('zh-CN')
  }
})
const legal = computed(() => legalDoc.value ?? null)
const legalFallback = computed(() =>
  legal.value && legal.value.locale !== locale.value ? t('checkout.legalFallback') : '')

const DRAFT_KEY = 'fullmall.checkoutDraft'
interface Draft {
  mode: 'SHIP' | 'PICKUP'
  contactName: string
  contactPhone: string
  addressDetail: string
}

function saveDraft() {
  if (!import.meta.client) return
  const draft: Draft = {
    mode: mode.value,
    contactName: contactName.value,
    contactPhone: contactPhone.value,
    addressDetail: addressDetail.value
  }
  localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
}

function restoreDraft() {
  if (!import.meta.client) return
  const raw = localStorage.getItem(DRAFT_KEY)
  if (!raw) return
  try {
    const draft = JSON.parse(raw) as Draft
    if (draft.mode === 'SHIP' || draft.mode === 'PICKUP') mode.value = draft.mode
    contactName.value = draft.contactName || ''
    contactPhone.value = draft.contactPhone || ''
    addressDetail.value = draft.addressDetail || ''
  } catch {
    localStorage.removeItem(DRAFT_KEY)
  }
}

onMounted(async () => {
  // 幂等键在进入页面时生成一次，整个页面生命周期内不再变：提交超时后重试、或重复点提交，
  // 后端按同一键返回同一张订单，不会重复扣款/重复出库；只有刷新页面才会换新键。
  idempotencyKey.value = randomUuid()
  restoreDraft()
  groupBuyId.value = localStorage.getItem('fullmall.groupBuyId') ?? ''
  groupBuyCode.value = localStorage.getItem('fullmall.groupBuyCode') ?? ''
  const rawReferral = localStorage.getItem('fullmall.referral')
  if (rawReferral) {
    try {
      referral.value = JSON.parse(rawReferral) as { staffId: string; mode: string }
    } catch {
      localStorage.removeItem('fullmall.referral')
    }
  }
  await loadStores()
  if (mode.value === 'PICKUP') requestLocation()
})

watch([contactName, contactPhone, addressDetail, mode], saveDraft)

async function loadStores() {
  storesLoading.value = true
  storesError.value = ''
  try {
    stores.value = await $fetch<Store[]>(`${apiBase}/api/v1/stores`, {
      query: {
        skuId: cart.value[0]?.skuId,
        latitude: coords.value?.latitude,
        longitude: coords.value?.longitude
      }
    })
    if (!selectedStoreId.value || !stores.value.some(store => store.id === selectedStoreId.value)) {
      selectedStoreId.value = stores.value[0]?.id ?? ''
    }
  } catch (cause) {
    // 失败必须落到 storesError；不能让它表现成“暂无自提门店”，否则顾客会以为这单不支持自提，
    // 而真实原因是门店服务不可用——两者的下一步动作不同，不能合并成一个空态。
    storesError.value = apiErrorMessage(cause, '门店列表加载失败，请稍后重试')
  } finally {
    storesLoading.value = false
  }
}

function requestLocation() {
  if (!import.meta.client) return
  if (!navigator.geolocation) {
    locationStatus.value = 'unsupported'
    return
  }
  locationStatus.value = 'requesting'
  navigator.geolocation.getCurrentPosition(
    position => {
      coords.value = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude
      }
      locationStatus.value = 'granted'
      void loadStores()
    },
    error => {
      // 明确区分“用户拒绝”和“定位失败”，并保留重新授权的入口。
      locationStatus.value = error.code === error.PERMISSION_DENIED ? 'denied' : 'unavailable'
    },
    { timeout: 8000, maximumAge: 300000 }
  )
}

watch(mode, value => {
  saveDraft()
  if (value === 'PICKUP' && locationStatus.value === 'idle') {
    // 切到自提时再请求权限，避免一进结算页就弹系统弹窗。
    requestLocation()
  }
})

async function submit() {
  errorMessage.value = ''
  if (!cart.value.length) {
    errorMessage.value = '购物车为空'
    return
  }
  // 未登录先登录：不要先要求填写地址，否则用户点“登录后下单”却看到地址校验。
  if (config.public.appMode !== 'demo' && !memberToken.value) {
    await navigateTo({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (mode.value === 'SHIP' && !addressDetail.value.trim()) {
    errorMessage.value = '请填写收货地址'
    return
  }
  if (!contactName.value.trim()) {
    errorMessage.value = '请填写联系人'
    return
  }
  if (!phonePattern.test(contactPhone.value.trim())) {
    errorMessage.value = identifierHint.value
    return
  }
  if (!legalAccepted.value) {
    errorMessage.value = '请先阅读并同意服务条款'
    return
  }
  if (!legal.value) {
    errorMessage.value = '服务条款加载失败，请刷新页面后重试'
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

  if (config.public.appMode === 'demo') {
    submitting.value = true
    try {
      demoResult.value = await $fetch<Record<string, unknown>>(`${apiBase}/mock/checkout`, {
        query: { scenario: 'paid' }
      })
    } catch (cause) {
      errorMessage.value = apiErrorMessage(cause, '演示下单失败，请稍后重试')
    } finally {
      submitting.value = false
    }
    return
  }

  submitting.value = true
  try {
    const acceptance = await $fetch<{ id: string }>(`${apiBase}/api/v1/legal/acceptances`, {
      method: 'POST',
      body: {
        documentVersionId: legal.value.id,
        locale: legal.value.locale,
        method: 'CLICKWRAP'
      }
    })
    let guestToken = ''
    const order = await $fetch<Order>(`${apiBase}/api/v1/checkout`, {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey.value, Authorization: `Bearer ${memberToken.value}` },
      onResponse: ({ response }) => {
        guestToken = response.headers.get('x-order-token') ?? ''
      },
      body: {
        items: cart.value.map(line => ({ skuId: line.skuId, quantity: line.quantity })),
        fulfillmentMode: mode.value,
        storeId: mode.value === 'PICKUP' ? selectedStoreId.value : undefined,
        contact: { name: contactName.value.trim(), phone: contactPhone.value.trim() },
        address: mode.value === 'SHIP' ? { detail: addressDetail.value.trim() } : undefined,
        legalAcceptanceIds: [acceptance.id],
        referrerId: referral.value?.staffId,
        referralMode: referral.value?.mode,
        groupBuyId: groupBuyId.value || undefined
      }
    })
    // 下单与“支付”是两次独立请求：订单创建成功后这里失败，页面只会显示“下单失败”，但订单其实已存在。
    // 所以失败文案要能重试，并且顾客最终以订单页状态为准；不要在这里就地清购物车。
    await $fetch(`${apiBase}/api/v1/payments/mock/${order.id}/pay`, { method: 'POST' })
    if (guestToken) {
      // 同时按订单号保存：顾客端地址使用 orderNo，页面在拿到订单前就要能带上访客令牌。
      localStorage.setItem(`fullmall.orderToken.${order.orderNo}`, guestToken)
      localStorage.setItem(`fullmall.orderToken.${order.id}`, guestToken)
    }
    remember(order.id)
    clear()
    localStorage.removeItem(DRAFT_KEY)
    localStorage.removeItem('fullmall.groupBuyId')
    localStorage.removeItem('fullmall.groupBuyCode')
    await navigateTo(`/orders/${order.orderNo}`)
  } catch (cause) {
    errorMessage.value = apiErrorMessage(cause, '下单失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const submitLabel = computed(() => {
  if (submitting.value) return t('checkout.submitting')
  if (config.public.appMode === 'demo') return t('checkout.demoResult')
  if (!memberToken.value) return t('checkout.loginToOrder')
  return t('checkout.submit')
})
</script>

<template>
  <section class="checkout-page">
    <div class="section-heading">
      <h1>{{ t('checkout.title') }}</h1>
      <span>{{ cart.length }} {{ t('checkout.items') }}</span>
    </div>

    <div v-if="!memberToken" class="checkout-login">
      <div>
        <strong>{{ t('checkout.login.title') }}</strong>
        <p>{{ t('checkout.login.desc') }}</p>
      </div>
      <NuxtLink :to="{ path: '/login', query: { redirect: route.fullPath } }" class="button">
        {{ t('login.submit') }}
      </NuxtLink>
    </div>

    <div class="checkout-grid">
      <div class="panel">
        <h2>{{ t('checkout.fulfillment') }}</h2>
        <div class="segmented" role="group" aria-label="履约方式">
          <button type="button" :class="{ active: mode === 'SHIP' }" :aria-pressed="mode === 'SHIP'" @click="mode = 'SHIP'">{{ t('checkout.ship') }}</button>
          <button type="button" :class="{ active: mode === 'PICKUP' }" :aria-pressed="mode === 'PICKUP'" @click="mode = 'PICKUP'">{{ t('checkout.pickup') }}</button>
        </div>

        <div v-if="mode === 'PICKUP'" class="field">
          <label for="pickup-store">{{ t('checkout.store') }}</label>
          <select id="pickup-store" v-model="selectedStoreId" :disabled="storesLoading || !stores.length">
            <option v-if="!stores.length" value="">{{ storesLoading ? '正在加载门店…' : '暂无可自提门店' }}</option>
            <option v-for="store in stores" :key="store.id" :value="store.id">
              {{ store.name }} · {{ store.address }}
              <template v-if="store.distanceMeters"> · 距您 {{ (store.distanceMeters / 1000).toFixed(1) }}km</template>
            </option>
          </select>
          <div class="location-row">
            <span class="muted">{{ locationText }}</span>
            <button v-if="canRequestLocation" type="button" class="location-button" @click="requestLocation">
              {{ locationStatus === 'idle' ? '允许定位并排序' : '重新获取位置' }}
            </button>
          </div>
          <p v-if="locationStatus === 'denied'" class="field-hint">
            浏览器已记住拒绝结果：可在地址栏左侧的站点设置中重新允许定位，或直接从列表选择门店。
          </p>
          <p v-if="storesError" class="error-text">{{ storesError }}</p>
          <p v-if="!storesLoading && !stores.length && !storesError" class="field-hint">
            当前商品暂无可自提门店，可
            <button type="button" class="link-button" @click="mode = 'SHIP'">改为快递配送</button>。
          </p>
        </div>
        <div v-else class="field">
          <label for="ship-address">{{ t('checkout.address') }}</label>
          <input id="ship-address" v-model="addressDetail" type="text" :placeholder="t('checkout.addressPlaceholder')" autocomplete="street-address">
        </div>

        <div class="field">
          <label for="contact-name">{{ t('checkout.contact') }}</label>
          <input id="contact-name" v-model="contactName" type="text" autocomplete="name" :maxlength="VALIDATION.contactNameMaxLength" :placeholder="t('checkout.contactPlaceholder')">
        </div>
        <div class="field">
          <label for="contact-phone">{{ t('checkout.phone') }}</label>
          <input id="contact-phone" v-model="contactPhone" type="tel" autocomplete="tel" :placeholder="t('checkout.phonePlaceholder')">
          <p class="field-hint">{{ identifierHint }}</p>
        </div>
      </div>

      <aside class="panel summary-panel">
        <h2>{{ t('checkout.summary') }}</h2>
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
        <p class="summary-hint">{{ t('checkout.summaryHint') }}</p>
        <!-- 合计是购物车快照价的预估：促销、优惠券、运费都由结算接口在服务端算，前端不重算也不改价；
             提交失败时这里仍是预估值，不代表已扣款，实际应付以订单返回为准。 -->
        <div class="summary-total">
          <span>{{ t('checkout.total') }}</span>
          <strong>{{ formatMoney(total) }}</strong>
        </div>

        <label class="checkbox-row">
          <input v-model="legalAccepted" type="checkbox">
          <span>
            {{ t('checkout.agree') }}
            <NuxtLink to="/legal/TERMS" target="_blank">《{{ t('legal.type.TERMS') }}》</NuxtLink>
            {{ t('common.and') }}
            <NuxtLink to="/legal/PRIVACY" target="_blank">《{{ t('legal.type.PRIVACY') }}》</NuxtLink>
          </span>
        </label>
        <p v-if="requiresAutoAccountNotice && !memberToken" class="compliance-note">
          {{ t('checkout.account.auto') }}
        </p>
        <p v-if="legalFallback" class="field-hint">{{ legalFallback }}</p>
        <p v-if="legalError" class="error-text">{{ t('checkout.termsLoadError') }}</p>

        <button type="button" class="button full-width" :disabled="submitting" @click="submit">
          {{ submitLabel }}
        </button>
        <p v-if="errorMessage" class="error-text" role="alert">{{ errorMessage }}</p>
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
.checkout-login {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 16px 20px;
  border: 1px solid color-mix(in srgb, var(--brand) 28%, var(--line));
  border-radius: var(--radius);
  background: color-mix(in srgb, var(--brand) 6%, var(--surface));
}

.checkout-login strong {
  display: block;
  margin-bottom: 4px;
}

.checkout-login p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.location-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
}

.field-hint {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.link-button {
  padding: 0;
  border: 0;
  background: none;
  color: var(--brand);
  font-size: inherit;
  text-decoration: underline;
}

.compliance-note {
  margin: 4px 0 14px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.6;
}

.error-text {
  color: var(--danger);
  font-size: 13px;
}

.group-buy-note {
  display: grid;
  gap: 4px;
  margin: 12px 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--accent) 12%, var(--surface));
  color: var(--accent);
  font-size: 13px;
}

.summary-hint {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 640px) {
  .checkout-login {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
