<script setup lang="ts">
const { siteName } = useSite()
useHead(() => ({ title: `砍价活动 · ${siteName.value}` }))
</script>

<template>
  <!-- 列表页不请求活动接口，“暂未发布”是写死的文案：既不是后端返回的空数组，也没有失败态。
       接入真实活动列表时必须补上“加载失败”分支，否则服务故障会显示成“没有活动”。 -->
  <section class="panel simple-page">
    <p class="eyebrow">BARGAIN</p>
    <h1>砍价活动</h1>
    <p class="muted">邀请好友帮你砍价，达到底价后即可下单。当前演示环境暂未发布砍价活动，活动上线后会在这里展示。</p>
    <!-- 砍价详情页（/bargain/[id]）已存在，但本页没有任何指向它的入口，只能靠外部链接进入；
         有真实活动数据后要在这里补列表与跳转。 -->
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
