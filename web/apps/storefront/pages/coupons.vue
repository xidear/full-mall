<script setup lang="ts">
const { siteName } = useSite()
useHead(() => ({ title: `领券中心 · ${siteName.value}` }))
</script>

<template>
  <!-- 本页不请求任何券接口，是纯静态占位：文案里的“暂无券”不是后端返回的空列表，
       所以它既不代表加载失败，也不会随券上线自动出现内容——接券列表时必须同时补空态与失败态。 -->
  <section class="panel simple-page">
    <p class="eyebrow">COUPONS</p>
    <h1>领券中心</h1>
    <p class="muted">优惠券会按活动逐步开放。当前演示环境暂无顾客端可领取的券，先去看看限时活动吧。</p>
    <!-- 没有券也要给出下一步，避免停在死路；这里不读登录态，未登录顾客同样能到达促销页。 -->
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
