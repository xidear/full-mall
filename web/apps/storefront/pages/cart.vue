<script setup lang="ts">
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

    <div v-if="cart.length" class="cart-layout">
      <div class="panel cart-list">
        <div v-for="line in cart" :key="line.skuId" class="cart-line">
          <div class="cart-thumb">FM</div>
          <div class="cart-info">
            <strong>{{ line.title }}</strong>
            <span class="muted">{{ line.skuCode }}</span>
          </div>
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

    <div v-else class="empty-cart panel">
      <h2>购物车还是空的</h2>
      <p>去挑几件喜欢的商品吧。</p>
      <NuxtLink to="/products" class="button">去逛逛</NuxtLink>
    </div>
  </section>
</template>