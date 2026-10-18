<script setup lang="ts">
import type { Product } from '~/types'

const props = defineProps<{ product: Product }>()

const firstSku = computed(() => props.product.skus[0])
const tone = computed(() => categoryTone(props.product.categoryId))
const initials = computed(() => props.product.title.slice(0, 2))
const sold = computed(() => 100 + [...props.product.title].reduce((sum, char) => sum + char.charCodeAt(0), 0) % 900)
const badge = computed(() => {
  if (firstSku.value?.compareAtPrice) return '限时价'
  if (props.product.shelfLifeManaged) return '批次/效期'
  return '自营'
})
</script>

<template>
  <article class="product-card">
    <NuxtLink :to="`/product/${product.id}`" class="product-media" :class="tone">
      <img v-if="firstSku?.imageUrl" :src="firstSku.imageUrl" :alt="product.title">
      <span v-else class="product-placeholder">{{ initials }}</span>
      <span class="badge">{{ badge }}</span>
    </NuxtLink>
    <div class="product-body">
      <NuxtLink :to="`/product/${product.id}`" class="product-title">{{ product.title }}</NuxtLink>
      <p>{{ product.description }}</p>
      <div class="product-tags">
        <span>包邮</span>
        <span>七天无理由</span>
        <span v-if="product.shelfLifeManaged">门店自提</span>
      </div>
      <div class="product-meta">
        <strong>{{ formatMoney(firstSku?.price) }}</strong>
        <span v-if="firstSku?.compareAtPrice" class="compare-price">
          {{ formatMoney(firstSku.compareAtPrice) }}
        </span>
      </div>
      <div class="product-actions">
        <NuxtLink :to="`/product/${product.id}`" class="button button-secondary">查看详情</NuxtLink>
        <span class="sold">已售 {{ sold }}+</span>
      </div>
    </div>
  </article>
</template>