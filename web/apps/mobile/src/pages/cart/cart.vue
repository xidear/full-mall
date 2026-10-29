<!--
  购物车以本地 storage 为唯一数据源（未登录也能先攒商品）；price 单位是分，本地合计只用于展示，
  价格/库存/优惠由服务端在 checkout 裁决，页面不做预判。
-->
<script setup lang="ts">
import { computed, ref } from 'vue'
import { useMobileTheme } from '../../theme'
import { formatMoney } from '../../utils/money'

const { vars: themeVars } = useMobileTheme()

interface CartItem { skuId: string; title: string; price: number; quantity: number }

// 这个键被 detail.vue（加购）、checkout.vue（读车/清车）共用，三处必须字面量一致，
// 改名会让老用户的购物车「静默消失」。
const key = 'fullmall.mobile.cart'
const items = ref<CartItem[]>(uni.getStorageSync(key) as CartItem[] || [])

const total = computed(() =>
  items.value.reduce((sum, item) => sum + item.price * item.quantity, 0))

function money(value: number) {
  return formatMoney(value)
}

function persist() {
  uni.setStorageSync(key, items.value)
}

function change(item: CartItem, delta: number) {
  item.quantity += delta
  if (item.quantity <= 0) {
    items.value = items.value.filter(row => row.skuId !== item.skuId)
  }
  persist()
}

function checkout() {
  if (!items.value.length) {
    uni.showToast({ title: '购物车是空的', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/checkout/checkout' })
}
</script>

<template>
  <view class="page" :style="themeVars">
    <view v-if="!items.length" class="hint">购物车是空的</view>
    <template v-else>
      <view v-for="item in items" :key="item.skuId" class="row">
        <view class="info">
          <text class="title">{{ item.title }}</text>
          <text class="price">{{ money(item.price) }}</text>
        </view>
        <view class="qty">
          <button size="mini" @click="change(item, -1)">-</button>
          <text class="count">{{ item.quantity }}</text>
          <button size="mini" @click="change(item, 1)">+</button>
        </view>
      </view>

      <view class="summary">
        <text>合计 {{ money(total) }}</text>
        <button type="primary" size="mini" @click="checkout">去下单</button>
      </view>
    </template>
  </view>
</template>

<style scoped>
.page { padding: 24rpx; background: var(--bg); color: var(--text); min-height: 100vh; }
.hint { color: var(--muted); text-align: center; padding: 60rpx; }
.row { display: flex; justify-content: space-between; align-items: center;
  background: var(--surface); border: 1rpx solid var(--line); border-radius: calc(var(--radius) * 1.5); padding: 20rpx; margin-bottom: 16rpx; }
.info { display: flex; flex-direction: column; }
.title { font-size: 28rpx; }
.price { color: var(--brand); font-size: 26rpx; }
.qty { display: flex; align-items: center; gap: 12rpx; }
.count { min-width: 40rpx; text-align: center; }
button { min-height: 88rpx; }
/* 88rpx ≈ 44px，移动端可点区域下限；加减按钮做小了在手机上会频繁点错。 */
.summary { display: flex; justify-content: space-between; align-items: center;
  margin-top: 24rpx; background: var(--surface); border: 1rpx solid var(--line); border-radius: calc(var(--radius) * 1.5); padding: 20rpx; }
</style>
