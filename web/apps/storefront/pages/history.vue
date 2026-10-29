<script setup lang="ts">
// 会员时区优先，未登录时用站点默认时区（避免 SSR 与浏览器显示不一致）。
const memberTimezone = useDisplayTimeZone()
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

// 只取最近 50 条且没有分页/加载更多：这里展示的是“最近足迹”，不是全部历史。
// 未登录时仍会发一次不带令牌的请求（后端 401），但模板优先显示登录提示，顾客看不到这个失败。
const { data, pending, error } = await useAsyncData<HistoryEntry[]>('history', () =>
  $fetch<HistoryEntry[]>(`${base}/api/v1/history`, { headers, query: { limit: 50 } }))

const items = computed(() => data.value ?? [])

function price(value: number, currency: string) {
  return formatMoney({ amount: value, currency })
}
</script>

<template>
  <section class="page">
    <h1>浏览足迹</h1>

    <!-- 顺序不可调换：未登录、加载中都排在空态之前，否则请求未回时会先闪“还没有浏览记录”；
         error 独立一支，因为空列表 ≠ 加载失败，失败时不能告诉顾客“你没看过商品”。 -->
    <p v-if="!memberToken" class="hint">登录后可查看足迹。</p>
    <p v-else-if="pending" class="hint">加载中…</p>
    <p v-else-if="error" class="hint error">足迹加载失败，请稍后再试。</p>
    <EmptyState v-else-if="!items.length" icon="box" title="还没有浏览记录" description="看过的商品会自动记录在这里，方便回头再找。">
      <NuxtLink to="/products" class="button">去逛商品</NuxtLink>
    </EmptyState>

    <ul v-else class="list">
      <li v-for="item in items" :key="item.skuId">
        <NuxtLink :to="`/product/${item.productId}`">{{ item.title }}</NuxtLink>
        <strong>{{ price(item.priceAmount, item.currency) }}</strong>
        <span class="muted">看过 {{ item.viewCount }} 次 · {{ formatMemberDate(item.viewedAt, memberTimezone) }}</span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.page { max-width: 720px; margin: 0 auto; padding: 24px 16px; }
.hint { color: var(--muted); }
.error { color: var(--danger); }
.list { list-style: none; padding: 0; display: grid; gap: 12px; }
.list li {
  display: grid; grid-template-columns: 1fr auto auto; gap: 12px; align-items: center;
  padding: 12px 16px; border: 1px solid var(--line); border-radius: 10px;
}
.muted { color: var(--muted); font-size: 13px; }
</style>
