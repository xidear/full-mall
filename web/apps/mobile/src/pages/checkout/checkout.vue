<!--
  一条链路：取条款版本 → 验证码换会员 token（未注册即注册）→ 写条款接受记录 → 创建订单。
  任一步失败都停在原地，不允许「看起来下单成功其实没落库」：订单号与应付金额都取后端返回值。
-->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { VALIDATION } from '@fullmall/api-client'
import { useMobileTheme } from '../../theme'
import { formatMoney } from '../../utils/money'
// 手机号规则来自 docs/validation.schema.json 生成的 VALIDATION（AGENTS.md §9.1），
// 这里只是镜像后端做快速反馈；禁止在页面里手写正则，提交结果以后端为准。

const { market, vars: themeVars } = useMobileTheme()

interface CartItem { skuId: string; title: string; price: number; quantity: number }

const apiBase = import.meta.env.VITE_API_BASE
  || `${location.protocol}//${location.hostname}:41280`
const key = 'fullmall.mobile.cart'
const cart = ref<CartItem[]>(uni.getStorageSync(key) as CartItem[] || [])
const form = ref({ name: '', phone: '', code: '', province: '', city: '', district: '', detail: '' })
const codeSent = ref(false)
const memberToken = ref('')
const submitting = ref(false)
const error = ref('')
const placed = ref<{ id: string; orderNo: string; totalAmount: number } | null>(null)
const legalDocId = ref('')
const legalAccepted = ref(false)

const total = computed(() => cart.value.reduce((sum, i) => sum + i.price * i.quantity, 0))
const money = (v: number) => formatMoney(v)
const PHONE_PATTERN = new RegExp(VALIDATION.phonePattern)

// 条款版本 id 拿不到必须立刻提示：否则用户填完所有字段、点提交才失败，极易误认为已下单成功。
onMounted(() => {
  uni.request({
    url: `${apiBase}/api/v1/legal/documents`,
    data: { type: 'TERMS', locale: 'zh-CN', market: market.value },
    success: response => {
      if (response.statusCode === 200) {
        legalDocId.value = (response.data as { id: string }).id
      } else {
        error.value = '服务条款加载失败，请稍后重试'
      }
    },
    fail: failure => { error.value = failure.errMsg || '服务条款加载失败' }
  })
})

function validate(): string | null {
  if (!cart.value.length) return '购物车是空的'
  if (!form.value.name.trim()) return '请填写收货人'
  if (!PHONE_PATTERN.test(form.value.phone.trim())) return '请输入正确的手机号（可带国家区号）'
  if (!form.value.province.trim() || !form.value.city.trim() || !form.value.district.trim()) return '请填写省市区'
  if (!form.value.detail.trim()) return '请填写详细地址'
  if (!legalAccepted.value) return '请先阅读并同意服务条款'
  if (!legalDocId.value) return '服务条款加载失败，请稍后重试'
  return null
}

function requestCode() {
  if (!PHONE_PATTERN.test(form.value.phone.trim())) {
    error.value = '请先填写正确的手机号'
    return
  }
  uni.request({
    url: `${apiBase}/api/v1/member/auth/otp`,
    method: 'POST',
    header: { 'Content-Type': 'application/json' },
    data: { phone: form.value.phone.trim() },
    success: response => {
      if (response.statusCode !== 200) { error.value = '获取验证码失败'; return }
      codeSent.value = true
      const body = response.data as { debugCode?: string }
      if (body.debugCode) {
      // debugCode 只在后端开发/演示配置下返回，生产没有该字段；自动填入只是联调便利，不能当正式流程。
        // 开发模式后端会回传验证码，仅用于本地联调
        form.value.code = body.debugCode
        uni.showToast({ title: `开发模式验证码 ${body.debugCode}`, icon: 'none' })
      }
    },
    fail: failure => { error.value = failure.errMsg || '网络异常' }
  })
}

function verify(): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${apiBase}/api/v1/member/auth/verify`,
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: { phone: form.value.phone.trim(), code: form.value.code.trim() },
      success: response => {
        if (response.statusCode !== 200) { reject(new Error('验证码不正确')); return }
        const token = (response.data as { token: string }).token
        memberToken.value = token
        // 登录态存在 uni storage（fullmall.mobile.token）；orders.vue 用同一个键查订单状态，改键要同步。
        uni.setStorageSync('fullmall.mobile.token', token)
        resolve(token)
      },
      fail: failure => reject(new Error(failure.errMsg || '网络异常'))
    })
  })
}

async function submit() {
  if (submitting.value) return
  const message = validate()
  if (message) { error.value = message; return }
  error.value = ''
  submitting.value = true
  try {
    if (!memberToken.value) {
      if (!form.value.code.trim()) { error.value = '请先获取并填写短信验证码'; submitting.value = false; return }
      await verify()
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '登录失败'
    submitting.value = false
    return
  }
  const idempotencyKey = `mobile-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  // 顺序不能换：条款接受记录需要登录态，且必须在创建订单之前；创建失败直接中止，
  // 绝不带着空 legalAcceptanceIds 去下单（后端会整单拒掉）。
  // 合规：下单前记录条款接受（与 PC 结算一致），不能传空数组。
  const acceptanceId = await new Promise<string>((resolve, reject) => {
    uni.request({
      url: `${apiBase}/api/v1/legal/acceptances`,
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: { documentVersionId: legalDocId.value, locale: 'zh-CN', method: 'CLICKWRAP' },
      success: response => {
        if (response.statusCode === 201) resolve((response.data as { id: string }).id)
        else reject(new Error('服务条款确认失败，请稍后重试'))
      },
      fail: () => reject(new Error('网络异常，条款确认失败'))
    })
  }).catch(cause => {
    error.value = cause instanceof Error ? cause.message : '服务条款确认失败'
    submitting.value = false
    return ''
  })
  if (!acceptanceId) return
  uni.request({
    url: `${apiBase}/api/v1/checkout`,
    method: 'POST',
    header: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
      ...(memberToken.value ? { Authorization: `Bearer ${memberToken.value}` } : {})
    },
    data: {
      items: cart.value.map(item => ({ skuId: item.skuId, quantity: item.quantity })),
      fulfillmentMode: 'SHIP',
      contact: { name: form.value.name.trim(), phone: form.value.phone.trim() },
      address: {
        province: form.value.province.trim(),
        city: form.value.city.trim(),
        district: form.value.district.trim(),
        detail: form.value.detail.trim()
      },
      legalAcceptanceIds: [acceptanceId]
    },
    success: response => {
      if (response.statusCode !== 201) {
        const body = response.data as { message?: string }
        error.value = body?.message || `下单失败（${response.statusCode}）`
        return
      }
      placed.value = response.data as { id: string; orderNo: string; totalAmount: number }
      const history = uni.getStorageSync('fullmall.mobile.orders') as Array<{
        id: string; orderNo: string; totalAmount: number; at: number }> || []
      history.unshift({ ...placed.value, at: Date.now() })
      uni.setStorageSync('fullmall.mobile.orders', history.slice(0, 20))
      cart.value = []
      uni.removeStorageSync(key)
    },
    fail: failure => { error.value = failure.errMsg || '网络异常，请稍后重试' },
    complete: () => { submitting.value = false }
  })
}
</script>

<template>
  <view class="page" :style="themeVars">
    <view v-if="placed" class="panel">
      <text class="ok">下单成功</text>
      <text class="line">订单号：{{ placed.orderNo }}</text>
      <text class="line">应付：{{ money(placed.totalAmount) }}</text>
      <text class="hint">请在「我的订单」完成支付；未支付订单会按时释放库存。</text>
      <button class="btn" type="primary" @click="uni.navigateTo({ url: '/pages/orders/orders' })">查看我的订单</button>
    </view>

    <template v-else>
      <view class="panel">
        <text class="line">合计：{{ money(total) }}（{{ cart.length }} 种商品）</text>
      </view>

      <view class="panel">
        <input v-model="form.name" class="field" placeholder="收货人" />
        <input v-model="form.phone" class="field" type="text" placeholder="手机号（海外号码请带 + 区号）" />
        <view class="code-row">
          <input v-model="form.code" class="field code-input" type="number" maxlength="6" placeholder="短信验证码" />
          <button class="code-btn" size="mini" @click="requestCode">{{ codeSent ? '重新获取' : '获取验证码' }}</button>
        </view>
        <input v-model="form.province" class="field" placeholder="省" />
        <input v-model="form.city" class="field" placeholder="市" />
        <input v-model="form.district" class="field" placeholder="区/县" />
        <input v-model="form.detail" class="field" placeholder="详细地址" />
        <label class="legal">
          <checkbox :checked="legalAccepted" @click="legalAccepted = !legalAccepted" />
          <text>我已阅读并同意《服务条款》与《隐私政策》；未注册手机号验证后将自动创建账号。</text>
        </label>
      </view>

      <text v-if="error" class="error">{{ error }}</text>
      <button class="submit" type="primary" :loading="submitting" @click="submit">提交订单</button>
    </template>
  </view>
</template>

<style scoped>
.page { padding: 24rpx; background: var(--bg); color: var(--text); min-height: 100vh; }
.panel { background: var(--surface); border: 1rpx solid var(--line); border-radius: calc(var(--radius) * 1.5); padding: 24rpx; margin-bottom: 20rpx; display: flex; flex-direction: column; gap: 16rpx; }
.code-row { display: flex; gap: 12rpx; align-items: center; }
.code-input { flex: 1; }
.code-btn { font-size: 24rpx; }
.field { border: 2rpx solid var(--line); border-radius: 8rpx; padding: 16rpx; background: var(--surface); color: var(--text); font-size: 28rpx; }
.line { font-size: 28rpx; }
.ok { font-size: 32rpx; font-weight: 700; color: var(--brand); }
.hint { font-size: 24rpx; color: var(--muted); }
.error { color: var(--danger); font-size: 26rpx; margin: 8rpx 0 16rpx; }
.submit { margin-top: 8rpx; min-height: 88rpx; line-height: 88rpx; }
button { min-height: 88rpx; }
input, textarea { font-size: 32rpx; }
</style>
