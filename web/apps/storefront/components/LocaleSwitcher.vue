<script setup lang="ts">
import type { Locale } from '~/composables/useLocale'

const { locale, load, supported } = useLocale()
const labels: Record<string, string> = { 'zh-CN': '中文', en: 'English' }

async function change(value: Locale) {
  await load(value)
}
</script>

<template>
  <div class="locale-switcher">
    <button v-for="item in supported" :key="item" type="button"
            :class="{ active: locale === item }" @click="change(item)">
      {{ labels[item] ?? item }}
    </button>
  </div>
</template>

<style scoped>
.locale-switcher { display: inline-flex; gap: 6px; }
.locale-switcher button {
  border: 1px solid #dcdfe6; background: #fff; border-radius: 4px;
  padding: 2px 8px; font-size: 12px; cursor: pointer;
}
.locale-switcher button.active { border-color: #409eff; color: #409eff; }
</style>
