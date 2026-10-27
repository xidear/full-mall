<script setup lang="ts">
import { formatMemberDateTime } from '~/utils/datetime'
import { orderStatusLabel } from '@fullmall/api-client'
import type { Order } from '~/types'

const memberTimezone = useCookie<string | null>('fullmall_member_timezone').value ?? undefined
const memberCookie = useCookie<string | null>('fullmall_member_token')
const config = useRuntimeConfig()
const { orderIds } = useOrders()
const orders = ref<Order[]>([])
const loading = ref(false)
const error = ref('')

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

useHead({ title: '我的订单 · Full Mall' })
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
        :to="`/orders/${order.id}`"
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
      <p v-if="!loading && !orders.length && !error" class="empty-state">
        还没有订单。完成一次下单后会在这里看到。
      </p>
    </div>
  </section>
</template>

<style scoped>
.account-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.account-nav { display: flex; gap: 14px; flex-wrap: wrap; margin: 4px 0 18px; }
.account-nav a { color: var(--brand, #e1251b); font-size: 14px; }
.account-error { margin-bottom: 16px; }
</style>
