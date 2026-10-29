<!--
  只做「本地记下的订单号 + 服务端最新状态」的合并展示；支付/取消在别的端，
  但顾客必须能在这里看清是否已付款。
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useMobileTheme } from '../../theme'
import { formatMoney } from '../../utils/money'

const { vars: themeVars } = useMobileTheme()

interface LocalOrder { id: string; orderNo: string; totalAmount: number; at: number }
interface OrderView { id: string; orderNo: string; status: string; totalAmount?: number }

const apiBase = import.meta.env.VITE_API_BASE
  || `${location.protocol}//${location.hostname}:41280`
const orders = ref<{ local: LocalOrder; status: string }[]>([])
const loading = ref(false)
const error = ref('')

onMounted(() => {
  const local = (uni.getStorageSync('fullmall.mobile.orders') as LocalOrder[]) || []
  const token = uni.getStorageSync('fullmall.mobile.token') as string || ''
  if (!local.length) { orders.value = []; return }
  loading.value = true
  let done = 0
  // uni.request 是回调式、没有 Promise.all：用 complete 计数（成功失败都算）判断全部返回，
  // 否则任何一次失败都会让 loading 永不收起。
  local.forEach(item => {
    uni.request({
      url: `${apiBase}/api/v1/orders/${item.id}`,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: response => {
        if (response.statusCode === 200) {
          const body = response.data as OrderView
          orders.value.push({ local: item, status: body.status })
        } else if (response.statusCode === 404) {
          // 404 有两义：token 过期，或这单不属于当前会员；顾客能做的都是重新登录后再看。
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

const money = (v?: number) => formatMoney(v)
// 状态文案的唯一映射：后端新增状态必须同步这里，否则会落到 statusText 的兜底分支显示「处理中」。
const statusLabels: Record<string, string> = {
  PENDING_PAYMENT: '待付款',
  PAID: '已付款',
  ALLOCATED: '备货中',
  PARTIALLY_FULFILLED: '部分发货',
  FULFILLED: '已完成',
  CANCELLED: '已取消',
  REFUNDED: '已退款'
}
// 未知的机器状态不要直接抛给顾客；自定义提示（网络异常等）原样保留。
const statusText = (value: string) =>
  statusLabels[value] ?? (/^[A-Z_]+$/.test(value) ? '处理中' : value)
</script>

<template>
  <view class="page" :style="themeVars">
    <text v-if="loading" class="muted">查询中…</text>
    <text v-if="error" class="error">{{ error }}</text>
    <view v-if="!orders.length && !loading" class="empty">
      <text class="muted">还没有订单</text>
      <button size="mini" @click="uni.navigateTo({ url: '/pages/index/index' })">去逛逛</button>
    </view>
    <view v-for="row in orders" :key="row.local.id" class="order">
      <view class="row">
        <text class="no">{{ row.local.orderNo }}</text>
        <text class="status">{{ statusText(row.status) }}</text>
      </view>
      <view class="row muted">
        <text>{{ new Date(row.local.at).toLocaleString() }}</text>
        <text>{{ money(row.local.totalAmount) }}</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 24rpx; background: var(--bg); color: var(--text); min-height: 100vh; }
button { min-height: 88rpx; }
.order { background: var(--surface); border: 1rpx solid var(--line); border-radius: calc(var(--radius) * 1.5); padding: 20rpx; margin-bottom: 16rpx; }
.row { display: flex; justify-content: space-between; align-items: center; }
.no { font-size: 28rpx; font-weight: 600; }
.status { font-size: 24rpx; color: var(--brand); }
.muted { color: var(--muted); font-size: 24rpx; }
.error { color: var(--danger); }
.empty { display: flex; flex-direction: column; align-items: center; gap: 16rpx; padding: 80rpx 0; }
</style>
