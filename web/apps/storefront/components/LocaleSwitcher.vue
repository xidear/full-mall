<script setup lang="ts">
import { useLocale, type Locale } from '~/composables/useLocale'

const { locale, load, options, error } = useLocale()

async function change(event: Event) {
  const value = (event.target as HTMLSelectElement).value as Locale
  if (value === locale.value) return
  await load(value)
  // 商品/分类等 useFetch 在 setup 时捕获了 locale；强制整页重载，保证 SSR 数据与文案一致
  if (import.meta.client) {
    window.location.reload()
  }
}
</script>

<template>
  <label class="locale-switcher" :title="error || '切换语言'">
    <span class="sr-only">语言</span>
    <select :value="locale" aria-label="切换语言" @change="change">
      <option v-for="item in options" :key="item.code" :value="item.code">{{ item.label }}</option>
    </select>
  </label>
</template>

<style scoped>
.locale-switcher { display: inline-flex; align-items: center; }
.locale-switcher select {
  appearance: none;
  border: 1px solid #d8d3c8;
  background: #fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%23666' fill='none' stroke-width='1.5'/%3E%3C/svg%3E") no-repeat right 8px center;
  border-radius: 999px;
  padding: 4px 26px 4px 10px;
  font-size: 12px;
  color: var(--text, #222);
  cursor: pointer;
}
.locale-switcher select:focus-visible { outline: 2px solid var(--brand, #e1251b); outline-offset: 1px; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
</style>
