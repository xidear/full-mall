<script setup lang="ts">
// 会员时区优先，未登录时用站点默认时区（避免 SSR 与浏览器显示不一致）。
const memberTimezone = useDisplayTimeZone()
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
// 用 cookie 而不是 localStorage 取令牌：服务端渲染时也能读到，首屏请求就能带上 Authorization。
// 注意未登录时下面仍会发一次不带令牌的请求（后端回 401），但模板优先渲染“登录后可查看收藏”，
// 所以顾客不会看到失败提示；这个请求只是浪费一次往返，不代表页面出错。
const memberToken = useCookie<string | null>('fullmall_member_token').value ?? ''
const headers: Record<string, string> = memberToken ? { Authorization: `Bearer ${memberToken}` } : {}

// headers 在 setup 时定稿、key 固定为 'favorites'：同一页面生命周期内不会跟随令牌变化重新取数，
// 刚在别处登录/切换账号后需要刷新页面才能看到正确的收藏列表。
const { data: favorites, pending, error } = await useAsyncData<Favorite[]>('favorites', () =>
  $fetch<Favorite[]>(`${base}/api/v1/favorites`, { headers }))

const items = computed(() => favorites.value ?? [])

function price(value: number, currency: string) {
  return formatMoney({ amount: value, currency })
}
</script>

<template>
  <section class="page">
    <h1>我的收藏</h1>

    <!-- 四个分支的先后顺序不能换：未登录和加载中必须排在“还没有收藏”之前，
         否则登录前或请求未回时会先闪一句“还没有收藏”，把空态误报成事实；
         error 单独一支，因为空列表 ≠ 加载失败。 -->
    <p v-if="!memberToken" class="hint">登录后可查看收藏。</p>
    <p v-else-if="pending" class="hint">加载中…</p>
    <p v-else-if="error" class="hint error">收藏加载失败，请稍后再试。</p>
    <EmptyState v-else-if="!items.length" icon="box" title="还没有收藏" description="在商品页点「收藏」，喜欢的商品会出现在这里。">
      <NuxtLink to="/products" class="button">去逛商品</NuxtLink>
    </EmptyState>

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
.hint { color: var(--muted); }
.error { color: var(--danger); }
.favorite-list { list-style: none; padding: 0; display: grid; gap: 12px; }
.favorite-list li {
  display: grid; grid-template-columns: 1fr auto auto; gap: 12px; align-items: center;
  padding: 12px 16px; border: 1px solid var(--line); border-radius: 10px;
}
.muted { color: var(--muted); font-size: 13px; }
</style>
