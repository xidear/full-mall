<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useMobileTheme } from '../../theme'

interface Money {
  amount: number
  currency: string
}

interface Product {
  id: string
  title: string
  description?: string
  shelfLifeManaged: boolean
  skus: Array<{ id: string; skuCode: string; price: Money }>
}

const { theme, vars: themeVars, presets, setTheme, loadRemote } = useMobileTheme()
const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const products = ref<Product[]>([])
const loading = ref(true)
const error = ref('')

onMounted(() => {
  loadRemote()
  load()
})

function load() {
  loading.value = true
  error.value = ''
  uni.request({
    url: `${apiBase}/api/v1/products`,
    data: { locale: 'zh-CN', page: 0, size: 20 },
    success: response => {
      const payload = response.data as { items: Product[] }
      products.value = payload.items ?? []
    },
    fail: failure => {
      error.value = failure.errMsg || '商品加载失败'
    },
    complete: () => {
      loading.value = false
    }
  })
}

function price(product: Product) {
  const money = product.skus[0]?.price
  if (!money) return '-'
  return `¥${(money.amount / 100).toFixed(2)}`
}
</script>

<template>
  <view class="page" :style="themeVars">
    <view class="theme-bar">
      <text class="theme-label">风格</text>
      <view
        v-for="preset in presets"
        :key="preset.preset"
        class="theme-chip"
        :class="{ active: theme === preset.preset }"
        @click="setTheme(preset.preset)"
      >
        {{ preset.label }}
      </view>
    </view>

    <view class="hero">
      <text class="eyebrow">FULL MALL</text>
      <text class="title">{{ presets.find(item => item.preset === theme)?.bannerText }}</text>
      <text class="subtitle">uni-app 多端骨架 · H5 / 微信小程序 / APP</text>
    </view>

    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="error" class="state error">{{ error }}</view>
    <view v-else>
      <view v-for="product in products" :key="product.id" class="card">
        <view class="card-head">
          <text class="card-title">{{ product.title }}</text>
          <text v-if="product.shelfLifeManaged" class="tag">批次/效期</text>
        </view>
        <text class="desc">{{ product.description }}</text>
        <view class="card-foot">
          <text class="price">{{ price(product) }}</text>
          <text class="sku">{{ product.skus.length }} 个规格</text>
        </view>
      </view>
      <view v-if="!products.length" class="state">暂无商品</view>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28rpx;
  background: var(--bg);
  color: var(--text);
  font-family: var(--font-family);
}

.theme-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.theme-label {
  color: var(--muted);
  font-size: 22rpx;
}

.theme-chip {
  padding: 8rpx 18rpx;
  border: 1rpx solid var(--line);
  border-radius: 999rpx;
  background: var(--surface);
  color: var(--muted);
  font-size: 22rpx;
}

.theme-chip.active {
  border-color: var(--brand);
  background: var(--brand);
  color: #fff;
}

.hero {
  display: flex;
  flex-direction: column;
  padding: 40rpx;
  border-radius: calc(var(--radius) * 2);
  background: var(--surface-muted);
}

.eyebrow {
  color: var(--accent);
  font-size: 22rpx;
  font-weight: 800;
  letter-spacing: 4rpx;
}

.title {
  margin-top: 16rpx;
  color: var(--text);
  font-size: 56rpx;
  font-weight: 800;
  line-height: 1.15;
}

.subtitle {
  margin-top: 14rpx;
  color: var(--muted);
  font-size: 26rpx;
}

.card {
  margin-top: 24rpx;
  padding: 30rpx;
  border: 1rpx solid var(--line);
  border-radius: calc(var(--radius) * 1.5);
  background: var(--surface);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.card-title {
  color: var(--text);
  font-size: 34rpx;
  font-weight: 700;
}

.tag {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: var(--surface-muted);
  color: var(--brand-dark);
  font-size: 20rpx;
}

.desc {
  display: block;
  margin-top: 12rpx;
  color: var(--muted);
  font-size: 24rpx;
}

.card-foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 22rpx;
}

.price {
  color: var(--brand);
  font-size: 36rpx;
  font-weight: 800;
}

.sku {
  color: var(--muted);
  font-size: 22rpx;
}

.state {
  padding: 60rpx 0;
  color: var(--muted);
  text-align: center;
}

.state.error {
  color: var(--accent);
}
</style>