<script setup lang="ts">
import { formatMemberDateTime } from '~/utils/datetime'
import type { LegalDocument } from '~/types'

// 会员时区优先，未登录时用站点默认时区（避免 SSR 与浏览器显示不一致）。
const memberTimezone = useDisplayTimeZone()
const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const { locale, t } = useLocale()
const { market, siteName } = useSite()

// type 直接取自路由参数，缺省按 TERMS 处理；不做前端白名单拦截（例如 /legal/FOO 也会照发请求），
// 后端没有这个类型就整页 404。下面的 typeLabel 对未知类型回退成原始字符串，避免标题为空。
const type = computed(() => String(route.params.type || 'TERMS'))
const typeLabels: Record<string, string> = {
  TERMS: '服务条款',
  PRIVACY: '隐私政策',
  REFUND: '退款与售后政策',
  PICKUP: '门店自提规则'
}
const typeLabel = computed(() => {
  const key = `legal.type.${type.value}`
  const translated = t(key)
  return translated === key ? (typeLabels[type.value] || type.value) : translated
})
const { data: document, error, pending } = await useAsyncData<LegalDocument>(
  `legal-${type.value}-${locale.value}-${market.value}`,
  async () => {
    const load = (targetLocale: string) => $fetch<LegalDocument>(`${base}/api/v1/legal/documents`, {
      query: { type: type.value, locale: targetLocale, market: market.value }
    })
    try {
      return await load(locale.value)
    } catch (cause) {
      // 只在“该语种文档确实缺失（404）”时才回退到 zh-CN，并在页面上明示回退（fallbackNotice）。
      // 5xx/网络错误照原样抛出，不拿中文文档盖住真实故障——否则顾客会以为自己看的是当前语言版本。
      const statusCode = (cause as { statusCode?: number } | null)?.statusCode
      if (locale.value === 'zh-CN' || statusCode !== 404) throw cause
      return await load('zh-CN')
    }
  }
)

// 用文档实际语言与当前语言比较，避免把 ref 写在 useAsyncData 里导致水合后丢失。
const fallbackNotice = computed(() =>
  document.value && document.value.locale !== locale.value ? t('legal.fallback') : '')
useHead(() => ({ title: `${document.value?.title || typeLabel.value} · ${siteName.value}` }))

if (error.value || (!pending.value && !document.value)) {
  throw createError({ statusCode: 404, statusMessage: '法律文档不存在' })
}

type DocumentStatus = 'CURRENT' | 'UPCOMING' | 'EXPIRED'
// 用 SSR 时确定的时间做判定，避免服务端/客户端刚好跨过生效时刻时文本闪烁。
const evaluatedAt = useState<number>('legal-evaluated-at', () => Date.now())
const status = computed<DocumentStatus>(() => {
  if (!document.value) return 'CURRENT'
  const from = new Date(document.value.effectiveFrom).getTime()
  const to = document.value.effectiveTo ? new Date(document.value.effectiveTo).getTime() : Number.POSITIVE_INFINITY
  if (evaluatedAt.value < from) return 'UPCOMING'
  if (evaluatedAt.value >= to) return 'EXPIRED'
  return 'CURRENT'
})
const statusLabel = computed(() => t(`legal.${status.value.toLowerCase()}`))

const effectiveFromText = computed(() => document.value
  ? formatMemberDateTime(document.value.effectiveFrom, memberTimezone)
  : '')
const effectiveToText = computed(() => document.value?.effectiveTo
  ? formatMemberDateTime(document.value.effectiveTo, memberTimezone)
  : '')
const effectiveText = computed(() => {
  if (!document.value) return ''
  const key = status.value === 'UPCOMING' ? 'legal.effectiveOn' : 'legal.effectiveFrom'
  return t(key).replace('{date}', effectiveFromText.value)
})
</script>

<template>
  <!-- 上面“无文档即抛 404”，所以这行基本只在导航过渡的瞬间出现，它不是失败提示；
       真正的失败态是 404 错误页，两者不要互相替代。 -->
  <p v-if="!document" class="empty-state">文档加载中…</p>
  <article v-else class="legal-page panel">
    <header class="legal-head">
      <p class="legal-kicker">{{ typeLabel }}</p>
      <h1>{{ document.title }}</h1>
      <p class="legal-version">
        <span class="legal-status" :class="`legal-status--${status.toLowerCase()}`">{{ statusLabel }}</span>
        <span>{{ t('legal.revision') }} {{ document.version }}</span>
        <span>{{ effectiveText }}</span>
        <span v-if="effectiveToText">{{ t('legal.validUntil') }} {{ effectiveToText }}</span>
      </p>
      <p v-if="fallbackNotice" class="legal-fallback">{{ fallbackNotice }}</p>
    </header>

    <div class="legal-content">{{ document.content }}</div>

    <footer class="legal-foot">
      <p>{{ t('legal.footnote') }}</p>
    </footer>
  </article>
</template>

<style scoped>
.legal-head {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--line);
}

.legal-kicker {
  margin: 0 0 10px;
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.legal-head h1 {
  margin: 0 0 14px;
  font-size: clamp(28px, 4vw, 40px);
  line-height: 1.2;
}

.legal-version {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.legal-status {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.legal-status--current {
  background: color-mix(in srgb, var(--brand) 12%, var(--surface));
  color: var(--brand-dark);
}

.legal-status--upcoming {
  background: color-mix(in srgb, var(--accent) 14%, var(--surface));
  color: var(--accent);
}

.legal-status--expired {
  background: var(--surface-muted);
  color: var(--muted);
}

.legal-fallback {
  margin: 12px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.legal-content {
  margin: 28px 0;
  white-space: pre-wrap;
  font-size: 15px;
  line-height: 1.85;
}

.legal-foot {
  padding-top: 16px;
  border-top: 1px solid var(--line);
  color: var(--muted);
  font-size: 12px;
}

.legal-foot p {
  margin: 0;
}
</style>
