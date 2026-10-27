<script setup lang="ts">
const { locale } = useLocale()
import type { Product, Promotion } from '~/types'

const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const { data: product, error } = await useFetch<Product>(`${base}/api/v1/products/${route.params.id}`, {
  key: `product-${route.params.id}-${locale.value}`,
  query: { locale: locale.value }
})
const { data: promotions } = await useFetch<Promotion[]>(`${base}/api/v1/promotions`, {
  key: 'promotions-active'
})

useHead(() => ({ title: product.value ? `${product.value.title} · Full Mall` : '商品详情 · Full Mall' }))

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
const activeHero = ref('')
const heroShown = computed(() => activeHero.value || heroImage.value)
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
    imageUrl: sku.imageUrl,
    price: sku.price,
    quantity: quantity.value
  })
  added.value = true
  setTimeout(() => { added.value = false }, 1800)
}

const { token: memberToken } = useMember()
const favorite = ref(false)
const favoriteBusy = ref(false)
const favoriteMessage = ref('')

interface Review { id: string; rating: number; content: string; createdAt: string }
const { data: reviews } = await useAsyncData<Review[]>('product-reviews', () =>
  $fetch<Review[]>(`${base}/api/v1/products/${selectedSkuId.value}/reviews`),
  { watch: [selectedSkuId] }
)

async function loadFavorite() {
  favoriteMessage.value = ''
  if (!memberToken.value) {
    favorite.value = false
    return
  }
  try {
    const list = await $fetch<Array<{ skuId: string }>>(`${config.public.apiBase}/api/v1/favorites`, {
      headers: { Authorization: `Bearer ${memberToken.value}` }
    })
    favorite.value = list.some(item => item.skuId === selectedSkuId.value)
  } catch {
    favoriteMessage.value = '收藏状态加载失败，可稍后重试'
  }
}

async function toggleFavorite() {
  if (!memberToken.value) {
    favoriteMessage.value = '请先登录后收藏'
    return
  }
  favoriteBusy.value = true
  favoriteMessage.value = ''
  try {
    if (favorite.value) {
      await $fetch(`${config.public.apiBase}/api/v1/favorites/${selectedSkuId.value}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${memberToken.value}` }
      })
      favorite.value = false
      favoriteMessage.value = '已取消收藏'
    } else {
      await $fetch(`${config.public.apiBase}/api/v1/favorites/${selectedSkuId.value}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${memberToken.value}` }
      })
      favorite.value = true
      favoriteMessage.value = '已加入收藏'
    }
  } catch {
    favoriteMessage.value = '收藏操作失败，请稍后重试'
  } finally {
    favoriteBusy.value = false
  }
}

onMounted(loadFavorite)
watch(selectedSkuId, loadFavorite)

const { data: mediaList } = await useFetch<Array<{
  url: string; role: string; kind: string; skuId?: string | null
}>>(`${base}/api/v1/products/${route.params.id}/media`, {
  key: `product-media-${route.params.id}`
})

/** 当前展示的主图：优先规格图，其次商品主图，最后回退 SKU 自带图。 */
const heroImage = computed(() => {
  const list = mediaList.value ?? []
  const spec = list.find(item => item.role === 'SPEC' && item.skuId === selectedSku.value?.id)
  const main = list.find(item => item.role === 'MAIN')
  return spec?.url ?? main?.url ?? selectedSku.value?.imageUrl ?? ''
})
const gallery = computed(() =>
  (mediaList.value ?? []).filter(item => item.role === 'GALLERY' && item.kind !== 'VIDEO'))
const detailImages = computed(() =>
  (mediaList.value ?? []).filter(item => item.role === 'DETAIL' && item.kind !== 'VIDEO'))
const videos = computed(() =>
  (mediaList.value ?? []).filter(item => item.role === 'VIDEO' || item.kind === 'VIDEO'))

async function buyNow() {
  addToCart()
  await navigateTo('/checkout')
}
</script>

<template>
  <div v-if="product" class="product-detail">
    <div class="product-detail-media" :class="tone">
      <img v-if="heroShown" :src="heroShown" :alt="product.title">
      <span v-else class="product-placeholder large">{{ product.title.slice(0, 2) }}</span>
    </div>

    <div v-if="gallery.length || videos.length" class="product-media-strip">
      <button v-for="item in gallery" :key="item.url" type="button" class="media-thumb"
              :aria-label="`查看图片：${item.url}`" @click="activeHero = item.url">
        <img :src="item.url" :alt="product.title" loading="lazy">
      </button>
      <video v-for="item in videos" :key="item.url" :src="item.url" controls muted playsinline></video>
    </div>
    <div class="product-detail-body">
      <nav class="breadcrumb">
        <NuxtLink to="/">首页</NuxtLink> /
        <NuxtLink to="/products">全部商品</NuxtLink> /
        <span>{{ product.title }}</span>
      </nav>
      <h1>{{ product.title }}</h1>
      <p class="product-description">{{ product.description }}</p>
      <div v-if="detailImages.length" class="product-detail-images">
        <img v-for="item in detailImages" :key="item.url" :src="item.url" :alt="product.title" loading="lazy">
      </div>
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

      <div v-if="product.skus.length" class="option-group">
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

      <div class="purchase-row purchase-actions">
        <label class="quantity">
          数量
          <input v-model.number="quantity" type="number" min="1" max="99">
        </label>
        <button type="button" class="button" @click="addToCart">加入购物车</button>
        <button type="button" class="button button-secondary" @click="buyNow">立即购买</button>
        <button type="button" class="button button-ghost" :disabled="favoriteBusy" @click="toggleFavorite">
          {{ favorite ? '已收藏' : '收藏' }}
        </button>
      </div>
      <p v-if="added" class="success-text">已加入购物车</p>
      <p v-if="favoriteMessage" class="muted">{{ favoriteMessage }}</p>
      <p v-if="flashPromotion" class="notice">秒杀价在结算时由服务端校验活动库存与限购。</p>

      <section class="reviews-panel">
        <div class="reviews-head">
          <h2>用户评价</h2>
          <span class="muted">{{ reviews?.length ?? 0 }} 条</span>
        </div>
        <article v-for="review in reviews" :key="review.id" class="review-item">
          <div class="review-stars" :aria-label="`评分 ${review.rating} / 5`">{{ '★'.repeat(review.rating) }}{{ '☆'.repeat(5 - review.rating) }}</div>
          <p>{{ review.content || '该用户未填写文字评价' }}</p>
          <span class="muted">{{ new Date(review.createdAt).toLocaleDateString('zh-CN') }}</span>
        </article>
        <p v-if="!reviews?.length" class="muted reviews-empty">暂无评价，购买后可在订单页发表评价。</p>
      </section>

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
.reviews-panel { margin-top: 24px; border-top: 1px solid var(--line); padding-top: 18px; }
.reviews-head { display: flex; align-items: baseline; justify-content: space-between; }
.reviews-head h2 { margin: 0; }
.review-item { padding: 12px 0; border-bottom: 1px solid var(--line); }
.review-item p { margin: 6px 0; }
.review-stars { color: #d97706; letter-spacing: 2px; }
.reviews-empty { margin-top: 18px; }

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

.product-detail-media img { width: 100%; aspect-ratio: 1 / 1; object-fit: cover; border-radius: 10px; }
.media-thumb { padding: 0; border: 1px solid #e2e8f0; background: none; border-radius: 8px; cursor: pointer; }
.media-thumb:focus-visible { outline: 2px solid #2563eb; outline-offset: 2px; }
.product-media-strip img, .product-media-strip video { width: 84px; height: 84px; object-fit: cover; border-radius: 8px; }
.product-media-strip {
  display: flex; gap: 8px; overflow-x: auto; margin: 12px 0 0; padding-bottom: 4px;
}

.product-detail-images { display: flex; flex-direction: column; gap: 10px; margin: 12px 0; }
.product-detail-images img { width: 100%; border-radius: 8px; }
@media (max-width: 640px) {
  .purchase-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
@media (max-width: 640px) {
  .purchase-actions { position: sticky; bottom: 0; background: #fff;
    padding: 10px 0 14px; box-shadow: 0 -6px 16px rgba(15,23,42,.08); z-index: 5; }
  .purchase-actions .button { min-height: 44px; }
}
}
</style>
