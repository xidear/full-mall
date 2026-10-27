<script setup lang="ts">
useHead({ title: '领券中心 · Full Mall' })
</script>

<template>
  <section class="panel simple-page">
    <p class="eyebrow">COUPONS</p>
    <h1>领券中心</h1>
    <p class="muted">优惠券会按活动逐步开放。当前演示环境暂无顾客端可领取的券，先去看看限时活动吧。</p>
    <div class="simple-actions">
      <NuxtLink to="/promotions" class="button">查看促销活动</NuxtLink>
      <NuxtLink to="/products" class="button button-secondary">去逛商品</NuxtLink>
    </div>
  </section>
</template>

<style scoped>
.simple-page { padding: 28px; }
.simple-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 18px; }
</style>
