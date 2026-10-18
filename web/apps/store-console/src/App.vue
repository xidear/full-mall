<script setup lang="ts">
import StoreConsoleView from './views/StoreConsoleView.vue'
</script>

<template>
  <div class="store-shell">
    <header class="store-header">
      <div>
        <span class="store-mark">FM</span>
        <div>
          <strong>门店工作台</strong>
          <small>收货 · 批次 · 拣货 · 自提核销</small>
        </div>
      </div>
      <span class="store-mode">扫码优先 · 平板适配</span>
    </header>
    <main class="store-main">
      <StoreConsoleView />
    </main>
  </div>
</template>