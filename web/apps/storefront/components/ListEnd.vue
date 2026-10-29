<script setup lang="ts">
const { t } = useLocale()

withDefaults(defineProps<{
  count?: number
  noun?: string
}>(), {
  noun: '件商品'
})
</script>

<template>
  <div class="list-end">
    <div class="list-end-row">
      <span class="list-end-rule" aria-hidden="true" />
      <p class="list-end-copy" role="status">
        <svg class="list-end-icon" viewBox="0 0 20 20" aria-hidden="true">
          <circle cx="10" cy="10" r="8.25" fill="none" stroke="currentColor" stroke-width="1.3" />
          <path d="M6.4 10.2l2.4 2.4 4.8-5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span>{{ t('list.end.prefix') }}</span>
        <strong v-if="count" class="list-end-count">{{ count }}</strong>
        <span>{{ noun || t('list.end.noun') }}</span>
      </p>
      <span class="list-end-rule" aria-hidden="true" />
    </div>
  </div>
</template>

<style scoped>
.list-end {
  display: grid;
  justify-items: center;
  gap: 10px;
  margin: 36px 0 4px;
}

.list-end-row {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 16px;
}

.list-end-rule {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line));
}

.list-end-rule:last-child {
  background: linear-gradient(90deg, var(--line), transparent);
}

.list-end-copy {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0;
  color: var(--muted);
  font-size: 12px;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.list-end-icon {
  width: 16px;
  height: 16px;
  margin-right: 3px;
  color: var(--brand);
}

.list-end-count {
  color: var(--text);
  font-weight: 700;
}
</style>
