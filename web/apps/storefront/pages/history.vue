<script setup lang="ts">
import { computed } from 'vue'

interface HistoryEntry {
  skuId: string
  productId: string
  title: string
  priceAmount: number
  currency: string
  viewedAt: string
  viewCount: number
}

const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const memberToken = useCookie<string | null>('fullmall_member_token').value ?? ''
const headers: Record<string, string> = memberToken ? { Authorization: `Bearer ${memberToken}` } : {}

const { data, pending, error } = await useAsyncData<HistoryEntry[]>('history', () =>
  $fetch<HistoryEntry[]>(`${base}/api/v1/history`, { headers, query: { limit: 50 } }))

const items = computed(() => data.value ?? [])

function price(value: number, currency: string) {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency }).format(value / 100)
}
</script>

<template>
  <section class="page">
    <h1>浏览足迹</h1>

    <p v-if="!memberToken" class="hint">登录后可查看足迹。</p>
    <p v-else-if="pending" class="hint">加载中…</p>
    <p v-else-if="error" class="hint error">足迹加载失败，请稍后再试。</p>
    <p v-else-if="!items.length" class="hint">还没有浏览记录。</p>

    <ul v-else class="list">
      <li v-for="item in items" :key="item.skuId">
        <NuxtLink :to="`/product/${item.productId}`">{{ item.title }}</NuxtLink>
        <strong>{{ price(item.priceAmount, item.currency) }}</strong>
        <span class="muted">看过 {{ item.viewCount }} 次 · {{ new Date(item.viewedAt).toLocaleDateString('zh-CN') }}</span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.page { max-width: 720px; margin: 0 auto; padding: 24px 16px; }
.hint { color: #6b7280; }
.error { color: #dc2626; }
.list { list-style: none; padding: 0; display: grid; gap: 12px; }
.list li {
  display: grid; grid-template-columns: 1fr auto auto; gap: 12px; align-items: center;
  padding: 12px 16px; border: 1px solid #e5e7eb; border-radius: 10px;
}
.muted { color: #9ca3af; font-size: 13px; }
</style>
