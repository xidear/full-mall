<script setup lang="ts">
// 会员时区：由会员资料接口提供；未登录时为 undefined（退回浏览器本地时区）
const memberTimezone = useCookie<string | null>('fullmall_member_timezone').value ?? undefined
import { computed } from 'vue'

interface Favorite {
  skuId: string
  productId: string
  title: string
  priceAmount: number
  currency: string
  createdAt: string
}

const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const memberToken = useCookie<string | null>('fullmall_member_token').value ?? ''
const headers: Record<string, string> = memberToken ? { Authorization: `Bearer ${memberToken}` } : {}

const { data: favorites, pending, error } = await useAsyncData<Favorite[]>('favorites', () =>
  $fetch<Favorite[]>(`${base}/api/v1/favorites`, { headers }))

const items = computed(() => favorites.value ?? [])

function price(value: number, currency: string) {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency }).format(value / 100)
}
</script>

<template>
  <section class="page">
    <h1>我的收藏</h1>

    <p v-if="!memberToken" class="hint">登录后可查看收藏。</p>
    <p v-else-if="pending" class="hint">加载中…</p>
    <p v-else-if="error" class="hint error">收藏加载失败，请稍后再试。</p>
    <p v-else-if="!items.length" class="hint">还没有收藏，去商品页点「收藏」试试。</p>

    <ul v-else class="favorite-list">
      <li v-for="item in items" :key="item.skuId">
        <NuxtLink :to="`/product/${item.productId}`">{{ item.title }}</NuxtLink>
        <strong>{{ price(item.priceAmount, item.currency) }}</strong>
        <span class="muted">{{ formatMemberDate(item.createdAt, memberTimezone) }}</span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.page { max-width: 720px; margin: 0 auto; padding: 24px 16px; }
.hint { color: #6b7280; }
.error { color: #dc2626; }
.favorite-list { list-style: none; padding: 0; display: grid; gap: 12px; }
.favorite-list li {
  display: grid; grid-template-columns: 1fr auto auto; gap: 12px; align-items: center;
  padding: 12px 16px; border: 1px solid #e5e7eb; border-radius: 10px;
}
.muted { color: #9ca3af; font-size: 13px; }
</style>
