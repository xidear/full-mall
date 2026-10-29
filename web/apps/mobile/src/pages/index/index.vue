<!--
  三态互斥：loading（骨架）/ error（失败）/ empty（成功但没数据）。
  不要用空数组表示失败——否则顾客看到「暂无商品」，真实原因（网络/后端挂了）被吞掉。
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useMobileTheme } from '../../theme'
import { formatMoney } from '../../utils/money'

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
// 只有 H5 有 location；小程序/App 端必须构建期注入 VITE_API_BASE，否则地址拼不出来、全部 fail。
const apiBase = import.meta.env.VITE_API_BASE
  || `${location.protocol}//${location.hostname}:41280`
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
      console.error('[mobile] 商品加载失败:', failure.errMsg)
      error.value = '商品加载失败，请稍后重试'
    },
    complete: () => {
      // 用 complete 而不是 success：失败也必须收起骨架，否则断网时永远停在「加载中」。
      loading.value = false
    }
  })
}

function goOrders() {
  uni.navigateTo({ url: '/pages/orders/orders' })
}

function toDetail(product: Product) {
  uni.navigateTo({ url: `/pages/product/detail?id=${product.id}` })
}

function price(product: Product) {
  const money = product.skus[0]?.price
  if (!money) return '-'
  return formatMoney(money.amount, money.currency)
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

    <view class="quick-links">
      <button size="mini" @click="goOrders">我的订单</button>
      <button size="mini" @click="uni.navigateTo({ url: '/pages/cart/cart' })">购物车</button>
    </view>

    <view class="hero">
      <text class="eyebrow">FULL MALL</text>
      <text class="title">{{ presets.find(item => item.preset === theme)?.bannerText }}</text>
      <text class="subtitle">uni-app 多端骨架 · H5 / 微信小程序 / APP</text>
    </view>

    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="error" class="state error">{{ error }}</view>
    <view v-else>
      <view v-for="product in products" :key="product.id" class="card" @click="toDetail(product)">
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
      <view v-if="!products.length" class="state empty">
        <text class="empty-title">暂无商品</text>
        <text class="empty-desc">换个分类或稍后再来看看。</text>
      </view>
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

.state.empty {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  margin-top: 24rpx;
  border: 1rpx dashed var(--line);
  border-radius: calc(var(--radius) * 1.5);
  background: var(--surface);
}

.empty-title {
  color: var(--text);
  font-size: 30rpx;
  font-weight: 700;
}

.empty-desc {
  color: var(--muted);
  font-size: 24rpx;
}

.state.error {
  color: var(--accent);
}
.quick-links { display: flex; gap: 12rpx; padding: 16rpx 24rpx 0; }
</style>