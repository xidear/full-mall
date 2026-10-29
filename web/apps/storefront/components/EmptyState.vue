<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  description?: string
  icon?: 'search' | 'box'
}>(), {
  description: '',
  icon: 'search'
})
</script>

<template>
  <div class="empty-state-card">
    <span class="empty-icon" aria-hidden="true">
      <svg v-if="icon === 'search'" viewBox="0 0 48 48">
        <circle cx="21" cy="21" r="12.5" fill="none" stroke="currentColor" stroke-width="2" />
        <path d="M30.5 30.5L40 40" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      </svg>
      <svg v-else viewBox="0 0 48 48">
        <path d="M10 17l14-7 14 7v17l-14 7-14-7z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
        <path d="M10 17l14 7 14-7M24 24v17" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
      </svg>
    </span>
    <h2>{{ title }}</h2>
    <p v-if="description">{{ description }}</p>
    <div class="empty-actions">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.empty-state-card {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 56px 24px;
  border: 1px dashed var(--line);
  border-radius: var(--radius);
  background: var(--surface);
  text-align: center;
}

.empty-icon {
  display: grid;
  width: 56px;
  height: 56px;
  place-items: center;
  margin-bottom: 4px;
  border-radius: 50%;
  background: var(--surface-muted);
  color: var(--muted);
}

.empty-icon svg {
  width: 28px;
  height: 28px;
}

.empty-state-card h2 {
  margin: 0;
  font-size: 18px;
}

.empty-state-card p {
  max-width: 420px;
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 10px;
}
</style>
