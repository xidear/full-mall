<script setup lang="ts">
import { computed } from 'vue'
import { useLocale, type Locale } from '~/composables/useLocale'

const props = withDefaults(defineProps<{ tone?: 'dark' | 'light' }>(), { tone: 'dark' })
const { locale, load, options, error } = useLocale()

const currentLabel = computed(() => options.find(item => item.code === locale.value)?.label ?? locale.value)

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
  <label class="locale-switcher" :class="`locale-switcher--${props.tone}`" :title="error || `当前语言：${currentLabel}`">
    <span class="sr-only">切换语言</span>
    <!-- 刻意用原生 <select>：自定义下拉要自己实现焦点陷阱与外部点击关闭，收益不抵成本。 -->
    <select :value="locale" aria-label="切换语言" @change="change">
      <option v-for="item in options" :key="item.code" :value="item.code">{{ item.label }}</option>
    </select>
  </label>
</template>

<style scoped>
/* 语言切换是“工具条控件”，必须继承所在容器的视觉语境：
   放在深色 topbar 时不能变成一块孤立的白色胶囊。 */
.locale-switcher {
  display: inline-flex;
  align-items: center;
  min-width: 0;
}

.locale-switcher select {
  min-height: 26px;
  padding: 2px 24px 2px 9px;
  border: 1px solid transparent;
  border-radius: 999px;
  background-color: transparent;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%23ffffff' fill='none' stroke-width='1.4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  color: inherit;
  font-size: 12px;
  line-height: 1.2;
  cursor: pointer;
  appearance: none;
  transition: background-color 0.18s ease, border-color 0.18s ease;
}

/* option 是原生弹层，不继承父级配色：不显式给浅底深字，深色 topbar 下会白底白字。 */
.locale-switcher select option {
  background: #fff;
  color: #222;
}

.locale-switcher--dark select {
  border-color: rgba(255, 255, 255, 0.22);
  color: rgba(255, 255, 255, 0.86);
}

.locale-switcher--dark select:hover {
  border-color: rgba(255, 255, 255, 0.48);
  background-color: rgba(255, 255, 255, 0.08);
}

.locale-switcher--light select {
  border-color: var(--line);
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%23666' fill='none' stroke-width='1.4'/%3E%3C/svg%3E");
  color: var(--text);
}

.locale-switcher--light select:hover {
  border-color: var(--brand);
}

.locale-switcher select:focus-visible {
  outline: 2px solid currentColor;
  outline-offset: 2px;
}

@media (max-width: 768px) {
  .locale-switcher select {
    min-height: 44px;
    padding-right: 28px;
  }
}
</style>
