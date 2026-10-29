<!--
  参数只能从页面栈的 options 取（uni-app 没有 router）；取不到 id 必须明确报错，
  H5 直接刷新本页时最容易走到这个分支。
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useMobileTheme } from '../../theme'
import { formatMoney } from '../../utils/money'

const { vars: themeVars } = useMobileTheme()

interface Money { amount: number; currency: string }
interface Sku { id: string; skuCode: string; price: Money; options?: Record<string, string> }
interface Product {
  id: string
  title: string
  description?: string
  skus: Sku[]
}

const apiBase = import.meta.env.VITE_API_BASE
  || `${location.protocol}//${location.hostname}:41280`
const product = ref<Product | null>(null)
const loading = ref(true)
const error = ref('')
const selected = ref<Sku | null>(null)

onMounted(() => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as unknown as { options?: { id?: string } }
  const id = current?.options?.id
  if (!id) {
    error.value = '缺少商品 ID'
    loading.value = false
    return
  }
  uni.request({
    url: `${apiBase}/api/v1/products/${id}`,
    data: { locale: 'zh-CN' },
    success: response => {
      // 已知边界：这里不判 statusCode，404 的错误体会被当成 Product → 表现是标题空白、
      // 价格显示 '-'、没有规格可选；排障时先看接口状态码，再怀疑页面。
      const payload = response.data as Product
      product.value = payload
      selected.value = payload.skus?.[0] ?? null
    },
    fail: failure => { error.value = failure.errMsg || '加载失败' },
    complete: () => { loading.value = false }
  })
})

// 价格缺失显示 '-' 而不是 0：0 元是合法价格（赠品），不能拿来当缺失值。
function money(value?: Money) {
  if (!value) return '-'
  return formatMoney(value.amount, value.currency)
}

// 加购是「读本地购物车 → 合并同 SKU → 写回」，必须与 cart.vue 用同一个 storage 键；
// 不合并会出现两行相同商品、合计翻倍。
function addToCart() {
  if (!product.value || !selected.value) return
  const key = 'fullmall.mobile.cart'
  const cart = uni.getStorageSync(key) as Array<{ skuId: string; title: string; price: number; quantity: number }> || []
  const existing = cart.find(item => item.skuId === selected.value!.id)
  if (existing) {
    existing.quantity += 1
  } else {
    cart.push({
      skuId: selected.value.id,
      title: product.value.title,
      price: selected.value.price.amount,
      quantity: 1
    })
  }
  uni.setStorageSync(key, cart)
  uni.showToast({ title: '已加入购物车', icon: 'success' })
}

function goCart() {
  uni.navigateTo({ url: '/pages/cart/cart' })
}
</script>

<template>
  <view class="page" :style="themeVars">
    <view v-if="loading" class="hint">加载中…</view>
    <view v-else-if="error" class="hint error">{{ error }}</view>
    <template v-else-if="product">
      <view class="title">{{ product.title }}</view>
      <view class="price">{{ money(selected?.price) }}</view>
      <view v-if="product.description" class="desc">{{ product.description }}</view>

      <view class="sku-list">
        <view v-for="(sku, index) in product.skus" :key="sku.id" class="sku"
              :class="{ active: selected?.id === sku.id }" @click="selected = sku">
          <text>{{ money(sku.price) }}</text>
          <text class="code">
            {{ sku.options && Object.keys(sku.options).length
              ? Object.values(sku.options).join(' / ')
              : (product.skus.length > 1 ? `规格 ${index + 1}` : '默认规格') }}
          </text>
        </view>
      </view>

      <view class="actions">
        <button class="btn" type="primary" @click="addToCart">加入购物车</button>
        <button class="btn ghost" @click="goCart">去购物车</button>
      </view>
    </template>
  </view>
</template>

<style scoped>
.page { padding: 24rpx; background: var(--bg); color: var(--text); min-height: 100vh; }
.hint { color: var(--muted); padding: 40rpx; text-align: center; }
.hint.error { color: var(--danger); }
.title { font-size: 36rpx; font-weight: 700; }
.price { font-size: 44rpx; color: var(--brand); margin: 16rpx 0; }
.desc { color: var(--muted); font-size: 26rpx; margin-bottom: 24rpx; }
.sku-list { display: flex; flex-wrap: wrap; gap: 16rpx; }
.sku { padding: 16rpx; border: 2rpx solid var(--line); background: var(--surface); border-radius: 12rpx; display: flex; flex-direction: column; }
.sku.active { border-color: var(--brand); color: var(--brand); }
.sku .code { font-size: 20rpx; color: var(--muted); }
.actions { display: flex; gap: 16rpx; margin-top: 32rpx; }
.btn { flex: 1; }
.btn.ghost { background: var(--surface); color: var(--brand); border: 2rpx solid var(--brand); }
</style>
