<script setup lang="ts">
import { onMounted, ref } from 'vue'

interface Money { amount: number; currency: string }
interface Sku { id: string; skuCode: string; price: Money; options?: Record<string, string> }
interface Product {
  id: string
  title: string
  description?: string
  skus: Sku[]
}

const apiBase = import.meta.env.VITE_API_BASE
  || `${location.protocol}//${location.hostname}:41280`
const product = ref<Product | null>(null)
const loading = ref(true)
const error = ref('')
const selected = ref<Sku | null>(null)

onMounted(() => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as unknown as { options?: { id?: string } }
  const id = current?.options?.id
  if (!id) {
    error.value = '缺少商品 ID'
    loading.value = false
    return
  }
  uni.request({
    url: `${apiBase}/api/v1/products/${id}`,
    data: { locale: 'zh-CN' },
    success: response => {
      const payload = response.data as Product
      product.value = payload
      selected.value = payload.skus?.[0] ?? null
    },
    fail: failure => { error.value = failure.errMsg || '加载失败' },
    complete: () => { loading.value = false }
  })
})

function money(value?: Money) {
  if (!value) return '-'
  return `¥${(value.amount / 100).toFixed(2)}`
}

function addToCart() {
  if (!product.value || !selected.value) return
  const key = 'fullmall.mobile.cart'
  const cart = uni.getStorageSync(key) as Array<{ skuId: string; title: string; price: number; quantity: number }> || []
  const existing = cart.find(item => item.skuId === selected.value!.id)
  if (existing) {
    existing.quantity += 1
  } else {
    cart.push({
      skuId: selected.value.id,
      title: product.value.title,
      price: selected.value.price.amount,
      quantity: 1
    })
  }
  uni.setStorageSync(key, cart)
  uni.showToast({ title: '已加入购物车', icon: 'success' })
}

function goCart() {
  uni.navigateTo({ url: '/pages/cart/cart' })
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="hint">加载中…</view>
    <view v-else-if="error" class="hint error">{{ error }}</view>
    <template v-else-if="product">
      <view class="title">{{ product.title }}</view>
      <view class="price">{{ money(selected?.price) }}</view>
      <view v-if="product.description" class="desc">{{ product.description }}</view>

      <view class="sku-list">
        <view v-for="sku in product.skus" :key="sku.id" class="sku"
              :class="{ active: selected?.id === sku.id }" @click="selected = sku">
          <text>{{ money(sku.price) }}</text>
          <text class="code">{{ sku.skuCode }}</text>
        </view>
      </view>

      <view class="actions">
        <button class="btn" type="primary" @click="addToCart">加入购物车</button>
        <button class="btn ghost" @click="goCart">去购物车</button>
      </view>
    </template>
  </view>
</template>

<style scoped>
.page { padding: 24rpx; background: #f6f5f2; min-height: 100vh; }
.hint { color: #888; padding: 40rpx; text-align: center; }
.hint.error { color: #cf1322; }
.title { font-size: 36rpx; font-weight: 700; }
.price { font-size: 44rpx; color: #d4380d; margin: 16rpx 0; }
.desc { color: #666; font-size: 26rpx; margin-bottom: 24rpx; }
.sku-list { display: flex; flex-wrap: wrap; gap: 16rpx; }
.sku { padding: 16rpx; border: 2rpx solid #ddd; border-radius: 12rpx; display: flex; flex-direction: column; }
.sku.active { border-color: #d4380d; color: #d4380d; }
.sku .code { font-size: 20rpx; color: #999; }
.actions { display: flex; gap: 16rpx; margin-top: 32rpx; }
.btn { flex: 1; }
.btn.ghost { background: #fff; color: #d4380d; border: 2rpx solid #d4380d; }
</style>
