<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { resolveMediaUrl } from '../media'

/**
 * 商品/素材缩略图：统一处理"地址为空"与"加载失败"。
 *
 * 过去的实现是直接 <img :src="row.skus[0].imageUrl">：
 * 地址 404 时浏览器显示裂图，alt 文本撑破 44px 的格子（截图里的竖排乱码就是这样来的）。
 */
const props = withDefaults(defineProps<{
  src?: string | null
  alt?: string
  size?: number
  radius?: number
  placeholder?: string
}>(), { size: 44, radius: 8, placeholder: '无图' })

const failed = ref(false)
const resolved = computed(() => resolveMediaUrl(props.src))

watch(() => props.src, () => { failed.value = false })

const style = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  borderRadius: `${props.radius}px`
}))
</script>

<template>
  <img
    v-if="resolved && !failed"
    :src="resolved"
    :alt="alt || ''"
    class="admin-thumb"
    :style="style"
    loading="lazy"
    @error="failed = true"
  >
  <span v-else class="admin-thumb placeholder" :style="style" :title="alt">
    {{ placeholder }}
  </span>
</template>

<style scoped>
.admin-thumb {
  display: block;
  flex: none;
  object-fit: cover;
  background: #f2f1ec;
  /* 占位文字要可读：#8a8f8a 在 #f2f1ec 上只有 2.9:1（与 AssetImage 保持一致） */
  color: #5b615c;
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
}

.admin-thumb.placeholder {
  display: grid;
  place-items: center;
  white-space: nowrap;
}
</style>
