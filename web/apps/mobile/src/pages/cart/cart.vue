<script setup lang="ts">
import { computed, ref } from 'vue'

interface CartItem { skuId: string; title: string; price: number; quantity: number }

const key = 'fullmall.mobile.cart'
const items = ref<CartItem[]>(uni.getStorageSync(key) as CartItem[] || [])

const total = computed(() =>
  items.value.reduce((sum, item) => sum + item.price * item.quantity, 0))

function money(value: number) {
  return `¥${(value / 100).toFixed(2)}`
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
  // 下单需要地址与手机号，移动端表单尚未做：明确告知，不伪造订单
  uni.showModal({
    title: '暂未开通',
    content: '移动端下单表单（地址/手机号）还没做，请先在网页版完成下单。',
    showCancel: false
  })
}
</script>

<template>
  <view class="page">
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
.page { padding: 24rpx; background: #f6f5f2; min-height: 100vh; }
.hint { color: #888; text-align: center; padding: 60rpx; }
.row { display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx; }
.info { display: flex; flex-direction: column; }
.title { font-size: 28rpx; }
.price { color: #d4380d; font-size: 26rpx; }
.qty { display: flex; align-items: center; gap: 12rpx; }
.count { min-width: 40rpx; text-align: center; }
.summary { display: flex; justify-content: space-between; align-items: center;
  margin-top: 24rpx; background: #fff; border-radius: 12rpx; padding: 20rpx; }
</style>
