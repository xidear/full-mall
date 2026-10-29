<script setup lang="ts">
import type { Product } from '~/types'

const props = defineProps<{ product: Product }>()
const { t } = useLocale()

// 卡片只取第一个 SKU 的价格与图片（多规格的确切价在详情页）；
// 无 SKU 时靠 formatMoney 显示 '-'，补 0 会让“无价商品”看起来免费。
const firstSku = computed(() => props.product.skus[0])
const tone = computed(() => categoryTone(props.product.categoryId))
const initials = computed(() => props.product.title.slice(0, 2))
// 角标只报最影响决策的一条（降价 > 批次 > 自提），全堆上去就是字墙。
const badge = computed(() => {
  if (firstSku.value?.compareAtPrice) return t('product.badge.deal')
  if (props.product.shelfLifeManaged) return t('product.badge.batch')
  return t('product.badge.self')
})
</script>

<template>
  <article class="product-card">
    <NuxtLink :to="`/product/${product.id}`" class="product-media" :class="tone">
      <img v-if="firstSku?.imageUrl" :src="resolveMediaUrl(firstSku.imageUrl)"
           :alt="product.title" @error="onImageError">
      <span v-else class="product-placeholder">{{ initials }}</span>
      <span class="badge">{{ badge }}</span>
    </NuxtLink>
    <div class="product-body">
      <NuxtLink :to="`/product/${product.id}`" class="product-title">{{ product.title }}</NuxtLink>
      <p>{{ product.description }}</p>
      <div class="product-tags">
        <span>{{ t('product.tag.freeShipping') }}</span>
        <span>{{ t('product.tag.returns') }}</span>
        <span v-if="product.shelfLifeManaged">{{ t('product.tag.pickup') }}</span>
      </div>
      <div class="product-meta">
        <strong>{{ formatMoney(firstSku?.price) }}</strong>
        <span v-if="firstSku?.compareAtPrice" class="compare-price">
          {{ formatMoney(firstSku.compareAtPrice) }}
        </span>
      </div>
      <div class="product-actions">
        <NuxtLink :to="`/product/${product.id}`" class="button button-secondary">{{ t('product.view') }}</NuxtLink>
      </div>
    </div>
  </article>
</template>
