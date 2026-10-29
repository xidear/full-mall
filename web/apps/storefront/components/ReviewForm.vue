<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { VALIDATION } from '@fullmall/api-client'

/**
 * 订单页的评价表单：逐件商品打分、写文字、传图片/视频。
 *
 * <p>上传走 `/api/v1/media` 拿 URL，再把 URL 随评价一起提交（POST /api/v1/products/reviews）——
 * 这样评价图片不会进运营素材库，也能在提交前让顾客删掉选错的图。
 *
 * <p>上限与可传类型来自后端配置，页面只负责展示与预检。
 */
const props = defineProps<{
  orderId: string
  lines: Array<{ id: string; skuId: string; title: string }>
  token: string
}>()

interface DraftMedia { url: string; kind: 'IMAGE' | 'VIDEO' }
interface Draft {
  rating: number
  content: string
  media: DraftMedia[]
  uploading: boolean
  submitting: boolean
  error: string
  done: boolean
}

const drafts = reactive<Record<string, Draft>>({})
const limits = ref<Awaited<ReturnType<typeof fetchMemberUploadLimits>> | null>(null)
const limitsError = ref('')

props.lines.forEach(line => {
  drafts[line.id] = { rating: 5, content: '', media: [], uploading: false, submitting: false, error: '', done: false }
})

onMounted(async () => {
  try {
    limits.value = await fetchMemberUploadLimits()
  } catch (cause) {
    limitsError.value = apiErrorMessage(cause, '上传规则加载失败，暂时无法传图')
  }
})

const accept = computed(() => {
  if (!limits.value) return ''
  return [...limits.value.imageTypes, ...limits.value.videoTypes].join(',')
})

const mediaHint = computed(() => {
  if (limitsError.value) return limitsError.value
  if (!limits.value) return '正在加载上传规则…'
  return `图片 ≤ ${megabytes(limits.value.imageMaxBytes)}MB，视频 ≤ ${megabytes(limits.value.videoMaxBytes)}MB，最多 ${VALIDATION.reviewMediaMax} 个`
})

function isVideoUrl(url: string): boolean {
  return /\.(mp4|webm|mov)(\?|$)/i.test(url)
}

async function pickFiles(lineId: string, event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  const draft = drafts[lineId]
  draft.error = ''
  if (!files.length) return
  if (!limits.value) {
    draft.error = limitsError.value || '上传规则还没加载完，请稍后再试'
    input.value = ''
    return
  }
  if (draft.media.length + files.length > VALIDATION.reviewMediaMax) {
    draft.error = `最多只能附 ${VALIDATION.reviewMediaMax} 个图片或视频`
    input.value = ''
    return
  }
  draft.uploading = true
  try {
    for (const file of files) {
      const uploaded = await uploadMemberMedia(file, props.token, limits.value)
      draft.media.push({ url: uploaded.url, kind: uploaded.kind })
    }
  } catch (cause) {
    draft.error = apiErrorMessage(cause, '上传失败，请重试或换一张图')
  } finally {
    draft.uploading = false
    input.value = ''
  }
}

function removeMedia(lineId: string, index: number) {
  drafts[lineId].media.splice(index, 1)
}

async function submit(line: { id: string; skuId: string }) {
  const draft = drafts[line.id]
  draft.error = ''
  draft.submitting = true
  try {
    await $fetch('/api/v1/products/reviews', {
      method: 'POST',
      headers: { Authorization: `Bearer ${props.token}` },
      body: {
        orderId: props.orderId,
        skuId: line.skuId,
        rating: draft.rating,
        content: draft.content.trim() || null,
        mediaUrls: draft.media.map(item => item.url)
      }
    })
    draft.done = true
  } catch (cause) {
    draft.error = apiErrorMessage(cause, '提交失败，请稍后重试')
  } finally {
    draft.submitting = false
  }
}
</script>

<template>
  <div class="review-form">
    <p class="muted">{{ mediaHint }}</p>
    <div v-for="line in lines" :key="line.id" class="review-line">
      <strong>{{ line.title }}</strong>
      <template v-if="drafts[line.id].done">
        <p class="review-done">已提交，审核通过后会展示在商品页。</p>
      </template>
      <template v-else>
        <div class="rating-row">
          <button v-for="star in 5" :key="star" type="button" class="star"
                  :class="{ active: star <= drafts[line.id].rating }"
                  :aria-label="`${star} 星`"
                  @click="drafts[line.id].rating = star">★</button>
        </div>
        <textarea v-model="drafts[line.id].content" rows="3" maxlength="1000"
                  placeholder="说说使用感受（选填，最多 1000 字）" />
        <div class="media-row">
          <div v-for="(item, index) in drafts[line.id].media" :key="item.url" class="media-item">
            <video v-if="item.kind === 'VIDEO'" :src="resolveMediaUrl(item.url)" controls preload="metadata" />
            <img v-else :src="resolveMediaUrl(item.url)" alt="评价图片" />
            <button type="button" class="remove" @click="removeMedia(line.id, index)">删除</button>
          </div>
          <label class="upload-tile">
            <input type="file" multiple :accept="accept" :disabled="drafts[line.id].uploading"
                   @change="pickFiles(line.id, $event)" />
            <span>{{ drafts[line.id].uploading ? '上传中…' : '图片/视频' }}</span>
          </label>
        </div>
        <p v-if="drafts[line.id].error" class="error">{{ drafts[line.id].error }}</p>
        <button type="button" class="submit" :disabled="drafts[line.id].submitting"
                @click="submit(line)">
          {{ drafts[line.id].submitting ? '提交中…' : '提交评价' }}
        </button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.review-form { display: grid; gap: 16px; }
.review-line { display: grid; gap: 8px; border-top: 1px solid var(--line, #e6e4dd); padding-top: 12px; }
.rating-row { display: flex; gap: 4px; }
.star { border: 0; background: none; font-size: 22px; color: #c9c6bd; cursor: pointer; }
.star.active { color: #e2a10d; }
textarea { width: 100%; border: 1px solid var(--line, #e6e4dd); border-radius: 8px; padding: 8px 10px; font: inherit; }
.media-row { display: flex; flex-wrap: wrap; gap: 10px; }
.media-item { position: relative; width: 96px; height: 96px; border-radius: 8px; overflow: hidden; }
.media-item img, .media-item video { width: 100%; height: 100%; object-fit: cover; display: block; }
.media-item .remove { position: absolute; right: 2px; bottom: 2px; font-size: 12px; }
.upload-tile { width: 96px; height: 96px; border: 1px dashed var(--line, #cfccc2); border-radius: 8px;
  display: grid; place-items: center; font-size: 12px; cursor: pointer; text-align: center; }
.upload-tile input { display: none; }
.error { color: #c0392b; margin: 0; }
.review-done { color: #2e7d32; margin: 0; }
.submit { justify-self: start; padding: 8px 16px; border-radius: 8px; border: 0; background: var(--accent, #1f6feb); color: #fff; cursor: pointer; }
</style>
