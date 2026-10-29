<script setup lang="ts">
const { siteName } = useSite()
useHead(() => ({ title: `购物车 · ${siteName.value}` }))
const { cart, total, updateQuantity, remove } = useCart()
</script>

<template>
  <section class="cart-page">
    <div class="section-heading">
      <div>
        <p class="eyebrow">购物车</p>
        <h1>我的购物车</h1>
      </div>
      <span class="muted">{{ cart.length }} 件商品</span>
    </div>

    <!-- 购物车只存在本机 localStorage、不发请求，所以空态是“真的空”，没有加载失败这一支；
         换浏览器或清缓存同样会走到这里，那不是故障。 -->
    <div v-if="cart.length" class="cart-layout">
      <div class="panel cart-list">
        <div v-for="line in cart" :key="line.skuId" class="cart-line">
          <div class="cart-thumb">
            <img v-if="line.imageUrl" :src="resolveMediaUrl(line.imageUrl)" :alt="line.title"
                 @error="onImageError">
            <span v-else>{{ line.title.slice(0, 1) }}</span>
          </div>
          <div class="cart-info">
            <strong>{{ line.title }}</strong>
          </div>
          <!-- 减到 0 会被 updateQuantity 直接当成“删除该行”，所以这里没有单独的删除确认；
               金额是加购当时的价格快照，单位分，仅作预估展示——实付以结算时后端重算为准。 -->
          <div class="cart-quantity">
            <button type="button" @click="updateQuantity(line.skuId, line.quantity - 1)">−</button>
            <span>{{ line.quantity }}</span>
            <button type="button" @click="updateQuantity(line.skuId, line.quantity + 1)">＋</button>
          </div>
          <strong class="cart-subtotal">
            {{ formatMoney({ amount: line.price.amount * line.quantity, currency: line.price.currency }) }}
          </strong>
          <button type="button" class="cart-remove" @click="remove(line.skuId)">删除</button>
        </div>
      </div>

      <!-- 这里的“应付”只等于各行预估小计之和：不含运费、优惠券、秒杀/拼团等任何后端规则，
           促销与限购一律由结算接口计算，前端不在这里预扣也不显示“省了多少”，以免与最终价不一致。 -->
      <aside class="panel cart-summary">
        <h2>结算信息</h2>
        <div class="summary-row"><span>商品件数</span><span>{{ cart.reduce((sum, line) => sum + line.quantity, 0) }}</span></div>
        <div class="summary-row"><span>商品金额</span><span>{{ formatMoney(total) }}</span></div>
        <div class="summary-row"><span>运费</span><span>免运费</span></div>
        <div class="summary-total"><span>应付</span><strong>{{ formatMoney(total) }}</strong></div>
        <NuxtLink to="/checkout" class="button full-width">去结算</NuxtLink>
        <p class="muted">支持门店自提或快递配送，结算时可选择。</p>
      </aside>
    </div>

    <EmptyState v-else icon="box" title="购物车还是空的" description="去挑几件喜欢的商品，支持门店自提与快递配送。">
      <NuxtLink to="/products" class="button">去逛逛</NuxtLink>
    </EmptyState>
  </section>
</template>