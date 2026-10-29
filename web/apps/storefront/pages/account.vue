<script setup lang="ts">
import { formatMemberDateTime } from '~/utils/datetime'
import { orderStatusLabel } from '@fullmall/api-client'
import type { Order } from '~/types'

// 会员时区优先，未登录时用站点默认时区（避免 SSR 与浏览器显示不一致）。
const memberTimezone = useDisplayTimeZone()
const memberCookie = useCookie<string | null>('fullmall_member_token')
const config = useRuntimeConfig()
// orderIds 是本机 localStorage 记住的最近 50 笔订单号（useOrders），不是后端按会员返回的全部订单：
// 换设备、清缓存或换个浏览器登录同一账号，这里就看不到旧订单——这是已知边界，不是加载失败。
const { orderIds } = useOrders()
const orders = ref<Order[]>([])
const loading = ref(false)
const error = ref('')

// 会员令牌同时从 cookie（SSR 可读）和 localStorage（兼容历史登录）取，优先 cookie；
// 访客订单另带 X-Order-Token。两个令牌都没有时请求会 401，由下面按单条失败汇总，不静默丢弃。
function authHeaders(orderId: string): Record<string, string> {
  const cookieToken = memberCookie.value ?? ''
  const localToken = import.meta.client ? (localStorage.getItem('fullmall.memberToken') ?? '') : ''
  const memberToken = cookieToken || localToken
  const guestToken = import.meta.client
    ? (localStorage.getItem(`fullmall.orderToken.${orderId}`) ?? '')
    : ''
  return {
    ...(memberToken ? { Authorization: `Bearer ${memberToken}` } : {}),
    ...(guestToken ? { 'X-Order-Token': guestToken } : {})
  }
}

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    // 用 allSettled 而不是 all：单笔订单失败（令牌过期/不属于本账号）不该让已经取到的订单全部消失，
    // 所以部分失败时仍然展示成功的那些，并把“有几笔没加载出来”如实告诉顾客，文案不暴露状态码与路径。
    const results = await Promise.allSettled(
      orderIds.value.map(id =>
        $fetch<Order>(`${config.public.apiBase}/api/v1/orders/${id}`, {
          headers: authHeaders(id)
        })
      )
    )
    orders.value = results
      .filter((item): item is PromiseFulfilledResult<Order> => item.status === 'fulfilled')
      .map(item => item.value)
    const failed = results.filter(item => item.status === 'rejected').length
    if (failed) {
      error.value = failed === results.length
        ? '订单加载失败：登录状态可能已过期，或订单不属于当前账号。请重新登录后重试。'
        : `有 ${failed} 笔订单暂时无法加载（可能登录状态过期），其余订单已显示。`
    }
  } catch (cause) {
    error.value = cause instanceof Error ? '订单加载失败，请稍后重试。' : '订单加载失败'
  } finally {
    loading.value = false
  }
})

const { siteName } = useSite()
useHead(() => ({ title: `我的订单 · ${siteName.value}` }))
</script>

<template>
  <section class="account-page">
    <div class="section-heading">
      <div>
        <p class="eyebrow">我的账户</p>
        <h1>我的订单</h1>
      </div>
      <div class="account-actions">
        <NuxtLink to="/login" class="button button-secondary">登录 / 注册</NuxtLink>
        <NuxtLink to="/products" class="button">继续购物</NuxtLink>
      </div>
    </div>

    <nav class="account-nav" aria-label="账户快捷入口">
      <NuxtLink to="/favorites">我的收藏</NuxtLink>
      <NuxtLink to="/history">浏览足迹</NuxtLink>
      <NuxtLink to="/notifications">消息中心</NuxtLink>
      <NuxtLink to="/cart">购物车</NuxtLink>
    </nav>

    <p v-if="error" class="account-error">{{ error }}</p>
    <p v-if="loading" class="empty-state">订单加载中…</p>
    <div class="order-list">
      <NuxtLink
        v-for="order in orders"
        :key="order.id"
        :to="`/orders/${order.orderNo}`"
        class="order-card panel"
      >
        <div class="order-card-head">
          <strong>{{ order.orderNo }}</strong>
          <span class="status-badge">{{ orderStatusLabel(order.status) }}</span>
        </div>
        <p class="muted">{{ order.createdAt ? formatMemberDateTime(order.createdAt, memberTimezone) : '' }}</p>
        <div class="order-card-lines">
          <span v-for="line in order.lines" :key="line.id">{{ line.title }} × {{ line.quantity }}</span>
        </div>
        <div class="order-card-foot">
          <span>{{ order.fulfillmentMode === 'PICKUP' ? '门店自提' : '快递配送' }}</span>
          <strong>{{ formatMoney(order.total) }}</strong>
        </div>
      </NuxtLink>
      <!-- 三个条件缺一不可：加载中或出错时都还没有完整结论，此时说“还没有订单”等于把失败当空态。 -->
      <EmptyState v-if="!loading && !orders.length && !error" icon="box" title="还没有订单" description="完成一次下单后，订单会出现在这里。">
        <NuxtLink to="/products" class="button">去逛商品</NuxtLink>
      </EmptyState>
    </div>
  </section>
</template>

<style scoped>
.account-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.account-nav { display: flex; gap: 14px; flex-wrap: wrap; margin: 4px 0 18px; }
.account-nav a { color: var(--brand, #e1251b); font-size: 14px; }
.account-error { margin-bottom: 16px; }
</style>
