<script setup lang="ts">
import { orderStatusLabel } from '@fullmall/api-client'
import type { Order } from '~/types'

const config = useRuntimeConfig()
const { orderIds } = useOrders()
const orders = ref<Order[]>([])
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const results = await Promise.all(
      orderIds.value.map(id =>
        $fetch<Order>(`${config.public.apiBase}/api/v1/orders/${id}`)
      )
    )
    orders.value = results
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '订单加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="account-page">
    <div class="section-heading">
      <div>
        <p class="eyebrow">我的账户</p>
        <h1>我的订单</h1>
      </div>
      <NuxtLink to="/products" class="button button-secondary">继续购物</NuxtLink>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
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
        <p class="muted">{{ order.createdAt ? new Date(order.createdAt).toLocaleString('zh-CN') : '' }}</p>
        <div class="order-card-lines">
          <span v-for="line in order.lines" :key="line.id">{{ line.title }} × {{ line.quantity }}</span>
        </div>
        <div class="order-card-foot">
          <span>{{ order.fulfillmentMode === 'PICKUP' ? '门店自提' : '快递配送' }}</span>
          <strong>{{ formatMoney(order.total) }}</strong>
        </div>
      </NuxtLink>
      <p v-if="!loading && !orders.length" class="empty-state">
        还没有订单。完成一次下单后会在这里看到。
      </p>
    </div>
  </section>
</template>