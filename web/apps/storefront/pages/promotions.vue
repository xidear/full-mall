<script setup lang="ts">
import type { GroupBuy, Money, Promotion } from '~/types'

const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const { data: promotions, error } = await useFetch<Promotion[]>(`${base}/api/v1/promotions`, {
  key: 'active-promotions'
})
const { add, clear } = useCart()

// 取数失败直接整页报错（502），与页面底部“当前没有进行中的促销”严格区分：
// 后者只在请求成功但返回空数组时出现，失败时绝不能拿空态顶替，否则顾客会以为活动全下线了。
if (error.value) {
  console.error('[promotions] 加载失败:', error.value)
  throw createError({ statusCode: 502, statusMessage: '促销活动加载失败，请稍后重试' })
}

const flashSales = computed(() => (promotions.value ?? []).filter(item => item.type === 'FLASH_SALE'))
const gifts = computed(() => (promotions.value ?? []).filter(item => item.type === 'BUY_GIFT'))
const groups = computed(() => (promotions.value ?? []).filter(item => item.type === 'GROUP_BUY'))

const groupDialog = ref(false)
const selectedPromotion = ref<Promotion | null>(null)
const phone = ref('13800000000')
const joinCode = ref('')
const groupError = ref('')
const groupBusy = ref(false)
const createdGroup = ref<GroupBuy | null>(null)

function addPromotion(promotion: Promotion, quantity: number) {
  // 购物车里只放活动展示价，作为结算页的预估；秒杀库存、限购与最终应付由结算服务端重算，
  // 前端不能自己乘数量或叠加其它优惠，否则会与后端算出的金额对不上。
  const price = promotion.promoPrice ?? promotion.originalPrice
  add({
    productId: promotion.productId,
    skuId: promotion.skuId,
    skuCode: promotion.skuCode,
    title: promotion.productTitle,
    price,
    quantity
  })
  navigateTo('/checkout')
}

function openGroup(promotion: Promotion) {
  selectedPromotion.value = promotion
  joinCode.value = ''
  groupError.value = ''
  createdGroup.value = null
  groupDialog.value = true
}

function groupCart(group: GroupBuy, promotion: Promotion) {
  // 先清空购物车：拼团订单规定只能包含 1 件拼团商品（结算页会再校验一次）。
  // 这是会丢掉顾客原有购物车内容的副作用，且不可撤销，所以只在确认团已存在/已创建后执行。
  clear()
  add({
    productId: promotion.productId,
    skuId: group.skuId,
    skuCode: group.skuCode,
    title: promotion.productTitle,
    price: group.price,
    quantity: 1
  })
  localStorage.setItem('fullmall.groupBuyId', group.id)
  localStorage.setItem('fullmall.groupBuyCode', group.code)
  groupDialog.value = false
  navigateTo('/checkout')
}

async function createGroup() {
  if (!selectedPromotion.value) return
  if (config.public.appMode === 'demo') {
    groupError.value = '演示站为只读模式，不创建真实拼团。'
    return
  }
  groupBusy.value = true
  groupError.value = ''
  try {
    const group = await $fetch<GroupBuy>(`${config.public.apiBase}/api/v1/group-buys`, {
      method: 'POST',
      body: { promotionId: selectedPromotion.value.id, phone: phone.value }
    })
    createdGroup.value = group
    groupCart(group, selectedPromotion.value)
  } catch (cause) {
    groupError.value = apiErrorMessage(cause, '发起拼团失败，请稍后重试')
  } finally {
    groupBusy.value = false
  }
}

async function joinGroup() {
  if (!joinCode.value.trim()) {
    groupError.value = '请输入团号'
    return
  }
  if (config.public.appMode === 'demo') {
    groupError.value = '演示站为只读模式，不加入真实拼团。'
    return
  }
  groupBusy.value = true
  groupError.value = ''
  try {
    const group = await $fetch<GroupBuy>(`${config.public.apiBase}/api/v1/group-buys/${joinCode.value.trim()}`)
    if (group.status !== 'FORMING') {
      groupError.value = '该拼团已结束'
      return
    }
    if (group.remainingSlots <= 0) {
      groupError.value = '该拼团人数已满'
      return
    }
    const promotion = (promotions.value ?? []).find(item => item.id === group.promotionId)
    if (!promotion) {
      groupError.value = '拼团对应的活动已结束'
      return
    }
    groupCart(group, promotion)
  } catch (cause) {
    groupError.value = apiErrorMessage(cause, '加入拼团失败，请稍后重试')
  } finally {
    groupBusy.value = false
  }
}

function money(value?: Money) {
  return formatMoney(value)
}
</script>

<template>
  <section class="promotion-page">
    <header class="promotion-hero">
      <p class="eyebrow">限时活动</p>
      <h1>秒杀 · 买赠 · 拼团</h1>
      <p>活动价格与名额由服务端校验；加入购物车后以结算页应付金额为准。</p>
    </header>

    <section v-if="flashSales.length" class="promotion-section">
      <div class="section-heading">
        <div>
          <p class="eyebrow">FLASH SALE</p>
          <h2>限时秒杀</h2>
        </div>
      </div>
      <div class="promotion-grid">
        <article v-for="promotion in flashSales" :key="promotion.id" class="promotion-card flash">
          <span class="promotion-badge">秒杀</span>
          <h3>{{ promotion.title }}</h3>
          <p class="muted">{{ promotion.productTitle }}</p>
          <div class="promotion-price">
            <strong>{{ money(promotion.promoPrice) }}</strong>
            <del>{{ money(promotion.originalPrice) }}</del>
          </div>
          <p class="promotion-meta">
            剩余 {{ promotion.remainingStock ?? 0 }} 件
            <template v-if="promotion.perUserLimit"> · 每人限 {{ promotion.perUserLimit }} 件</template>
          </p>
          <button
            type="button"
            class="button"
            :disabled="promotion.soldOut"
            @click="addPromotion(promotion, 1)"
          >
            {{ promotion.soldOut ? '已售罄' : '立即抢购' }}
          </button>
        </article>
      </div>
    </section>

    <section v-if="gifts.length" class="promotion-section">
      <div class="section-heading">
        <div>
          <p class="eyebrow">BUY &amp; GIFT</p>
          <h2>买赠活动</h2>
        </div>
      </div>
      <div class="promotion-grid">
        <article v-for="promotion in gifts" :key="promotion.id" class="promotion-card gift">
          <span class="promotion-badge">买赠</span>
          <h3>{{ promotion.title }}</h3>
          <p>{{ promotion.productTitle }} 买 {{ promotion.buyQuantity }} 件，赠 {{ promotion.giftTitle }} {{ promotion.giftQuantity }} 件。</p>
          <p class="promotion-meta">
            <template v-if="promotion.maxGiftPerOrder">每单最多赠 {{ promotion.maxGiftPerOrder }} 件 · </template>
            赠品剩余 {{ promotion.remainingStock ?? '不限' }}
          </p>
          <button type="button" class="button" @click="addPromotion(promotion, promotion.buyQuantity ?? 2)">
            去凑单
          </button>
        </article>
      </div>
    </section>

    <section v-if="groups.length" class="promotion-section">
      <div class="section-heading">
        <div>
          <p class="eyebrow">GROUP BUY</p>
          <h2>拼团专区</h2>
        </div>
      </div>
      <div class="promotion-grid">
        <article v-for="promotion in groups" :key="promotion.id" class="promotion-card group">
          <span class="promotion-badge">拼团</span>
          <h3>{{ promotion.title }}</h3>
          <p class="muted">{{ promotion.productTitle }}</p>
          <div class="promotion-price">
            <strong>{{ money(promotion.promoPrice) }}</strong>
            <del>{{ money(promotion.originalPrice) }}</del>
          </div>
          <p class="promotion-meta">
            {{ promotion.groupSize }} 人成团 · {{ promotion.groupDurationHours }} 小时内未成团自动退款
          </p>
          <button type="button" class="button" @click="openGroup(promotion)">发起 / 加入拼团</button>
        </article>
      </div>
    </section>

    <section class="promotion-section promo-links">
      <div class="section-heading">
        <div>
          <p class="eyebrow">MORE</p>
          <h2>更多优惠</h2>
        </div>
      </div>
      <div class="promo-link-row">
        <NuxtLink to="/coupons" class="promo-link-card">
          <strong>领券中心</strong>
          <span>查看可领取的优惠券</span>
        </NuxtLink>
        <NuxtLink to="/bargain" class="promo-link-card">
          <strong>砍价活动</strong>
          <span>邀请好友帮你砍到低价</span>
        </NuxtLink>
      </div>
    </section>

    <p v-if="!(promotions ?? []).length" class="empty-state">当前没有进行中的促销活动。</p>

    <div v-if="groupDialog" class="promotion-modal-mask" @click.self="groupDialog = false">
      <div class="promotion-modal">
        <h3>{{ selectedPromotion?.title }}</h3>
        <p class="muted">发起新团或输入好友的团号加入。拼团订单仅包含该商品 1 件。</p>
        <label class="promotion-field">
          手机号
          <input v-model="phone" type="tel" placeholder="用于参团与限购校验">
        </label>
        <button type="button" class="button full-width" :disabled="groupBusy" @click="createGroup">
          {{ groupBusy ? '提交中…' : '发起拼团并去结算' }}
        </button>
        <div class="promotion-divider"><span>或加入已有拼团</span></div>
        <label class="promotion-field">
          团号
          <input v-model="joinCode" type="text" placeholder="例如 GBABCD1234">
        </label>
        <button type="button" class="button button-ghost full-width" :disabled="groupBusy" @click="joinGroup">
          加入拼团
        </button>
        <p v-if="createdGroup" class="success-text">
          拼团已创建：{{ createdGroup.code }}，邀请好友输入团号即可参团。
        </p>
        <p v-if="groupError" class="error-text">{{ groupError }}</p>
        <button type="button" class="promotion-close" @click="groupDialog = false">关闭</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.promo-link-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.promo-link-card { display: flex; flex-direction: column; gap: 4px; padding: 18px; border: 1px solid var(--line); border-radius: var(--radius); background: var(--surface); }
.promo-link-card strong { font-size: 16px; }
.promo-link-card span { color: var(--muted); font-size: 13px; }
.promotion-page {
  padding: 12px 0 48px;
}

.promotion-hero {
  padding: 42px 32px;
  border-radius: 24px;
  background: linear-gradient(135deg, var(--brand-dark), var(--brand));
  color: #fff;
}

.promotion-hero h1 {
  margin: 8px 0;
  font-size: clamp(30px, 5vw, 52px);
}

.promotion-hero p {
  max-width: 720px;
  color: rgba(255, 255, 255, 0.82);
}

.promotion-section {
  margin-top: 32px;
}

.promotion-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
}

.promotion-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 24px;
  border-radius: 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  box-shadow: 0 16px 40px rgba(29, 52, 36, 0.08);
}

.promotion-card h3 {
  margin: 0;
  font-size: 20px;
}

.promotion-card.flash {
  border-top: 5px solid var(--danger);
}

.promotion-card.gift {
  border-top: 5px solid var(--brand);
}

.promotion-card.group {
  border-top: 5px solid var(--accent);
}

.promotion-badge {
  align-self: flex-start;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--surface-muted);
  font-size: 12px;
  font-weight: 700;
}

.promotion-price {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.promotion-price strong {
  font-size: 26px;
  color: var(--brand);
}

.promotion-price del {
  color: var(--muted);
}

.promotion-meta {
  color: var(--muted);
  font-size: 13px;
}

.promotion-card .button {
  margin-top: auto;
}

.promotion-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(16, 24, 18, 0.55);
}

.promotion-modal {
  width: min(440px, 100%);
  padding: 28px;
  border-radius: 20px;
  background: var(--surface);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.24);
}

.promotion-field {
  display: grid;
  gap: 6px;
  margin: 12px 0;
  font-size: 14px;
  color: var(--text);
}

.promotion-field input {
  padding: 11px 12px;
  border: 1px solid var(--line);
  border-radius: 10px;
  font: inherit;
}

.promotion-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 18px 0 6px;
  color: var(--muted);
  font-size: 12px;
}

.promotion-divider::before,
.promotion-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--line);
}

.promotion-close {
  display: block;
  margin: 16px auto 0;
  border: 0;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}
</style>