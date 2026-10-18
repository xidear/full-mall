<script setup lang="ts">
import {
  fulfillmentStatusLabel,
  fulfillmentTypeLabel,
  orderStatusLabel
} from '@fullmall/api-client'
import type { Fulfillment, Order } from '~/types'

const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const { data: order, error } = await useFetch<Order>(`${base}/api/v1/orders/${route.params.id}`)
const { data: fulfillments } = await useFetch<Fulfillment[]>(`${base}/api/v1/orders/${route.params.id}/fulfillments`)

if (error.value || !order.value) {
  throw createError({ statusCode: 404, statusMessage: '订单不存在' })
}
</script>

<template>
  <section v-if="order" class="order-page">
    <div class="section-heading">
      <h1>订单 {{ order.orderNo }}</h1>
      <span class="status-badge">{{ orderStatusLabel(order.status) }}</span>
    </div>

    <div class="panel">
      <h2>商品</h2>
      <ul class="summary-list">
        <li v-for="line in order.lines" :key="line.id">
          <span>{{ line.title }} × {{ line.quantity }} · {{ line.fulfillmentType }}</span>
          <strong>{{ formatMoney({ amount: line.unitPrice.amount * line.quantity, currency: line.unitPrice.currency }) }}</strong>
        </li>
      </ul>
      <div class="summary-total">
        <span>合计</span>
        <strong>{{ formatMoney(order.total) }}</strong>
      </div>
    </div>

    <div class="panel">
      <h2>履约</h2>
      <div v-for="fulfillment in fulfillments ?? []" :key="fulfillment.id" class="fulfillment-row">
        <div>
          <strong>{{ fulfillmentTypeLabel(fulfillment.type) }}</strong>
          <span class="status-badge">{{ fulfillmentStatusLabel(fulfillment.status) }}</span>
        </div>
        <p v-if="fulfillment.pickupCode">
          提货码：<strong class="pickup-code">{{ fulfillment.pickupCode }}</strong>
        </p>
        <p v-if="fulfillment.trackingNo">物流单号：{{ fulfillment.trackingNo }}</p>
      </div>
    </div>
  </section>
</template>