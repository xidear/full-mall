<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { resolveMediaUrl } from '../media'

/**
 * 素材/商品图片（保留宿主页面的 class 与事件）。
 *
 * <p>与 AdminThumb 的区别：AdminThumb 是"固定尺寸缩略图"，本身负责尺寸；
 * AssetImage 用于列表/网格里已有自己样式的 img——只补两件事：
 * 1. 地址统一走 resolveMediaUrl()；
 * 2. 加载失败回落到同 class 的占位块，避免裂图 + alt 文本撑破布局。
 */
const props = defineProps<{
  src?: string | null
  alt?: string | null
  placeholder?: string
}>()

const failed = ref(false)
const resolved = computed(() => resolveMediaUrl(props.src))
// 视频素材不能用 <img> 渲染（浏览器会 404 并在控制台刷错），这里显示占位标记
const isVideo = computed(() => /\.(mp4|webm|mov)(\?|$)/i.test(props.src ?? ''))

watch(() => props.src, () => { failed.value = false })
</script>

<template>
  <span v-if="isVideo" class="asset-image-fallback" role="img" :aria-label="alt ?? '视频'">
    {{ placeholder ?? '视频' }}
  </span>
  <img
    v-else-if="resolved && !failed"
    :src="resolved"
    :alt="alt ?? ''"
    loading="lazy"
    @error="failed = true"
  >
  <span v-else class="asset-image-fallback" role="img" :aria-label="alt ?? '无图'">
    {{ placeholder ?? '无图' }}
  </span>
</template>

<style scoped>
.asset-image-fallback {
  display: inline-grid;
  place-items: center;
  overflow: hidden;
  background: #f2f1ec;
  /* 占位文字也要可读：原 #8a8f8a 与底色只有 2.9:1 */
  color: #5b615c;
  font-size: 12px;
  white-space: nowrap;
}
</style>
