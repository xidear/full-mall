<script setup lang="ts">
import { computed, ref } from 'vue'

interface CartItem { skuId: string; title: string; price: number; quantity: number }

const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const key = 'fullmall.mobile.cart'
const cart = ref<CartItem[]>(uni.getStorageSync(key) as CartItem[] || [])
const form = ref({ name: '', phone: '', code: '', province: '', city: '', district: '', detail: '' })
const codeSent = ref(false)
const memberToken = ref('')
const submitting = ref(false)
const error = ref('')
const placed = ref<{ id: string; orderNo: string; totalAmount: number } | null>(null)

const total = computed(() => cart.value.reduce((sum, i) => sum + i.price * i.quantity, 0))
const money = (v: number) => `¥${(v / 100).toFixed(2)}`

function validate(): string | null {
  if (!cart.value.length) return '购物车是空的'
  if (!form.value.name.trim()) return '请填写收货人'
  if (!/^1[3-9]\d{9}$/.test(form.value.phone.trim())) return '手机号格式不正确'
  if (!form.value.province.trim() || !form.value.city.trim() || !form.value.district.trim()) return '请填写省市区'
  if (!form.value.detail.trim()) return '请填写详细地址'
  return null
}

function requestCode() {
  if (!/^1[3-9]\d{9}$/.test(form.value.phone.trim())) {
    error.value = '先填正确手机号'
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
      legalAcceptanceIds: []
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
  <view class="page">
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
        <input v-model="form.phone" class="field" type="number" maxlength="11" placeholder="手机号" />
        <view class="code-row">
          <input v-model="form.code" class="field code-input" type="number" maxlength="6" placeholder="短信验证码" />
          <button class="code-btn" size="mini" @click="requestCode">{{ codeSent ? '重新获取' : '获取验证码' }}</button>
        </view>
        <input v-model="form.province" class="field" placeholder="省" />
        <input v-model="form.city" class="field" placeholder="市" />
        <input v-model="form.district" class="field" placeholder="区/县" />
        <input v-model="form.detail" class="field" placeholder="详细地址" />
      </view>

      <text v-if="error" class="error">{{ error }}</text>
      <button class="submit" type="primary" :loading="submitting" @click="submit">提交订单</button>
    </template>
  </view>
</template>

<style scoped>
.page { padding: 24rpx; background: #f6f5f2; min-height: 100vh; }
.panel { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 20rpx; display: flex; flex-direction: column; gap: 16rpx; }
.code-row { display: flex; gap: 12rpx; align-items: center; }
.code-input { flex: 1; }
.code-btn { font-size: 24rpx; }
.field { border: 2rpx solid #e5e5e5; border-radius: 8rpx; padding: 16rpx; font-size: 28rpx; }
.line { font-size: 28rpx; }
.ok { font-size: 32rpx; font-weight: 700; color: #18a058; }
.hint { font-size: 24rpx; color: #888; }
.error { color: #cf1322; font-size: 26rpx; margin: 8rpx 0 16rpx; }
.submit { margin-top: 8rpx; }
</style>
