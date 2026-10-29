<script setup lang="ts">
import { computed } from 'vue'
import AssetImage from './AssetImage.vue'

/**
 * 素材预览：按 URL 扩展名区分图片与视频。
 *
 * <p>素材库从"只存图片"扩到"图片 + 视频"后，若统一用 <img> 显示，
 * 视频条目会变成裂图（浏览器不会把 mp4 画成图片），运营会误以为上传失败。
 */
// 不声明 class：父级的 class 由 Vue 自动透传到根元素（视频/图片都能吃样式）。
const props = withDefaults(defineProps<{ src?: string | null; alt?: string }>(), {
  alt: '素材'
})

const isVideo = computed(() => /\.(mp4|webm|mov)(\?|$)/i.test(props.src ?? ''))
</script>

<template>
  <video v-if="isVideo" :src="src ?? undefined" controls preload="metadata" :title="alt" />
  <AssetImage v-else :src="src" :alt="alt" />
</template>
