<script setup lang="ts">
import { computed, ref } from 'vue'

interface CartItem { skuId: string; title: string; price: number; quantity: number }

const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const key = 'fullmall.mobile.cart'
const cart = ref<CartItem[]>(uni.getStorageSync(key) as CartItem[] || [])
const form = ref({ name: '', phone: '', province: '', city: '', district: '', detail: '' })
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

async function submit() {
  if (submitting.value) return
  const message = validate()
  if (message) { error.value = message; return }
  error.value = ''
  submitting.value = true
  const idempotencyKey = `mobile-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  uni.request({
    url: `${apiBase}/api/v1/checkout`,
    method: 'POST',
    header: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey
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
    </view>

    <template v-else>
      <view class="panel">
        <text class="line">合计：{{ money(total) }}（{{ cart.length }} 种商品）</text>
      </view>

      <view class="panel">
        <input v-model="form.name" class="field" placeholder="收货人" />
        <input v-model="form.phone" class="field" type="number" maxlength="11" placeholder="手机号" />
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
.field { border: 2rpx solid #e5e5e5; border-radius: 8rpx; padding: 16rpx; font-size: 28rpx; }
.line { font-size: 28rpx; }
.ok { font-size: 32rpx; font-weight: 700; color: #18a058; }
.hint { font-size: 24rpx; color: #888; }
.error { color: #cf1322; font-size: 26rpx; margin: 8rpx 0 16rpx; }
.submit { margin-top: 8rpx; }
</style>
