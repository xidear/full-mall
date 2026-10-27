<script setup lang="ts">
useHead({ title: '砍价活动 · Full Mall' })
</script>

<template>
  <section class="panel simple-page">
    <p class="eyebrow">BARGAIN</p>
    <h1>砍价活动</h1>
    <p class="muted">邀请好友帮你砍价，达到底价后即可下单。当前演示环境暂未发布砍价活动，活动上线后会在这里展示。</p>
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
