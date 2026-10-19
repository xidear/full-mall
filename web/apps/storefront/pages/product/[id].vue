<script setup lang="ts">
import type { Product, Promotion } from '~/types'

const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const { data: product, error } = await useFetch<Product>(`${base}/api/v1/products/${route.params.id}`, {
  query: { locale: 'zh-CN' }
})
const { data: promotions } = await useFetch<Promotion[]>(`${base}/api/v1/promotions`)

if (error.value || !product.value) {
  throw createError({ statusCode: 404, statusMessage: '商品不存在' })
}

const selectedSkuId = ref(product.value.skus[0]?.id ?? '')
const quantity = ref(1)
const selectedSku = computed(() => product.value?.skus.find(sku => sku.id === selectedSkuId.value))
const skuPromotions = computed(() =>
  (promotions.value ?? []).filter(promotion => promotion.skuId === selectedSkuId.value && promotion.active)
)
const flashPromotion = computed(() => skuPromotions.value.find(promotion => promotion.type === 'FLASH_SALE'))
const giftPromotion = computed(() => skuPromotions.value.find(promotion => promotion.type === 'BUY_GIFT'))
const groupPromotion = computed(() => skuPromotions.value.find(promotion => promotion.type === 'GROUP_BUY'))
const tone = computed(() => categoryTone(product.value?.categoryId))
const { add } = useCart()

onMounted(() => {
  const referrer = typeof route.query.ref === 'string' ? route.query.ref : ''
  if (referrer) {
    const mode = typeof route.query.mode === 'string' ? route.query.mode : 'ORDER'
    localStorage.setItem('fullmall.referral', JSON.stringify({ staffId: referrer, mode }))
  }
})
const added = ref(false)

function addToCart() {
  const sku = selectedSku.value
  if (!sku || !product.value) {
    return
  }
  add({
    productId: product.value.id,
    skuId: sku.id,
    skuCode: sku.skuCode,
    title: `${product.value.title} ${Object.values(sku.options).join(' / ')}`.trim(),
    price: sku.price,
    quantity: quantity.value
  })
  added.value = true
  setTimeout(() => { added.value = false }, 1800)
}

async function buyNow() {
  addToCart()
  await navigateTo('/checkout')
}
</script>

<template>
  <div v-if="product" class="product-detail">
    <div class="product-detail-media" :class="tone">
      <img v-if="selectedSku?.imageUrl" :src="selectedSku.imageUrl" :alt="product.title">
      <span v-else class="product-placeholder large">{{ product.title.slice(0, 2) }}</span>
    </div>
    <div class="product-detail-body">
      <nav class="breadcrumb">
        <NuxtLink to="/">首页</NuxtLink> /
        <NuxtLink to="/products">全部商品</NuxtLink> /
        <span>{{ product.title }}</span>
      </nav>
      <h1>{{ product.title }}</h1>
      <p class="product-description">{{ product.description }}</p>
      <div class="price-row">
        <strong class="price-large">{{ formatMoney(flashPromotion?.promoPrice ?? selectedSku?.price) }}</strong>
        <span v-if="flashPromotion" class="compare-price">{{ formatMoney(selectedSku?.price) }}</span>
        <span v-else-if="selectedSku?.compareAtPrice" class="compare-price">
          {{ formatMoney(selectedSku.compareAtPrice) }}
        </span>
      </div>
      <div v-if="skuPromotions.length" class="promotion-tags">
        <span v-if="flashPromotion" class="promotion-tag flash">
          秒杀 {{ formatMoney(flashPromotion.promoPrice) }} · 剩余 {{ flashPromotion.remainingStock ?? 0 }} 件
        </span>
        <span v-if="giftPromotion" class="promotion-tag gift">
          买 {{ giftPromotion.buyQuantity }} 赠 {{ giftPromotion.giftQuantity }} {{ giftPromotion.giftTitle }}
        </span>
        <NuxtLink v-if="groupPromotion" to="/promotions" class="promotion-tag group">
          {{ groupPromotion.groupSize }} 人拼团价 {{ formatMoney(groupPromotion.promoPrice) }}
        </NuxtLink>
      </div>
      <div class="service-row">
        <span>正品保障</span>
        <span>极速退款</span>
        <span>门店自提</span>
        <span>七天无理由</span>
      </div>

      <div v-if="product.skus.length > 1" class="option-group">
        <span class="option-label">规格</span>
        <div class="option-list">
          <button
            v-for="sku in product.skus"
            :key="sku.id"
            type="button"
            class="option-chip"
            :class="{ active: sku.id === selectedSkuId }"
            @click="selectedSkuId = sku.id"
          >
            {{ Object.values(sku.options).join(' / ') || sku.skuCode }}
          </button>
        </div>
      </div>

      <div v-if="product.shelfLifeManaged" class="notice">
        该商品按批次/效期管理，拣货时优先选择效期更早且满足最低剩余保质期的批次。
      </div>

      <div class="purchase-row">
        <label class="quantity">
          数量
          <input v-model.number="quantity" type="number" min="1" max="99">
        </label>
        <button type="button" class="button" @click="addToCart">加入购物车</button>
        <button type="button" class="button button-secondary" @click="buyNow">立即购买</button>
      </div>
      <p v-if="added" class="success-text">已加入购物车</p>
      <p v-if="flashPromotion" class="notice">秒杀价在结算时由服务端校验活动库存与限购。</p>

      <div class="product-extra">
        <div>
          <strong>配送</strong>
          <span>支持快递配送；自提门店以结算页库存为准。</span>
        </div>
        <div>
          <strong>售后</strong>
          <span>符合条件的商品支持七天无理由退货。</span>
        </div>
        <div>
          <strong>批次</strong>
          <span v-if="product.shelfLifeManaged">按批次/效期管理，拣货优先早效期。</span>
          <span v-else>常规库存管理。</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.promotion-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 4px 0 12px;
}

.promotion-tag {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
}

.promotion-tag.flash {
  background: #fdecea;
  color: #a93226;
}

.promotion-tag.gift {
  background: #eaf5ee;
  color: #276749;
}

.promotion-tag.group {
  background: #fff5e0;
  color: #9c650d;
}
</style>
