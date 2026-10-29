<script setup lang="ts">
const { locale, t } = useLocale()
const { siteName } = useSite()
// 会员时区优先，未登录时用站点默认时区（避免 SSR 与浏览器显示不一致）。
const memberTimezone = useDisplayTimeZone()
import type { Category, Product, Promotion } from '~/types'
import { VALIDATION } from '@fullmall/api-client'

const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const { data: product, error } = await useFetch<Product>(`${base}/api/v1/products/${route.params.id}`, {
  key: `product-${route.params.id}-${locale.value}`,
  query: { locale: locale.value }
})
// 促销一次取全量，再按 skuId + active 本地过滤：活动起止时间、限购、秒杀库存都由后端判定，
// active 就是后端给出的结论；前端不比较时间、不按站点时区重算，避免浏览器时钟/时区让活动误判。
const { data: promotions } = await useFetch<Promotion[]>(`${base}/api/v1/promotions`, {
  key: 'promotions-active'
})
// 与布局共用同一 useAsyncData key，不额外发请求；只用于规格参数里的分类名。
const { data: categories } = await useAsyncData<Category[]>('categories', () =>
  $fetch<Category[]>(`${base}/api/v1/categories`)
)

useHead(() => ({ title: product.value ? `${product.value.title} · ${siteName.value}` : `商品详情 · ${siteName.value}` }))

// 取数失败与商品本身不存在都落到同一个 404：顾客只需要“看不到这个商品”并能回列表，
// 真实原因（5xx、超时）留在服务端日志，不在顾客端暴露状态码或接口路径（AGENTS.md §10.2）。
if (error.value || !product.value) {
  throw createError({ statusCode: 404, statusMessage: '商品不存在' })
}

const selectedSkuId = ref(product.value.skus[0]?.id ?? '')
const activeTab = ref<'detail' | 'specs' | 'reviews' | 'service'>('detail')

// 富文本详情：按 locale 取"已发布译文"，没有则回退默认语言（后端已做回退）
const { data: detailHtml } = await useFetch<{ html: string }>(
  `${base}/api/v1/products/${route.params.id}/detail`,
  { query: { locale: locale.value } }
)
const categoryLabel = useCategoryLabel()
const categoryName = computed(() => {
  const category = categories.value?.find(item => item.id === product.value?.categoryId)
  return category ? categoryLabel(category) : ''
})
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

interface Review { id: string; rating: number; content: string; mediaUrls?: string[]; createdAt: string }
// 评价按 SKU 维度取，切换规格会重新请求，但固定 key 会复用同一份缓存。
// 这里没有接 error：请求失败时 reviews 也是空，页面显示“暂无评价”——空列表与失败在顾客端无法区分。
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

// “立即购买” = 写购物车 + 跳结算页，两步之间没有回滚：加购那一步没成功（例如没选到规格）也会跳过去，
// 所以结算页必须按购物车里的真实内容渲染，不能假定这里一定加购成功。
async function buyNow() {
  addToCart()
  await navigateTo('/checkout')
}
</script>

<template>
  <div v-if="product" class="product-page">
    <div class="product-detail">
      <div class="product-gallery">
        <div class="product-detail-media" :class="tone">
          <img v-if="heroShown" :src="resolveMediaUrl(heroShown)" :alt="product.title"
               @error="onImageError">
          <span v-else class="product-placeholder large">{{ product.title.slice(0, 2) }}</span>
        </div>
        <div v-if="gallery.length || videos.length" class="product-media-strip">
          <button v-for="item in gallery" :key="item.url" type="button" class="media-thumb"
                  :aria-label="`查看图片：${item.url}`" @click="activeHero = item.url">
            <img :src="resolveMediaUrl(item.url)" :alt="product.title" loading="lazy"
                 @error="onImageError">
          </button>
          <video v-for="item in videos" :key="item.url" :src="item.url" controls muted playsinline></video>
        </div>
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
            {{ t('product.quantity') }}
            <input v-model.number="quantity" type="number" :min="VALIDATION.quantityMin" :max="VALIDATION.quantityMax" :aria-label="t('product.quantity')">
          </label>
          <button type="button" class="button" @click="addToCart">{{ t('product.addToCart') }}</button>
          <button type="button" class="button button-secondary" @click="buyNow">{{ t('product.buyNow') }}</button>
          <button type="button" class="button button-ghost" :disabled="favoriteBusy" @click="toggleFavorite">
            {{ favorite ? t('product.favorited') : t('product.favorite') }}
          </button>
        </div>
        <p v-if="added" class="success-text">已加入购物车</p>
        <p v-if="favoriteMessage" class="muted">{{ favoriteMessage }}</p>
        <p v-if="flashPromotion" class="notice">秒杀价在结算时由服务端校验活动库存与限购。</p>
      </div>
    </div>

    <section class="product-tabs" aria-label="商品信息">
      <div class="tab-list" role="tablist">
        <button type="button" role="tab" :aria-selected="activeTab === 'detail'" :class="{ active: activeTab === 'detail' }" @click="activeTab = 'detail'">{{ t('product.tab.detail') }}</button>
        <button type="button" role="tab" :aria-selected="activeTab === 'specs'" :class="{ active: activeTab === 'specs' }" @click="activeTab = 'specs'">{{ t('product.tab.specs') }}</button>
        <button type="button" role="tab" :aria-selected="activeTab === 'reviews'" :class="{ active: activeTab === 'reviews' }" @click="activeTab = 'reviews'">
          {{ t('product.tab.reviews') }}<span class="tab-count">{{ reviews?.length ?? 0 }}</span>
        </button>
        <button type="button" role="tab" :aria-selected="activeTab === 'service'" :class="{ active: activeTab === 'service' }" @click="activeTab = 'service'">{{ t('product.tab.service') }}</button>
      </div>

      <div v-show="activeTab === 'detail'" class="tab-panel" role="tabpanel">
        <!-- 富文本详情：内容来自后台编辑器，服务端已用 jsoup 白名单清洗（script/style 不会进来） -->
        <div v-if="detailHtml?.html" class="product-rich-detail" v-html="detailHtml.html" />
        <template v-else>
          <p v-if="product.description" class="detail-copy">{{ product.description }}</p>
          <div v-if="detailImages.length" class="product-detail-images">
            <img v-for="item in detailImages" :key="item.url" :src="resolveMediaUrl(item.url)"
                 :alt="product.title" loading="lazy" @error="onImageError">
          </div>
        </template>
        <EmptyState v-if="!detailHtml?.html && !product.description && !detailImages.length" icon="box" title="暂无详细描述" description="商品详情正在补充中。" />
      </div>

      <div v-show="activeTab === 'specs'" class="tab-panel" role="tabpanel">
        <table class="spec-table">
          <tbody>
            <tr><th scope="row">分类</th><td>{{ categoryName || '—' }}</td></tr>
            <tr><th scope="row">库存管理</th><td>{{ product.inventoryTrackingLevel === 'VARIANT' ? '按规格（SKU）' : '按商品（SPU）' }}</td></tr>
            <tr><th scope="row">批次 / 效期</th><td>{{ product.shelfLifeManaged ? '按批次管理' : '常规库存管理' }}</td></tr>
            <tr v-for="(value, key) in selectedSku?.options ?? {}" :key="key">
              <th scope="row">{{ key }}</th><td>{{ value }}</td>
            </tr>
            <tr v-if="selectedSku"><th scope="row">规格编号</th><td>{{ selectedSku.skuCode }}</td></tr>
          </tbody>
        </table>
      </div>

      <div v-show="activeTab === 'reviews'" class="tab-panel" role="tabpanel">
        <div class="reviews-head">
          <h2>用户评价</h2>
          <span class="muted">{{ reviews?.length ?? 0 }} 条</span>
        </div>
        <article v-for="review in reviews" :key="review.id" class="review-item">
          <div class="review-stars" :aria-label="`评分 ${review.rating} / 5`">{{ '★'.repeat(review.rating) }}{{ '☆'.repeat(5 - review.rating) }}</div>
          <p>{{ review.content || '该用户未填写文字评价' }}</p>
          <div v-if="review.mediaUrls?.length" class="review-media">
            <template v-for="url in review.mediaUrls" :key="url">
              <video v-if="/\.(mp4|webm|mov)(\?|$)/i.test(url)" :src="resolveMediaUrl(url)"
                     controls preload="metadata" />
              <img v-else :src="resolveMediaUrl(url)" alt="买家晒图" loading="lazy">
            </template>
          </div>
          <span class="muted">{{ formatMemberDate(review.createdAt, memberTimezone) }}</span>
        </article>
        <p v-if="!reviews?.length" class="muted reviews-empty">暂无评价，购买后可在订单页发表评价。</p>
      </div>

      <div v-show="activeTab === 'service'" class="tab-panel" role="tabpanel">
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
    </section>
  </div>
</template>

<style scoped>
.product-page { display: block; }

/* 图集和主图属于同一列；此前媒体条是 grid 的独立子项，会把右侧购买区挤到下一行。 */
.product-gallery {
  position: sticky;
  top: 100px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.product-gallery .product-detail-media {
  position: static;
}

.product-tabs {
  margin-top: 54px;
  border-top: 1px solid var(--line);
}

.tab-list {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  border-bottom: 1px solid var(--line);
  scrollbar-width: thin;
}

.tab-list button {
  position: relative;
  min-height: 52px;
  padding: 0 18px;
  border: 0;
  background: none;
  color: var(--muted);
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.tab-list button:hover {
  color: var(--text);
}

.tab-list button.active {
  color: var(--brand);
}

.tab-list button.active::after {
  position: absolute;
  right: 18px;
  bottom: -1px;
  left: 18px;
  height: 2px;
  border-radius: 2px;
  background: var(--brand);
  content: "";
}

.tab-count {
  margin-left: 6px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 500;
}

.tab-panel {
  padding: 28px 0 8px;
}

.detail-copy {
  max-width: 72ch;
  margin: 0 0 22px;
  color: var(--text);
  font-size: 15px;
  line-height: 1.9;
  white-space: pre-wrap;
}

.spec-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.spec-table th,
.spec-table td {
  padding: 13px 16px;
  border-bottom: 1px solid var(--line);
  text-align: left;
}

.spec-table th {
  width: 180px;
  background: var(--surface-muted);
  color: var(--muted);
  font-weight: 500;
}

.reviews-panel { margin-top: 0; border-top: 0; padding-top: 0; }
.reviews-head { display: flex; align-items: baseline; justify-content: space-between; }
.reviews-head h2 { margin: 0; }
.review-item { padding: 12px 0; border-bottom: 1px solid var(--line); }
.review-item p { margin: 6px 0; }
.review-stars { color: var(--accent); letter-spacing: 2px; }
.review-media { display: flex; flex-wrap: wrap; gap: 8px; margin: 6px 0 8px; }
.review-media img,
.review-media video { width: 120px; height: 120px; object-fit: cover; border-radius: 10px; background: var(--surface-muted); }
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
  background: color-mix(in srgb, var(--danger) 12%, var(--surface));
  color: var(--danger);
}

.promotion-tag.gift {
  background: color-mix(in srgb, var(--brand) 12%, var(--surface));
  color: var(--brand-dark);
}

.promotion-tag.group {
  background: color-mix(in srgb, var(--accent) 14%, var(--surface));
  color: var(--accent);
}

.product-detail-media img { width: 100%; aspect-ratio: 1 / 1; object-fit: cover; border-radius: 10px; }
.media-thumb { padding: 0; border: 1px solid var(--line); background: none; border-radius: 8px; cursor: pointer; }
.media-thumb:focus-visible { outline: 2px solid var(--brand); outline-offset: 2px; }
.product-media-strip img, .product-media-strip video { width: 84px; height: 84px; object-fit: cover; border-radius: 8px; }
.product-media-strip {
  display: flex; gap: 8px; overflow-x: auto; margin: 12px 0 0; padding-bottom: 4px;
}

.product-rich-detail { line-height: 1.8; color: #2b302d; }
.product-rich-detail :deep(h2),
.product-rich-detail :deep(h3) { margin: 18px 0 8px; }
.product-rich-detail :deep(img),
.product-rich-detail :deep(video) { max-width: 100%; border-radius: 8px; margin: 8px 0; }
.product-rich-detail :deep(figcaption) { font-size: 12px; color: #6b716c; text-align: center; }
.product-rich-detail :deep(ul),
.product-rich-detail :deep(ol) { padding-left: 22px; }
.product-detail-images { display: flex; flex-direction: column; gap: 10px; margin: 12px 0; }
.product-detail-images img { width: 100%; border-radius: 8px; }
@media (max-width: 960px) {
  .product-gallery { position: static; }
}

@media (max-width: 640px) {
  .purchase-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
  .purchase-row .quantity,
  .purchase-row .button { flex: 1 1 auto; }
  .tab-list button { padding: 0 14px; }
  .spec-table th { width: 120px; }
  .purchase-actions {
    position: sticky;
    bottom: 0;
    z-index: 5;
    background: var(--surface);
    padding: 10px 0 14px;
    box-shadow: 0 -6px 16px rgba(15, 23, 42, 0.08);
  }
  .purchase-actions .button { min-height: var(--control-height); }
}
</style>
