<script setup lang="ts">
import { onMounted, ref } from 'vue'

interface LocalOrder { id: string; orderNo: string; totalAmount: number; at: number }
interface OrderView { id: string; orderNo: string; status: string; totalAmount?: number }

const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:41280'
const orders = ref<{ local: LocalOrder; status: string }[]>([])
const loading = ref(false)
const error = ref('')

onMounted(() => {
  const local = (uni.getStorageSync('fullmall.mobile.orders') as LocalOrder[]) || []
  const token = uni.getStorageSync('fullmall.mobile.token') as string || ''
  if (!local.length) { orders.value = []; return }
  loading.value = true
  let done = 0
  local.forEach(item => {
    uni.request({
      url: `${apiBase}/api/v1/orders/${item.id}`,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: response => {
        if (response.statusCode === 200) {
          const body = response.data as OrderView
          orders.value.push({ local: item, status: body.status })
        } else if (response.statusCode === 404) {
          orders.value.push({ local: item, status: '需登录或已失效' })
        } else {
          orders.value.push({ local: item, status: `查询失败 ${response.statusCode}` })
        }
      },
      fail: () => { orders.value.push({ local: item, status: '网络异常' }) },
      complete: () => {
        done++
        if (done === local.length) {
          loading.value = false
          orders.value.sort((a, b) => b.local.at - a.local.at)
        }
      }
    })
  })
})

const money = (v?: number) => `¥${((v ?? 0) / 100).toFixed(2)}`
</script>

<template>
  <view class="page">
    <text v-if="loading" class="muted">查询中…</text>
    <text v-if="error" class="error">{{ error }}</text>
    <view v-if="!orders.length && !loading" class="empty">
      <text class="muted">还没有订单</text>
      <button size="mini" @click="uni.navigateTo({ url: '/pages/index/index' })">去逛逛</button>
    </view>
    <view v-for="row in orders" :key="row.local.id" class="order">
      <view class="row">
        <text class="no">{{ row.local.orderNo }}</text>
        <text class="status">{{ row.status }}</text>
      </view>
      <view class="row muted">
        <text>{{ new Date(row.local.at).toLocaleString() }}</text>
        <text>{{ money(row.local.totalAmount) }}</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 24rpx; background: #f6f5f2; min-height: 100vh; }
button { min-height: 88rpx; }
.order { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx; }
.row { display: flex; justify-content: space-between; align-items: center; }
.no { font-size: 28rpx; font-weight: 600; }
.status { font-size: 24rpx; color: #d4380d; }
.muted { color: #888; font-size: 24rpx; }
.error { color: #cf1322; }
.empty { display: flex; flex-direction: column; align-items: center; gap: 16rpx; padding: 80rpx 0; }
</style>
